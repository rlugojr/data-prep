package org.talend.dataprep.transformation.pipeline.model;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.api.transformer.TransformerWriter;
import org.talend.dataprep.transformation.pipeline.*;
import org.talend.dataprep.transformation.pipeline.link.NullLink;

public class DiffWriterNode implements Node, Monitored {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiffWriterNode.class);

    private final int sourceNumber;

    private final TransformerWriter writer;

    private final Deque<DataSetRow> rowStack;

    private final Deque<RowMetadata> metadataStack;

    private long totalTime;

    private int count;

    private boolean startRecords;

    private boolean endMetadata;

    public DiffWriterNode(int sourceNumber, TransformerWriter writer) {
        this.sourceNumber = sourceNumber;
        this.writer = writer;
        rowStack = new ArrayDeque<>(sourceNumber);
        metadataStack = new ArrayDeque<>(sourceNumber);
    }

    @Override
    public long getTotalTime() {
        return totalTime;
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        final long start = System.currentTimeMillis();
        try {
            if (!startRecords) {
                writer.startObject();
                writer.fieldName("records");
                writer.startArray();
                startRecords = true;
            }
            // Values
            rowStack.push(row.clone());
            emptyRowSources();
            // Metadata
            if (metadataStack.size() % sourceNumber == 0) {
                metadataStack.clear();
            }
            metadataStack.push(metadata.clone());
        } catch (IOException e) {
            LOGGER.error("Unable to write record.", e);
        } finally {
            totalTime += System.currentTimeMillis() - start;
            count++;
        }
    }

    private void emptyRowSources() throws IOException {
        if (!rowStack.isEmpty() && rowStack.size() % sourceNumber == 0) {
            final DataSetRow initialRow = rowStack.pop();
            while (!rowStack.isEmpty()) {
                initialRow.diff(rowStack.pop());
            }
            if (initialRow.shouldWrite()) {
                writer.write(initialRow);
            }
        }
    }

    @Override
    public Link getLink() {
        return NullLink.INSTANCE;
    }

    @Override
    public void setLink(Link link) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void signal(Signal signal) {
        if ((signal == Signal.END_OF_STREAM || signal == Signal.CANCEL) && !endMetadata) {
            final long start = System.currentTimeMillis();
            try {
                emptyRowSources();
                writer.endArray(); // <- end records
                writer.fieldName("metadata"); // <- start metadata
                writer.startObject();
                {
                    writer.fieldName("columns");
                    final RowMetadata initialMetadata = metadataStack.pop();
                    while (!metadataStack.isEmpty()) {
                        initialMetadata.diff(metadataStack.pop());
                    }
                    // Preview don't need statistics, so wipe them out
                    for (ColumnMetadata column : initialMetadata.getColumns()) {
                        column.getStatistics().setInvalid(0);
                        column.getQuality().setInvalidValues(Collections.emptySet());
                        column.getQuality().setInvalid(0);
                    }
                    writer.write(initialMetadata);
                }
                writer.endObject();
                writer.endObject(); // <- end data set
                writer.flush();
            } catch (IOException e) {
                LOGGER.error("Unable to end writer.", e);
            } finally {
                totalTime += System.currentTimeMillis() - start;
                endMetadata = true;
            }
        } else {
            LOGGER.debug("Unhandled signal {}.", signal);
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitNode(this);
    }
}
