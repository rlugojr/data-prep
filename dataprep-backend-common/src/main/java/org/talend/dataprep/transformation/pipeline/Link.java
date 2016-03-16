package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;

/**
 * Links together {@link Node nodes}. Each implementation is responsible for handling correct
 * {@link #emit(DataSetRow, RowMetadata)} and {@link #signal(Signal)} methods.
 */
public interface Link {

    /**
     * Emits a new row and its corresponding metadata.
     * 
     * @param row A {@link DataSetRow row} to emit to the next {@link Node}.
     * @param metadata The {@link RowMetadata row metadata} to be used by the next {@link Node}.
     */
    void emit(DataSetRow row, RowMetadata metadata);

    /**
     * Sends a {@link Signal event} to the {@link Node}. Signals are data-independent events to indicate external events
     * (such as end of the stream).
     * 
     * @param signal A {@link Signal signal} to be sent to the pipeline.
     */
    void signal(Signal signal);

    /**
     * Visit the implementation of the {@link Link}.
     * 
     * @param visitor A {@link Visitor} to visit the whole pipeline structure.
     */
    void accept(Visitor visitor);
}
