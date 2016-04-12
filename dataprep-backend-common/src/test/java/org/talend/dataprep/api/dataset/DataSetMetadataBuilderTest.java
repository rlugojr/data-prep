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

package org.talend.dataprep.api.dataset;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.talend.dataprep.api.dataset.ColumnMetadata.Builder.column;
import static org.talend.dataprep.api.dataset.DataSetGovernance.Certification.CERTIFIED;
import static org.talend.dataprep.api.type.Type.INTEGER;
import static org.talend.dataprep.api.type.Type.STRING;

import org.apache.poi.ss.formula.functions.Match;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.talend.dataprep.api.dataset.location.HttpLocation;
import org.talend.dataprep.api.dataset.location.LocalStoreLocation;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.schema.FormatGuess;
import org.talend.dataprep.schema.SchemaParserResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DataSetMetadataBuilderTest.class)
@Configuration
@ComponentScan(basePackages = "org.talend.dataprep")
@EnableAutoConfiguration
public class DataSetMetadataBuilderTest {

    @Autowired
    private DataSetMetadataBuilder builder;

    @Test(expected = IllegalStateException.class)
    public void testExpectedId() {
        builder.metadata().build();
    }

    @Test(expected = IllegalStateException.class)
    public void testUseOfMetadata() {
        builder.id("12").build();
    }

    @Test
    public void testId() throws Exception {
        assertEquals("1234", builder.metadata().id("1234").build().getId());
    }

    @Test
    public void testAuthor() throws Exception {
        assertEquals("author", builder.metadata().id("1234").author("author").build().getAuthor());
    }

    @Test
    public void testName() throws Exception {
        assertEquals("name", builder.metadata().id("1234").name("name").build().getName());
    }

    @Test
    public void testVersion() throws Exception {
        // default version
        assertNotNull(builder.metadata().id("4321").build().getAppVersion());
        // set version
        assertEquals("1.0.PE", builder.metadata().id("4321").appVersion("1.0.PE").build().getAppVersion());
    }

    @Test
    public void testCreated() throws Exception {
        assertEquals(1234, builder.metadata().id("1234").created(1234).build().getCreationDate());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(30000, builder.metadata().id("1234").size(30000).build().getContent().getNbRecords());
    }

    @Test
    public void testHeaderSize() throws Exception {
        assertEquals(1, builder.metadata().id("1234").headerSize(1).build().getContent().getNbLinesInHeader());
    }

    @Test
    public void testFooterSize() throws Exception {
        assertEquals(1, builder.metadata().id("1234").footerSize(1).build().getContent().getNbLinesInFooter());
    }

    @Test
    public void testContentAnalyzed() throws Exception {
        final DataSetMetadata metadata = builder.metadata().id("1234").contentAnalyzed(false).build();
        assertEquals(false, metadata.getLifecycle().contentIndexed());
        metadata.getLifecycle().contentIndexed(true);
        assertEquals(true, metadata.getLifecycle().contentIndexed());
    }

    @Test
    public void testSchemaAnalyzed() throws Exception {
        final DataSetMetadata metadata = builder.metadata().id("1234").schemaAnalyzed(false).build();
        assertEquals(false, metadata.getLifecycle().schemaAnalyzed());
        metadata.getLifecycle().schemaAnalyzed(true);
        assertEquals(true, metadata.getLifecycle().schemaAnalyzed());
    }

    @Test
    public void testQualityAnalyzed() throws Exception {
        final DataSetMetadata metadata = builder.metadata().id("1234").qualityAnalyzed(false).build();
        assertEquals(false, metadata.getLifecycle().qualityAnalyzed());
        metadata.getLifecycle().qualityAnalyzed(true);
        assertEquals(true, metadata.getLifecycle().qualityAnalyzed());
    }

    @Test
    public void testImporting() throws Exception {
        final DataSetMetadata metadata = builder.metadata().id("1234").importing(true).build();
        assertEquals(true, metadata.getLifecycle().importing());
    }

    @Test
    public void testSheetName() throws Exception {
        assertEquals("test", builder.metadata().id("1234").sheetName("test").build().getSheetName());
    }

    @Test
    public void testDraft() throws Exception {
        assertEquals(true, builder.metadata().id("1234").draft(true).build().isDraft());
    }

    @Test
    public void testFormatGuessId() throws Exception {
        assertEquals("formatGuess",
                builder.metadata().id("1234").formatGuessId("formatGuess").build().getContent().getFormatGuessId());
    }

    @Test
    public void testMediaType() throws Exception {
        assertEquals("mediaType", builder.metadata().id("1234").mediaType("mediaType").build().getContent().getMediaType());
    }

    @Test
    public void testIsFavorite() throws Exception {
        assertEquals(true, builder.metadata().id("1234").isFavorite(true).build().isFavorite());
    }

    @Test
    public void testLocation() throws Exception {
        final DataSetMetadata metadata = builder.metadata().id("1234").location(new HttpLocation()).build();
        assertEquals(HttpLocation.class, metadata.getLocation().getClass());
    }

    @Test
    public void testCertificationStep() throws Exception {
        final DataSetMetadata metadata = builder.metadata().id("1234")
                .certificationStep(CERTIFIED).build();
        assertEquals(CERTIFIED, metadata.getGovernance().getCertificationStep());
    }

    @Test
    public void copyNonContentRelated_should_copy_non_content_related_metadata() throws Exception {
        //given
        final DataSetMetadata original = createCompleteMetadata();

        //when
        final DataSetMetadata copy = builder.metadata().copyNonContentRelated(original).build();

        //then
        assertNonContentRelatedMetadata(original, copy);
    }

