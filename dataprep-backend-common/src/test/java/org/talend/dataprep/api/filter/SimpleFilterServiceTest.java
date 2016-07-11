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

package org.talend.dataprep.api.filter;

import static java.time.Month.JANUARY;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.transformation.actions.date.DateParser;

public class SimpleFilterServiceTest {

    private final SimpleFilterService service = new SimpleFilterService();

    private DataSetRow datasetRowFromValues;

    private DataSetRow datasetRowFromMetadata;

    private RowMetadata rowMetadata;

    @Before
    public void init() {
        datasetRowFromValues = new DataSetRow(new HashMap<>());

        final Set<String> invalidValues = new HashSet<>();
        invalidValues.add("invalid value");
        final ColumnMetadata column = new ColumnMetadata();
        column.setId("0001");
        column.getQuality().setInvalidValues(invalidValues);
        rowMetadata = new RowMetadata();
        rowMetadata.setColumns(singletonList(column));
        datasetRowFromMetadata = new DataSetRow(rowMetadata);
    }

    @Test
    public void should_create_TRUE_predicate_on_empty_filter() throws Exception {
        //given
        final String filtersDefinition = "";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        assertThat(filter.test(datasetRowFromValues), is(true));
    }

    @Test(expected = TDPException.class)
    public void should_throw_exception_on_empty_object_definition() throws Exception {
        //given
        final String filtersDefinition = "{}";

        //when
        service.build(filtersDefinition, rowMetadata);

        //then
    }

    @Test(expected = TDPException.class)
    public void should_throw_exception_on_invalid_definition() throws Exception {
        //given
        final String filtersDefinition = "}";

        //when
        service.build(filtersDefinition, rowMetadata);

        //then
    }

    @Test(expected = TDPException.class)
    public void should_create_unknown_filter() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"bouh\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": \"toto\"" +
                "   }" +
                "}";

        //when
        service.build(filtersDefinition, rowMetadata);

