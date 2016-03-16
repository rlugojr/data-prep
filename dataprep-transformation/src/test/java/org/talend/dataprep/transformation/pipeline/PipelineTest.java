package org.talend.dataprep.transformation.pipeline;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.statistics.StatisticsAdapter;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.TransformationBaseTest;
import org.talend.dataprep.transformation.api.action.ActionParser;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;
import org.talend.dataprep.transformation.format.CSVWriter;
import org.talend.dataprep.transformation.pipeline.model.WriterNode;

public class PipelineTest extends TransformationBaseTest {

    @Autowired
    ActionParser actionParser;

    @Autowired
    AnalyzerService analyzerService;

    @Autowired
    ActionRegistry actionRegistry;

    @Test
    public void should_return_expected_actions() throws IOException {
        // given
        Map<String, String> values = new HashMap<>();
        values.put("0000", "Test1");
        values.put("0001", "Test2");
        values.put("0002", "Constant");
        DataSetRow row1 = new DataSetRow(values);
        values = new HashMap<>();
        values.put("0000", "Test3");
        values.put("0001", "Test4");
        values.put("0002", "Constant");
        DataSetRow row2 = new DataSetRow(values);
        values = new HashMap<>();
        values.put("0000", "Test5");
        values.put("0001", "Test6");
        values.put("0002", "Constant");
        DataSetRow row3 = new DataSetRow(values);
        String json = IOUtils.toString(PipelineTest.class.getResourceAsStream("actions.json"));

        // when
        final DataSet dataSet = new DataSet();
        final DataSetMetadata dataSetMetadata = new DataSetMetadata();
        dataSetMetadata.setRowMetadata(row1.getRowMetadata());
        dataSet.setMetadata(dataSetMetadata);
        dataSet.setRecords(Arrays.asList(row1, row2, row3).stream());
        Pipeline pipeline = Pipeline.PipelineBuilder.pipeline()
                .withActionRegistry(actionRegistry)
                .withInitialMetadata(row1.getRowMetadata())
                .withActions(actionParser.parse(json))
                .withOutput(() -> new WriterNode(new CSVWriter(System.out)))
                .withInlineAnalysis(analyzerService::schemaAnalysis)
                .withDelayedAnalysis(analyzerService::full)
                .withContext(new TransformationContext())
                .withStatisticsAdapter(new StatisticsAdapter())
                .build();
        System.out.println("- BEFORE EXECUTION -");
        System.out.println(pipeline);
        System.out.println("- EXECUTION -");
        pipeline.execute(dataSet);
        System.out.println("- AFTER EXECUTION -");
        System.out.println(pipeline);
    }

}