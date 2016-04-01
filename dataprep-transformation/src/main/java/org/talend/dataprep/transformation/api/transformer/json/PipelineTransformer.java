package org.talend.dataprep.transformation.api.transformer.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.statistics.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.api.action.ActionParser;
import org.talend.dataprep.transformation.api.transformer.Transformer;
import org.talend.dataprep.transformation.api.transformer.TransformerWriter;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.transformation.format.WriterRegistrationService;
import org.talend.dataprep.transformation.pipeline.ActionRegistry;
import org.talend.dataprep.transformation.pipeline.Pipeline;
import org.talend.dataprep.transformation.pipeline.model.WriterNode;

@Component
public class PipelineTransformer implements Transformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineTransformer.class);

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

    @Override
    public void transform(DataSet input, Configuration configuration) {
        final RowMetadata rowMetadata = input.getMetadata().getRowMetadata();
        final TransformerWriter writer = writerRegistrationService.getWriter(configuration.formatId(), configuration.output(), configuration.getArguments());
        final Pipeline pipeline = Pipeline.Builder.builder()
                .withActionRegistry(actionRegistry)
                .withActions(actionParser.parse(configuration.getActions()))
                .withInitialMetadata(rowMetadata)
                .withInlineAnalysis(analyzerService::schemaAnalysis)
                .withDelayedAnalysis(columns -> {
                    if (columns.isEmpty()) {
                        return NullAnalyzer.INSTANCE;
                    } else {
                        return analyzerService.full(columns);
                    }
                })
                .withOutput(() -> new WriterNode(writer))
                .withContext(configuration.getTransformationContext())
                .withStatisticsAdapter(adapter)
                .build();
        LOGGER.debug("Before transformation: {}", pipeline);
        pipeline.execute(input);
        LOGGER.debug("After transformation: {}", pipeline);
    }

    @Override
    public boolean accept(Configuration configuration) {
        return Configuration.class.equals(configuration.getClass()) && configuration.volume() == Configuration.Volume.SMALL;
    }
}
