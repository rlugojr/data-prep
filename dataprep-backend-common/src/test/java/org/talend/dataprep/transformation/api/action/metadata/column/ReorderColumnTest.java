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
package org.talend.dataprep.transformation.api.action.metadata.column;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.talend.dataprep.api.dataset.ColumnMetadata.Builder.column;
import static org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils.getColumn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.api.action.ActionTestWorkbench;
import org.talend.dataprep.transformation.api.action.metadata.AbstractMetadataBaseTest;
import org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils;

/**
 * Test class for Split action. Creates one consumer, and test it.
 *
 * @see CopyColumnMetadata
 */
public class ReorderColumnTest extends AbstractMetadataBaseTest {

    /** The action to test. */
    @Autowired
    private Reorder action;

    private Map<String, String> parameters;

    @Before
    public void init() throws IOException {
        parameters = ActionMetadataTestUtils.parseParameters(ReorderColumnTest.class.getResourceAsStream("reorderAction.json"));
    }

    @Test
    public void testAdapt() throws Exception {
        assertThat(action.adapt((ColumnMetadata) null), is(action));
        ColumnMetadata column = column().name("myColumn").id(0).type(Type.STRING).build();
        assertThat(action.adapt(column), is(action));
    }

    @Test
    public void should_reorder_values_meta() {

        // given
        final List<ColumnMetadata> input = new ArrayList<>();
        input.add(createMetadata("0000", "foo"));
        input.add(createMetadata("0001", "bar"));
        final RowMetadata rowMetadata = new RowMetadata(input);
        final Map<String, String> values = new HashMap<>();
        values.put("0000", "foo value");
        values.put("0001", "bar value");
        final DataSetRow row = new DataSetRow(values);
        row.setRowMetadata(rowMetadata);

        // when
        ActionTestWorkbench.test(row, factory.create(action, parameters));

        // then
        Assertions.assertThat(row.getRowMetadata().getColumns()).isNotEmpty().hasSize(2);
        Assertions.assertThat(row.getRowMetadata().getColumns().get(0).getId()).isEqualTo("0001");
        Assertions.assertThat(row.getRowMetadata().getColumns().get(1).getId()).isEqualTo("0000");
        Assertions.assertThat(row.get("0000")).isEqualTo("bar value");
        Assertions.assertThat(row.get("0001")).isEqualTo("foo value");
    }

    @Test
    public void should_accept_column() {
        assertTrue(action.acceptColumn(getColumn(Type.ANY)));
    }

    @Override
    protected ColumnMetadata.Builder columnBaseBuilder() {
        return super.columnBaseBuilder().headerSize(12).valid(5).invalid(2);
    }
}
