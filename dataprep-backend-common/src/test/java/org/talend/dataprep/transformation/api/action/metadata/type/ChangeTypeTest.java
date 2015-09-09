package org.talend.dataprep.transformation.api.action.metadata.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;

public class ChangeTypeTest {

    @Test
    public void should_change_type() throws Exception {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0001", "David Bowie");
        values.put("0002", "");
        values.put("0003", "Something");

        final RowMetadata rowMetadata = new RowMetadata();
        rowMetadata.setColumns(Arrays.asList(ColumnMetadata.Builder.column() //
                .type(Type.INTEGER) //
                .computedId("0002") //
                .domain("FR_BEER") //
                .domainFrequency(1) //
                .domainLabel("French Beer") //
                .build()));

        final DataSetRow row = new DataSetRow(values);
        row.setRowMetadata(rowMetadata);

        TypeChange typeChange = new TypeChange();

        TransformationContext transformationContext = new TransformationContext();

        Map<String, String> parameters = new HashMap<>();
        parameters.put(TypeChange.NEW_TYPE_PARAMETER_KEY, "STRING");

        // when
        typeChange.applyOnColumn(row, transformationContext, parameters, "0002");

        // then
        Assertions.assertThat(row.getRowMetadata().getColumns().get(0).getDomain()).isEmpty();
        Assertions.assertThat(row.getRowMetadata().getColumns().get(0).getDomainLabel()).isEmpty();
        Assertions.assertThat(row.getRowMetadata().getColumns().get(0).getDomainFrequency()).isEqualTo(0);

        Set<String> forcedColumns = typeChange.getForcedColumns(transformationContext);

        Assertions.assertThat(forcedColumns).isNotNull().isNotEmpty().hasSize(1).contains("0002");
    }

    @Test
    public void should_not_apply() {
        TypeChange typeChange = new TypeChange();

        for (Type type : Type.values()) {

            ColumnMetadata columnMetadata = ColumnMetadata.Builder.column() //
                .type(type) //
                .computedId("0002") //
                .domain("FR_BEER") //
                .domainFrequency(1) //
                .domainLabel("French Beer") //
                .build();

            Assertions.assertThat(typeChange.acceptColumn(columnMetadata)).isFalse();
        }
    }
}