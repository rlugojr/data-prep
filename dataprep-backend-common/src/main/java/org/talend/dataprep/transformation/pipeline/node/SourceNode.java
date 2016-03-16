package org.talend.dataprep.transformation.pipeline.node;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.link.NullLink;

public class SourceNode implements Node {

    private Link link = NullLink.INSTANCE;

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        link.emit(row, metadata);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitSource(this);
        link.accept(visitor);
    }

    @Override
    public void setLink(Link link) {
        this.link = link;
    }

    @Override
    public Link getLink() {
        return link;
    }

    @Override
    public void signal(Signal signal) {
        link.signal(signal);
    }

}
