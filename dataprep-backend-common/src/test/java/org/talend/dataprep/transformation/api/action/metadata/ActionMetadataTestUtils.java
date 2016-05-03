//  ============================================================================
//
//  Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.transformation.api.action.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.statistics.Statistics;
import org.talend.dataprep.api.preparation.Actions;
import org.talend.dataprep.api.preparation.json.MixedContentMapModule;
import org.talend.dataprep.api.type.Type;
import org.talend.dataquality.semantic.classifier.SemanticCategoryEnum;

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
     *
     * @param type the wanted data type.
     * @param semanticCategory the wanted semantic category.
     * @return a new column that matches the given data type and semantic domain.
     */
    public static ColumnMetadata getColumn(Type type, SemanticCategoryEnum semanticCategory) {
        return ColumnMetadata.Builder.column().id(0).name("name").type(type).domain(semanticCategory.name()).build();
    }

    /**
     * Parse the given input stream into a parameter map.
     *
     * @param input the parameters input stream.
     * @return the parsed parameters.
     * @throws IOException if an error occurs.
     */
    public static Map<String, String> parseParameters(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MixedContentMapModule());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Actions parsedAction = mapper.readerFor(Actions.class).readValue(input);
        return parsedAction.getActions().get(0).getParameters();
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
        int i = 0;
        for (String value : values) {
            rowValues.put(format.format(i), value);
            i++;
        }
        return new DataSetRow(rowValues);
    }

}
