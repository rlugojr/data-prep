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

package org.talend.dataprep.schema.xls.serialization;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.slf4j.Logger;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.schema.xls.XlsUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.ctc.wstx.sax.WstxSAXParserFactory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Serialize XLSX file.
 */
public class XlsxRunnable implements Runnable {

    /** This class' logger. */
    private static final Logger LOG = getLogger(XlsxRunnable.class);

    /** Where to serialize the json. */
    private final OutputStream jsonOutput;

    /** The xlsx raw input. */
    private final InputStream rawContent;

    /** The dataset metadata to serialize. */
    private final DataSetMetadata metadata;

    /** The jackson factory to use for the serialization. */
    private final JsonFactory jsonFactory;

    /**
     * Constructor.
     * 
     * @param jsonOutput Where to serialize the json.
     * @param rawContent The xlsx raw input.
     * @param metadata The dataset metadata to serialize.
     * @param factory The jackson factory to use for the serialization.
     */
    public XlsxRunnable(OutputStream jsonOutput, InputStream rawContent, DataSetMetadata metadata, JsonFactory factory) {
        this.jsonOutput = jsonOutput;
        this.rawContent = rawContent;
        this.metadata = metadata;
        this.jsonFactory = factory;
    }

    /**
     * @see Runnable#run()
     */
    @Override
    public void run() {
        try {
            JsonGenerator generator = jsonFactory.createGenerator(jsonOutput);

            OPCPackage container = OPCPackage.open(rawContent);

            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(container);
            XSSFReader xssfReader = new XSSFReader(container);

            List<String> activeSheetNames = XlsUtils.getActiveSheetsFromWorkbookSpec(xssfReader.getWorkbookData());

            StylesTable styles = xssfReader.getStylesTable();

            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

            generator.writeStartArray();

            while (sheetIterator.hasNext()) {
                try (InputStream sheetInputStream = sheetIterator.next()) {
                    String sheetName = sheetIterator.getSheetName();

                    // we ignore non active sheets and non selected one
                    if (!activeSheetNames.contains(sheetName) || (metadata.getSheetName() != null //
                            && !StringUtils.equals(metadata.getSheetName(), sheetName))) {
                        continue;
                    }

                    InputSource sheetSource = new InputSource(sheetInputStream);

                    XlsxContentHandler defaultSheetContentsHandler = new XlsxContentHandler(generator, metadata);

                    XMLReader sheetParser = new WstxSAXParserFactory().newSAXParser().getXMLReader();
                    ContentHandler handler = new XSSFSheetXMLHandler(styles, //
                            strings, //
                            defaultSheetContentsHandler, //
                            new CustomDataFormatter(),
                            false);

                    sheetParser.setContentHandler(handler);
                    sheetParser.parse(sheetSource);

                    generator.writeEndArray();

                    generator.flush();

                    // we don't care of other sheets
                    return;
                }
            }

        } catch (Exception e) {
            // Consumer may very well interrupt consumption of stream (in case of limit(n) use for sampling).
            // This is not an issue as consumer is allowed to partially consumes results, it's up to the
            // consumer to ensure data it consumed is consistent.
            LOG.debug("Unable to continue serialization for {}. Skipping remaining content.", metadata.getId(), e);
        } finally {
            try {
                jsonOutput.close();
            } catch (IOException e) {
                LOG.error("Unable to close output", e);
            }
        }
    }

    private static class CustomDataFormatter extends DataFormatter {

        @Override
        public String formatRawCellContents(double value, int formatIndex, String formatString, boolean use1904Windowing) {
            // TDP-1656 (olamy) for some reasons poi use date format with only 2 digits for years
            // even the excel data ws using 4 so force the pattern here
            if (DateUtil.isValidExcelDate(value) && StringUtils.countMatches(formatString, "y") == 2) {
                formatString = StringUtils.replace(formatString, "yy", "yyyy");
            }
            if (DateUtil.isValidExcelDate(value) && StringUtils.countMatches(formatString, "Y") == 2) {
                formatString = StringUtils.replace(formatString, "YY", "YYYY");
            }
            return super.formatRawCellContents(value, formatIndex, formatString, use1904Windowing);

        }
    }   
}