    @Test
    public void copyContentRelated_should_copy_content_related_metadata() throws Exception {
        //given
        final DataSetMetadata original = createCompleteMetadata();

        //when
        final DataSetMetadata copy = builder.metadata().id("2f57de4641a66").copyContentRelated(original).build();

        //then
        assertContentRelatedMetadata(original, copy);
    }

    @Test
    public void copy_should_copy_all_metadata() throws Exception {
        //given
        final DataSetMetadata original = createCompleteMetadata();

        //when
        final DataSetMetadata copy = builder.metadata().copy(original).build();

        //then
        assertNonContentRelatedMetadata(original, copy);
        assertContentRelatedMetadata(original, copy);
    }

    @Test
    public void testSchemaParserResult() throws Exception {
        final DataSetMetadata metadata = builder.metadata().id("1234")
                .schemaParserResult(SchemaParserResult.Builder.parserResult().sheetName("sheetName").build()).build();
        assertEquals("sheetName", metadata.getSchemaParserResult().getSheetName());
    }

    private DataSetMetadata createCompleteMetadata() {
        final Map<String, String> parameters = new HashMap<>(0);
        parameters.put("encoding", "UTF-8");

        final List<ColumnMetadata> columnsMetadata = new ArrayList<>(2);
        columnsMetadata.add(ColumnMetadata.Builder.column().id(0).name("id").type(INTEGER).build());
        columnsMetadata.add(ColumnMetadata.Builder.column().id(1).name("Name").type(STRING).build());

        final RowMetadata rowMetadata = new RowMetadata();
        rowMetadata.setColumns(columnsMetadata);

        final DataSetContent content = new DataSetContent();
        content.setNbRecords(1000);
        content.setLimit(1000L);
        content.setNbLinesInHeader(10);
        content.setNbLinesInFooter(10);
        content.setFormatGuessId("formatGuess#csv");
        content.setMediaType("text/csv");
        content.setParameters(parameters);

        final SchemaParserResult schemaParserResult = new SchemaParserResult.Builder()
                .draft(true)
                .build();

        final DataSetMetadata metadata = new DataSetMetadata("18ba64c154d5", "Avengers stats", "Stan Lee", System.currentTimeMillis(), System.currentTimeMillis(), rowMetadata, "1.0");
        metadata.setFavorite(true);
        metadata.setLocation(new LocalStoreLocation());
        metadata.getGovernance().setCertificationStep(CERTIFIED);
        metadata.setSheetName("Sheet 1");
        metadata.setDraft(true);
        metadata.setContent(content);
        metadata.setEncoding("UTF-8");
        metadata.getLifecycle().contentIndexed(true);
        metadata.getLifecycle().qualityAnalyzed(true);
        metadata.getLifecycle().schemaAnalyzed(true);
        metadata.getLifecycle().inProgress(true);
        metadata.getLifecycle().importing(true);
        metadata.setSchemaParserResult(schemaParserResult);

        return metadata;
    }

    private void assertNonContentRelatedMetadata(final DataSetMetadata original, final DataSetMetadata copy) {
        assertThat(copy.getId(), equalTo(original.getId()));
        assertThat(copy.getAppVersion(), equalTo(original.getAppVersion()));
        assertThat(copy.getAuthor(), equalTo(original.getAuthor()));
        assertThat(copy.getCreationDate(), equalTo(original.getCreationDate()));
        assertThat(copy.isFavorite(), equalTo(original.isFavorite()));
        assertThat(copy.getLocation(), equalTo(original.getLocation()));
        assertThat(copy.getLastModificationDate(), equalTo(original.getLastModificationDate()));
    }

    private void assertContentRelatedMetadata(final DataSetMetadata original, final DataSetMetadata copy) {
        assertThat(copy.getGovernance().getCertificationStep(), equalTo(original.getGovernance().getCertificationStep()));
        assertThat(copy.getSheetName(), equalTo(original.getSheetName()));
        assertThat(copy.isDraft(), equalTo(original.isDraft()));
        assertThat(copy.getContent().getNbRecords(), equalTo(original.getContent().getNbRecords()));
        assertThat(copy.getContent().getLimit().get(), equalTo(original.getContent().getLimit().get()));
        assertThat(copy.getContent().getNbLinesInHeader(), equalTo(original.getContent().getNbLinesInHeader()));
        assertThat(copy.getContent().getNbLinesInFooter(), equalTo(original.getContent().getNbLinesInFooter()));
        assertThat(copy.getContent().getFormatGuessId(), equalTo(original.getContent().getFormatGuessId()));
        assertThat(copy.getContent().getMediaType(), equalTo(original.getContent().getMediaType()));
        assertThat(copy.getContent().getParameters(), equalTo(original.getContent().getParameters()));
        assertThat(copy.getEncoding(), equalTo(original.getEncoding()));
        assertThat(copy.getLifecycle().contentIndexed(), equalTo(original.getLifecycle().contentIndexed()));
        assertThat(copy.getLifecycle().qualityAnalyzed(), equalTo(original.getLifecycle().qualityAnalyzed()));
        assertThat(copy.getLifecycle().schemaAnalyzed(), equalTo(original.getLifecycle().schemaAnalyzed()));
        assertThat(copy.getLifecycle().inProgress(), equalTo(original.getLifecycle().inProgress()));
        assertThat(copy.getLifecycle().importing(), equalTo(original.getLifecycle().importing()));
        assertThat(copy.getSchemaParserResult(), equalTo(original.getSchemaParserResult()));
        final List<String> originalColumnsIds = original.getRowMetadata()
                .getColumns()
                .stream()
                .map(ColumnMetadata::getId)
                .collect(toList());
        final List<String> actualColumnsIds = copy.getRowMetadata()
                .getColumns()
                .stream()
                .map(ColumnMetadata::getId)
                .collect(toList());
        assertThat(actualColumnsIds, equalTo(originalColumnsIds));
    }
}