        //then
    }

    @Test
    public void should_create_EQ_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"eq\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": \"toto\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "toto");
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "Toto"); //different case
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "tatatoto"); //contains but different
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", ""); //empty
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", null); //null
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_EQ_predicate_more_number_format_integer_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"eq\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": \"5\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "5.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5,00"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "05.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "0 005"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "4.5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "4,5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ",5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ".5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1.000,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1 000.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_EQ_predicate_more_number_format_decimal_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"eq\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": \"5,35\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "5.35"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5,35"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "05.35"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "0 005.35"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "4.5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "4,5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ",5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ".5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1.000,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1 000.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_GT_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"gt\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": 5" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "6"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "4"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "toto"); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ""); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", null); //null
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "4.5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "4,5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ",5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ".5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "5.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "5,00"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "05.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "0 005"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "5.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "1.000,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "1 000.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
    }

    @Test
    public void should_create_GTE_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"gte\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": 5" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "6"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "4"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "toto"); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ""); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", null); //null
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "4.5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "4,5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ",5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ".5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "5.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5,00"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "05.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "0 005"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "5.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "1.000,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "1 000.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(true));
    }

    @Test
    public void should_create_LT_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"lt\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": 5" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "6"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "5"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "4"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "toto"); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ""); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", null); //null
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "4.5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "4,5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", ",5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", ".5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "5.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "5,00"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "05.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "0 005"); //eq
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "5.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "5,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1.000,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1 000.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_LTE_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"lte\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": 5" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "6"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "5"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "4"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "toto"); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ""); //nan
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", null); //null
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "4.5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "4,5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", ",5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", ".5"); //lt
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "5.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "5,00"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "05.0"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "0 005"); //eq
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "5.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "5,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1.000,5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "1 000.5"); //gt
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_CONTAINS_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"contains\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": \"toto\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "toto"); //equals
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "Toto"); //different case
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "tatatoto"); //contains but different
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "tagada"); // not contains
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_MATCHES_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"matches\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": \"Aa9-\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", "toto"); // different pattern
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", "To5-"); // same pattern
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "To5--"); // different length
        assertThat(filter.test(datasetRowFromValues), is(false));

        datasetRowFromValues.set("0001", ""); // empty value
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_MATCHES_predicate_empty_pattern() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"matches\": {" +
                "       \"field\": \"0001\"," +
                "       \"value\": \"\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", ""); // empty value
        assertThat(filter.test(datasetRowFromValues), is(true));

        datasetRowFromValues.set("0001", "tagada"); // not empty value
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_INVALID_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"invalid\": {" +
                "       \"field\": \"0001\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromMetadata.set("0001", "invalid value"); //value in invalid array in column metadata
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", ""); //empty but not invalid
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "toto"); //correct value
        assertThat(filter.test(datasetRowFromMetadata), is(false));
    }

    @Test
    public void should_create_VALID_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"valid\": {" +
                "       \"field\": \"0001\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromMetadata.set("0001", "invalid value"); //value in invalid array in column metadata
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", ""); //empty
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "toto"); //correct value
        assertThat(filter.test(datasetRowFromMetadata), is(true));
    }

    @Test
    public void should_create_EMPTY_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"empty\": {" +
                "       \"field\": \"0001\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromMetadata.set("0001", ""); //empty
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "toto"); //not empty value
        assertThat(filter.test(datasetRowFromMetadata), is(false));
    }

    @Test
    public void should_create_number_RANGE_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"range\": {" +
                "       \"field\": \"0001\"," +
                "       \"start\": \"5\"," +
                "       \"end\": \"10\"" +
                "   }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromMetadata.getRowMetadata().getById("0001").setType("integer");
        datasetRowFromMetadata.set("0001", "a"); //invalid number
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "4"); //lt min
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "5"); //eq min
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "8"); //in range
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "10"); //eq max
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "20"); //gt max
        assertThat(filter.test(datasetRowFromMetadata), is(false));

        datasetRowFromMetadata.set("0001", "toto"); //nan
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", ""); //nan
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", null); //null
        assertThat(filter.test(datasetRowFromMetadata), is(false));

        datasetRowFromMetadata.set("0001", "4.5"); //lt
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "4,5"); //lt
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", ",5"); //lt
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", ".5"); //lt
        assertThat(filter.test(datasetRowFromMetadata), is(false));

        datasetRowFromMetadata.set("0001", "5.0"); //eq
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "5,00"); //eq
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "05.0"); //eq
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "0 005"); //eq
        assertThat(filter.test(datasetRowFromMetadata), is(true));

        datasetRowFromMetadata.set("0001", "5.5"); //gt
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "5,5"); //gt
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "1.000,5"); //gt
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "1 000.5"); //gt
        assertThat(filter.test(datasetRowFromMetadata), is(false));
    }

    @Test
    public void should_create_date_RANGE_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"range\": {" +
                "       \"field\": \"0001\"," +
                "       \"start\": 0," + //1970-01-01 UTC timezone
                "       \"end\": " + (LocalDateTime.of(1990, JANUARY, 1, 0, 0).toEpochSecond(UTC) * 1000) + //1990-01-01 UTC timezone
                "   }" +
                "}";

        final ColumnMetadata column = datasetRowFromMetadata.getRowMetadata().getById("0001");
        column.setType("date");
        final DateParser dateParser = Mockito.mock(DateParser.class);
        when(dateParser.parse("a", column)).thenThrow(new DateTimeException(""));
        when(dateParser.parse("1960-01-01", column)).thenReturn(LocalDateTime.of(1960, JANUARY, 1, 0, 0));
        when(dateParser.parse("1970-01-01", column)).thenReturn(LocalDateTime.of(1970, JANUARY, 1, 0, 0));
        when(dateParser.parse("1980-01-01", column)).thenReturn(LocalDateTime.of(1980, JANUARY, 1, 0, 0));
        when(dateParser.parse("1990-01-01", column)).thenReturn(LocalDateTime.of(1990, JANUARY, 1, 0, 0));
        when(dateParser.parse("2000-01-01", column)).thenReturn(LocalDateTime.of(2000, JANUARY, 1, 0, 0));
        service.setDateParser(dateParser);

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromMetadata.set("0001", "a"); //invalid number
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "1960-01-01"); //lt min
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "1970-01-01"); //eq min
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "1980-01-01"); //in range
        assertThat(filter.test(datasetRowFromMetadata), is(true));
        datasetRowFromMetadata.set("0001", "1990-01-01"); //eq max
        assertThat(filter.test(datasetRowFromMetadata), is(false));
        datasetRowFromMetadata.set("0001", "2000-01-01"); //gt max
        assertThat(filter.test(datasetRowFromMetadata), is(false));
    }

    @Test
    public void should_create_AND_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"and\": [" +
                "       {" +
                "           \"empty\": {" +
                "               \"field\": \"0001\"" +
                "           }" +
                "       }," +
                "       {" +
                "           \"eq\": {" +
                "               \"field\": \"0002\"," +
                "               \"value\": \"toto\"" +
                "           }" +
                "       }" +
                "   ]" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", ""); //empty
        datasetRowFromValues.set("0002", "toto"); //eq value
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "tata"); //not empty
        datasetRowFromValues.set("0002", "toto"); //eq value
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", ""); //empty
        datasetRowFromValues.set("0002", "tata"); //neq value
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_OR_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"or\": [" +
                "       {" +
                "           \"empty\": {" +
                "               \"field\": \"0001\"" +
                "           }" +
                "       }," +
                "       {" +
                "           \"eq\": {" +
                "               \"field\": \"0002\"," +
                "               \"value\": \"toto\"" +
                "           }" +
                "       }" +
                "   ]" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", ""); //empty
        datasetRowFromValues.set("0002", "toto"); //eq value
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "tata"); //not empty
        datasetRowFromValues.set("0002", "toto"); //eq value
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", ""); //empty
        datasetRowFromValues.set("0002", "tata"); //neq value
        assertThat(filter.test(datasetRowFromValues), is(true));
        datasetRowFromValues.set("0001", "tata"); //not empty
        datasetRowFromValues.set("0002", "tata"); //neq value
        assertThat(filter.test(datasetRowFromValues), is(false));
    }

    @Test
    public void should_create_NOT_predicate() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"not\":" +
                "       {" +
                "           \"empty\": {" +
                "               \"field\": \"0001\"" +
                "           }" +
                "       }" +
                "}";

        //when
        final Predicate<DataSetRow> filter = service.build(filtersDefinition, rowMetadata);

        //then
        datasetRowFromValues.set("0001", ""); //empty
        assertThat(filter.test(datasetRowFromValues), is(false));
        datasetRowFromValues.set("0001", "toto"); //not empty
        assertThat(filter.test(datasetRowFromValues), is(true));
    }

    @Test(expected = TDPException.class)
    public void should_create_NOT_predicate_invalid1() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"not\": [" +
                "       {" +
                "           \"empty\": {" +
                "               \"field\": \"0001\"" +
                "           }" +
                "       }," +
                "       {" +
                "           \"eq\": {" +
                "               \"field\": \"0002\"," +
                "               \"value\": \"toto\"" +
                "           }" +
                "       }" +
                "   ]" +
                "}";

        //when
        service.build(filtersDefinition, rowMetadata);

        //then
    }

    @Test(expected = TDPException.class)
    public void should_create_NOT_predicate_invalid2() throws Exception {
        //given
        final String filtersDefinition = "{" +
                "   \"not\":" +
                "       {" +
                "       }" +
                "}";

        //when
        service.build(filtersDefinition, rowMetadata);

        //then
    }

}