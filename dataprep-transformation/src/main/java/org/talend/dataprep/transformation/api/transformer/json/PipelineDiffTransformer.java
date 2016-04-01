// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.api.transformer.json;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.statistics.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.api.action.ActionParser;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;
import org.talend.dataprep.transformation.api.transformer.Transformer;
import org.talend.dataprep.transformation.api.transformer.TransformerWriter;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.transformation.api.transformer.configuration.PreviewConfiguration;
import org.talend.dataprep.transformation.format.WriterRegistrationService;
import org.talend.dataprep.transformation.pipeline.*;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.link.NullLink;
import org.talend.dataprep.transformation.pipeline.model.DiffWriterNode;
import org.talend.dataprep.transformation.pipeline.model.FilteredSourceNode;
import org.talend.dataprep.transformation.pipeline.node.BasicNode;

/**
 * Transformer that preview the transformation (puts additional json content so that the front can display the
 * difference between current and previous transformation).
 */
@Component
class PipelineDiffTransformer implements Transformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineDiffTransformer.class);

    @Autowired
    ActionParser actionParser;

    @Autowired
    ActionRegistry actionRegistry;

    @Autowired
    AnalyzerService analyzerService;

    @Autowired
    WriterRegistrationService writerRegistrationService;

    @Autowired
    StatisticsAdapter adapter;

    /**
     * Starts the transformation in preview mode.
     *
     * @param input the dataset content.
     * @param configuration The {@link Configuration configuration} for this transformation.
     */
    @Override
    public void transform(DataSet input, Configuration configuration) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null.");
        }
        final PreviewConfiguration previewConfiguration = (PreviewConfiguration) configuration;
        final RowMetadata rowMetadata = input.getMetadata().getRowMetadata();
        final TransformerWriter writer = writerRegistrationService.getWriter(configuration.formatId(), configuration.output(),
                configuration.getArguments());

        // Build diff pipeline
        Node diffWriterNode = new DiffWriterNode(2, writer);
        final String referenceActions = previewConfiguration.getReferenceActions();
        final String previewActions = previewConfiguration.getPreviewActions();
        final Pipeline referencePipeline = buildPipeline(rowMetadata, referenceActions, previewConfiguration.getReferenceContext(), diffWriterNode);
        final Pipeline previewPipeline = buildPipeline(rowMetadata, previewActions, previewConfiguration.getPreviewContext(), diffWriterNode);

        // Filter source records (extract TDP ids information)
        final List<Long> indexes = previewConfiguration.getIndexes();
        final boolean isIndexLimited = indexes != null && !indexes.isEmpty();
        final Long minIndex = isIndexLimited ? indexes.stream().mapToLong(Long::longValue).min().getAsLong() : 0L;
        final Long maxIndex = isIndexLimited ? indexes.stream().mapToLong(Long::longValue).max().getAsLong() : Long.MAX_VALUE;
        final Predicate<DataSetRow> filter = isWithinWantedIndexes(minIndex, maxIndex);

        // Build diff pipeline
        Node diffPipeline = NodeBuilder.filteredSource(filter) //
                .toMany() //
                .nodes(referencePipeline, previewPipeline) //
                .build();
        // Run diff
        try {
            // Print pipeline before execution (for debug purposes).
            LOGGER.debug("Before execution: {}", diffPipeline.toString());
            input.getRecords().forEach(r -> diffPipeline.receive(r, rowMetadata));
            diffPipeline.signal(Signal.END_OF_STREAM);
        } finally {
            // Print pipeline after execution (for debug purposes).
            LOGGER.debug("After execution: {}", diffPipeline.toString());
        }
    }

    private Pipeline buildPipeline(RowMetadata rowMetadata,
                                   String actions,
                                   TransformationContext transformationContext,
                                   Node output) {
        return Pipeline.Builder.builder() //
                .withActionRegistry(actionRegistry) //
                .withActions(actionParser.parse(actions)) //
                .withInitialMetadata(rowMetadata) //
                .withOutput(() -> output) //
                .withContext(transformationContext) //
                .withStatisticsAdapter(adapter) //
                .build();
    }

    @Override
    public boolean accept(Configuration configuration) {
        return PreviewConfiguration.class.isAssignableFrom(configuration.getClass());
    }

    private Predicate<DataSetRow> isWithinWantedIndexes(Long minIndex, Long maxIndex) {
        return row -> row.getTdpId() >= minIndex && row.getTdpId() <= maxIndex;
    }
}
