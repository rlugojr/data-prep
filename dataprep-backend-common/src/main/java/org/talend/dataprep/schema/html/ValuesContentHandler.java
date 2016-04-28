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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ContentHandler to get values from the selector
 */
public class ValuesContentHandler extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlSchemaParser.class);

    private final ArrayDeque<String> stack = new ArrayDeque<>();

    private boolean matchingValuePattern;

    /**
     * list of row values
     */
    private List<List<String>> values = new ArrayList<>();

    private final List<String> valueStack;

    /**
     * flag to manage empty cells (empty String as value)
     */
    private boolean valueFound;

    /**
     *
     * @param valueSelector an html element selector corresponding to values "html body table tr td" <b>attributes not
     * supported</b>
     */
    public ValuesContentHandler(String valueSelector) {
        this.valueStack = Arrays.asList(StringUtils.split(valueSelector, ' '));
    }

    public List<List<String>> getValues() {
        return values;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        stack.addLast(localName);
        if (stack.containsAll(valueStack)) {
            matchingValuePattern = true;
        }
        valueFound = false;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String top = stack.getLast();
        if (StringUtils.equals(top, localName)) {
            if (stack.containsAll(valueStack)) {
                if (!valueFound) {
                    values.get(values.size() - 1).add(StringUtils.EMPTY);
                }
            } else {
                if (matchingValuePattern) {
                    values.add(new ArrayList<>());
                }
                matchingValuePattern = false;
            }
        }
        valueFound = false;
        stack.removeLast();
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        if (matchingValuePattern) {
            char[] thechars = new char[length];
            System.arraycopy(chars, start, thechars, 0, length);
            String value = new String(thechars);
            LOGGER.debug("value: {}", value);
            if (values.isEmpty()) {
                values.add(new ArrayList<>());
            }
            List<String> currentRowValues = values.get(values.size() - 1);
            if (!valueFound) {
                currentRowValues.add(value);
            } else {
                // characters method is called with some buffering so can be called multiple time whereas it's the same
                // cell.
                String previous = currentRowValues.remove(currentRowValues.size() - 1);
                currentRowValues.add(previous + value);
            }
            valueFound = true;
        }
    }

}
