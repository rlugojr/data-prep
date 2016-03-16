package org.talend.dataprep.transformation.pipeline.model;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.api.transformer.TransformerWriter;
import org.talend.dataprep.transformation.pipeline.*;
import org.talend.dataprep.transformation.pipeline.link.NullLink;

public class WriterNode implements Node, Monitored {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriterNode.class);

    private final TransformerWriter writer;

    private RowMetadata lastRowMetadata;

    private boolean startRecords = false;

    private long totalTime;

    private int count;

    public WriterNode(TransformerWriter writer) {
        this.writer = writer;
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
            lastRowMetadata = metadata;
            if (row.shouldWrite()) {
                row.setRowMetadata(metadata);
                writer.write(row);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to write record.", e);
        } finally {
            totalTime += System.currentTimeMillis() - start;
            count++;
        }
    }

    @Override
    public void setLink(Link link) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Link getLink() {
        return NullLink.INSTANCE;
    }

    @Override
    public void signal(Signal signal) {
        if (signal == Signal.END_OF_STREAM || signal == Signal.CANCEL) {
            final long start = System.currentTimeMillis();
            try {
                writer.endArray(); // <- end records
                writer.fieldName("metadata"); // <- start metadata
                writer.startObject();
                {
                    writer.fieldName("columns");
                    writer.write(lastRowMetadata);
                }
                writer.endObject();
                writer.endObject(); // <- end data set
                writer.flush();
            } catch (IOException e) {
                LOGGER.error("Unable to end writer.", e);
            } finally {
                totalTime += System.currentTimeMillis() - start;
            }
        } else {
            LOGGER.debug("Unhandled signal {}.", signal);
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitNode(this);
    }

    @Override
    public long getTotalTime() {
        return totalTime;
    }

    @Override
    public long getCount() {
        return count;
    }

    public TransformerWriter getWriter() {
        return writer;
    }
}
