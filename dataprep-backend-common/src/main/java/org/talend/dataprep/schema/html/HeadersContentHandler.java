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
 * SAX ContentHandler to get headers values. Sax is used for low memory usage, and a HeadersContentFoundException is
 * thrown when the header is found.
 */
public class HeadersContentHandler extends DefaultHandler {

    /** This class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlSchemaParser.class);

    /** CSS selector like to follow to get to the headers within the document. */
    private final List<String> headersLocation;

    /** Current currentLocation while reading the location. */
    private final ArrayDeque<String> currentLocation = new ArrayDeque<>();

    /** True if reading headers (current location matches the headers location). */
    private boolean readingHeaders;

    /** The headers values. */
    private List<String> headerValues = new ArrayList<>();

    /** flag to manage empty cells (empty String as value). */
    private boolean valueFound;

    /** if <code>true</code> will stop the processing when reading the first headers. */
    private boolean fastStop;

    /**
     * Constructor.
     * 
     * @param headerSelector an html element selector corresponding to headers "html body table tr th" <b>attributes not
     * supported</b>.
     */
    public HeadersContentHandler(String headerSelector, boolean fastStop) {
        this.headersLocation = Arrays.asList(StringUtils.split(headerSelector, ' '));
        this.fastStop = fastStop;
    }

    /**
     * Exception throw when the header is read to stop the sax processing in fast mode.
     */
    public static class HeadersContentFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public HeadersContentFoundException() {
            // no op
        }
    }

    /**
     * @return the header values.
     */
    public List<String> getHeaderValues() {
        return headerValues;
    }

    /**
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        currentLocation.addLast(localName);
        if (currentLocation.containsAll(headersLocation)) {
            readingHeaders = true;
        }
        valueFound = false;
    }

    /**
     * @see DefaultHandler#endElement(String, String, String)
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String top = currentLocation.getLast();
        if (StringUtils.equals(top, localName)) {
            // stop when all headers have been read once
            if (currentLocation.containsAll(headersLocation)) {
                if (this.fastStop) {
                    throw new HeadersContentFoundException();
                }
                readingHeaders = true;
            } else {
                readingHeaders = false;
            }
        }
        valueFound = false;
        currentLocation.removeLast();
    }

    /**
     * @see DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        if (readingHeaders) {
            char[] thechars = new char[length];
            System.arraycopy(chars, start, thechars, 0, length);
            String headerValue = new String(thechars);
            if (!valueFound) {
                headerValues.add(headerValue);
            } else {
                // characters method is called with some buffering so can be called multiple time over the same cell.
                headerValues.set(headerValues.size() - 1, headerValues.get(headerValues.size() - 1) + headerValue);
            }
            LOGGER.debug("header: {}", headerValue);
        }
        valueFound = true;
    }

}
