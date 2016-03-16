package org.talend.dataprep.transformation.pipeline.link;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.Visitor;

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
