package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;

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
