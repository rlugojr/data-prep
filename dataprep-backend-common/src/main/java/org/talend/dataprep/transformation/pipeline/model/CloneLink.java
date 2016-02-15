package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;

public class CloneLink implements Link {

    private final Node[] nodes;

    public CloneLink(Node... nodes) {
        this.nodes = nodes;
    }

    @Override
    public void emit(DataSetRow row, RowMetadata metadata) {
        for (Node node : nodes) {
            node.receive(row.clone(), metadata.clone());
        }
    }

    @Override
    public void signal(Signal signal) {
        for (Node node : nodes) {
            node.signal(signal);
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitCloneLink(this);
    }

    public Node[] getNodes() {
        return nodes;
    }
}
