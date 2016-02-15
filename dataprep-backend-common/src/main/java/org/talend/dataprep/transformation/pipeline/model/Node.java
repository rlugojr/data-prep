package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;

/**
 * A node is a processing unit inside the transformation pipeline.
 */
public interface Node {

    /**
     * Called by an incoming {@link Link} when new row is submitted to the pipeline.
     * 
     * @param row A {@link DataSetRow row} to be processed by this node.
     * @param metadata The {@link RowMetadata row metadata} to be used when processing the <code>row</code>.
     */
    void receive(DataSetRow row, RowMetadata metadata);

    /**
     * Changes the {@link Link link} for output processing of this node.
     * 
     * @param link
     */
    void setLink(Link link);

    Link getLink();

    /**
     * Sends a {@link Signal event} to the node. Signals are data-independent events to indicate external events (such
     * as end of the stream). Node implementations are responsible of the signal propagation using the {@link Link link}
     * .
     *
     * @param signal A {@link Signal signal} to be sent to the pipeline.
     * @see #setLink(Link)
     */
    void signal(Signal signal);

    /**
     * Visit the implementation of the {@link Node}.
     *
     * @param visitor A {@link Visitor} to visit the whole pipeline structure.
     */
    void accept(Visitor visitor);
}
