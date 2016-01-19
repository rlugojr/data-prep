package org.talend.dataprep.api.dataset;

import org.junit.Test;
import org.talend.dataprep.api.type.Type;

import static org.junit.Assert.*;
import static org.talend.dataprep.api.dataset.ColumnMetadata.Builder.column;
import static org.talend.dataprep.api.dataset.DataSetMetadata.Builder.metadata;

public class DataSetMetadataTest {
    @Test
    public void should_be_compatible() throws Exception {
        final DataSetMetadata metadata1 = metadata().id("0001") //
                .row(column().type(Type.STRING).name("first"), column().type(Type.STRING).name("last")) //
                .build();

        final DataSetMetadata metadata2 = metadata().id("0002") //
                .row(column().type(Type.STRING).name("last"), column().type(Type.STRING).name("first")) //
                .build();

        final DataSetMetadata clone = metadata1.clone();
        assertTrue(metadata1.compatible(metadata1));
        assertTrue(metadata1.compatible(metadata2));
        assertTrue(metadata1.compatible(clone));
    }

    @Test
    public void should_be_incompatible() throws Exception {
        final DataSetMetadata metadata1 = metadata().id("0001") //
                .row(column().type(Type.STRING).name("first"), column().type(Type.INTEGER).name("last")) //
                .build();

        final DataSetMetadata metadata2 = metadata().id("0002") //
                .row(column().type(Type.STRING).name("last"), column().type(Type.STRING).name("first")) //
                .build();

        assertFalse(metadata1.compatible(null));
        assertFalse(metadata1.compatible(metadata2));
    }

}