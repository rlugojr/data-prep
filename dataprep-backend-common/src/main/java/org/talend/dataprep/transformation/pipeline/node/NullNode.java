package org.talend.dataprep.transformation.pipeline.node;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.link.NullLink;

/**
 * Equivalent for a /dev/null for a Node: has a {@link NullLink} and do nothing on {@link #receive(DataSetRow, RowMetadata)}.
 */
public class NullNode implements Node {

    public static final Node INSTANCE = new NullNode();

    private NullNode() {
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        // Nothing to do
    }

    @Override
    public void setLink(Link link) {
        // Nothing to do
    }

    @Override
    public Link getLink() {
        return NullLink.INSTANCE;
    }

    @Override
    public void signal(Signal signal) {
        // Nothing to do
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitNode(this);
    }
}
