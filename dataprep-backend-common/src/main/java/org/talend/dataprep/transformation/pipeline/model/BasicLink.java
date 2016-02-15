package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;

public class BasicLink implements Link {

    private final Node target;

    public BasicLink(Node target) {
        this.target = target;
    }

    @Override
    public void emit(DataSetRow row, RowMetadata metadata) {
        target.receive(row, metadata);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitBasicLink(this);
    }

    @Override
    public void signal(Signal signal) {
        target.signal(signal);
    }

    public Node getTarget() {
        return target;
    }
}
