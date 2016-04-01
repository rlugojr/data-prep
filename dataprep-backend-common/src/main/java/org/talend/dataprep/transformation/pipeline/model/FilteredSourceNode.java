package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.link.NullLink;

import java.util.function.Predicate;

public class FilteredSourceNode implements Node {

    private final Predicate<DataSetRow> filter;

    private Link link = NullLink.INSTANCE;

    public FilteredSourceNode(Predicate<DataSetRow> filter) {
        this.filter = filter;
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        if (filter.test(row)) {
            link.emit(row, metadata);
        }
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

    @Override
    public void accept(Visitor visitor) {
        visitor.visitNode(this);
    }
}
