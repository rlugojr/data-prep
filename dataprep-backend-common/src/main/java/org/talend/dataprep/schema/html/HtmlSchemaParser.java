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

package org.talend.dataprep.schema.html;

import java.io.InputStream;
import java.util.*;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.schema.SchemaParser;
import org.talend.dataprep.schema.Schema;

/**
 * This class is in charge of parsing html file to discover schema.
 * 
 */
@Service("parser#html")
public class HtmlSchemaParser implements SchemaParser {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlSchemaParser.class);

    /** HTML header selector parameter key. */
    public static final String HEADER_SELECTOR_KEY = "html.HEADER_SELECTOR_KEY";

    /** HTML value selector parameter key. */
    public static final String VALUES_SELECTOR_KEY = "html.VALUES_SELECTOR_KEY";

    /** List of patterns to use to parse datasets out of html. */
    private Pattern pattern = new Pattern("html body table tr th", "html body table tr td");

    /**
     * @see SchemaParser#parse(Request)
     */
    @Override
    public Schema parse(Request request) {

        try {
            Map<String, String> parameters = new HashMap<>(2);
            parameters.put(HEADER_SELECTOR_KEY, pattern.getHeaderSelector());
            parameters.put(VALUES_SELECTOR_KEY, pattern.getValuesSelector());
            request.getMetadata().getContent().setParameters(parameters);
            final String headerSelector = pattern.getHeaderSelector();

            HeadersContentHandler headersContentHandler = new HeadersContentHandler(headerSelector, false);

            InputStream inputStream = request.getContent();
            HtmlParser htmlParser = new HtmlParser();

            Metadata metadata = new Metadata();

            htmlParser.parse(inputStream, headersContentHandler, metadata, new ParseContext());

            List<ColumnMetadata> columnMetadatas = new ArrayList<>(headersContentHandler.getHeaderValues().size());

            for (String headerValue : headersContentHandler.getHeaderValues()) {
                columnMetadatas.add(ColumnMetadata.Builder.column() //
                        .type(Type.STRING) // ATM not doing any complicated type calculation
                        .name(headerValue) //
                        .id(columnMetadatas.size()) //
                        .build());
            }

            Schema.SheetContent sheetContent = new Schema.SheetContent();
            sheetContent.setColumnMetadatas(columnMetadatas);

            return Schema.Builder.parserResult() //
                    .sheetContents(Collections.singletonList(sheetContent)) //
                    .draft(false) //
                    .build();

        } catch (Exception e) {
            LOGGER.debug("Exception during parsing html request :" + e.getMessage(), e);
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }

    }

    /**
     * Class used to setup a HeadersContentHandler.
     */
    static class Pattern {

        /** CSS like selector to get the header out of the html. */
        private String headerSelector;

        /** CSS like selector to get the values out of the html. */
        private String valuesSelector;

        /**
         * Constructor.
         *
         * @param headerSelector selector to get the header out of the html.
         * @param valuesSelector selector to get the values out of the html.
         */
        public Pattern(String headerSelector, String valuesSelector) {
            this.headerSelector = headerSelector;
            this.valuesSelector = valuesSelector;
        }

        /**
         * @return the css like selector used to get the headers.
         */
        public String getHeaderSelector() {
            return headerSelector;
        }

        /**
         * @return the css like selector used to get the values.
         */
        public String getValuesSelector() {
            return valuesSelector;
        }

        /**
         * @see Object#toString()
         */
        @Override
        public String toString() {
            return "Pattern{" + "headerSelector='" + headerSelector + '\'' + ", valuesSelector='" + valuesSelector + '\'' + '}';
        }
    }
}
