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

package org.talend.dataprep.transformation.api.action.metadata.common;

import static com.google.common.collect.Sets.newHashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.api.action.metadata.AbstractMetadataBaseTest;

public class ActionMetadataUtilsTest extends AbstractMetadataBaseTest {

    @Before
    public void init() {
        ActionMetadataUtils.reinitializeCache();
    }

    @Test
    public void should_cache_analyzer_after_check_integer_validity() {
        ColumnMetadata columnMetadata = ColumnMetadata.Builder.column() //
                .type(Type.INTEGER) //
                .computedId("0001") //
                .invalidValues(newHashSet()) //
                .build();

        ActionMetadataUtils.checkInvalidValue(columnMetadata, "1");
        Assert.assertTrue(ActionMetadataUtils.getAnalyzerCache().get(Type.INTEGER.getName()) != null);

    }

    @Test
    public void should_cache_analyzer_after_check_double_validity() {
        ColumnMetadata columnMetadata = ColumnMetadata.Builder.column() //
                .type(Type.DOUBLE) //
                .computedId("0001") //
                .invalidValues(newHashSet()) //
                .build();

        ActionMetadataUtils.checkInvalidValue(columnMetadata, "1");
        Assert.assertTrue(ActionMetadataUtils.getAnalyzerCache().get(Type.DOUBLE.getName()) != null);

    }

    @Test
    public void should_cache_analyzer_after_check_boolean_validity() {
        ColumnMetadata columnMetadata = ColumnMetadata.Builder.column() //
                .type(Type.BOOLEAN) //
                .computedId("0001") //
                .invalidValues(newHashSet()) //
                .build();

        ActionMetadataUtils.checkInvalidValue(columnMetadata, "1");
        Assert.assertTrue(ActionMetadataUtils.getAnalyzerCache().get(Type.BOOLEAN.getName()) != null);

    }

    @Test
    public void should_cache_analyzer_after_check_date_validity() {
        ColumnMetadata columnMetadata = ColumnMetadata.Builder.column() //
                .type(Type.DATE) //
                .computedId("0001") //
                .invalidValues(newHashSet()) //
                .build();

        ActionMetadataUtils.checkInvalidValue(columnMetadata, "1");
        Assert.assertTrue(ActionMetadataUtils.getAnalyzerCache().get(Type.DATE.getName()) != null);

    }

    @Test
    public void should_cache_analyzer_after_check_domain_validity() {
        ColumnMetadata columnMetadata = ColumnMetadata.Builder.column() //
                .type(Type.STRING) //
                .computedId("0001") //
                .invalidValues(newHashSet()).domain("CITY") //
                .build();

        ActionMetadataUtils.checkInvalidValue(columnMetadata, "Toto");
        Assert.assertTrue(ActionMetadataUtils.getAnalyzerCache().get("CITY") != null);

    }

}