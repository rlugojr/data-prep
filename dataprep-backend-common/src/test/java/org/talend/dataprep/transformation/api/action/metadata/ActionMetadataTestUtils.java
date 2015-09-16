package org.talend.dataprep.transformation.api.action.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.statistics.Statistics;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.api.action.metadata.common.ActionMetadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for action metadata unit tests.
 */
public class ActionMetadataTestUtils {

    /**
     *
     * @param type the wanted column type.
     * @return a new column that matches the given type.
     */
    public static ColumnMetadata getColumn(Type type) {
        return ColumnMetadata.Builder.column().id(0).name("name").type(type).build();
    }

    /**
     * Parse the given input stream into a parameter map.
     *
     * @param action the action to parse the parameters for.
     * @param input the parameters input stream.
     * @return the parsed parameters.
     * @throws IOException if an error occurs.
     */
    public static Map<String, String> parseParameters(ActionMetadata action, InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );
        JsonNode node = mapper.readTree(input);
        return action.parseParameters(node.get("actions").get(0).get("parameters").fields());
    }

    /**
     * Set the statistics to the given column on the given row.
     *
     * @param row the row to update.
     * @param columnId the column id.
     * @param statisticsContent the statistics in json as expected from the DQ library.
     * @throws IOException you never know :)
     */
    public static void setStatistics(DataSetRow row, String columnId, InputStream statisticsContent) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Statistics statistics = mapper.readValue(statisticsContent, Statistics.class);
        row.getRowMetadata().getById(columnId).setStatistics(statistics);
    }

    /**
     * Return a new dataset row with the given values.
     *
     * @param values the row values.
     * @return a new dataset row that have the given values.
     */
    public static DataSetRow getRow(String... values) {
        DecimalFormat format = new DecimalFormat("0000");
        Map<String, String> rowValues = new HashMap<>();
        int i=1;
        for (String value : values) {
            rowValues.put(format.format(i), value);
            i++;
        }
        return new DataSetRow(rowValues);
    }
}
