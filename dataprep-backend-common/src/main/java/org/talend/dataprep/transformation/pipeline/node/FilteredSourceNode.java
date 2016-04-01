package org.talend.dataprep.transformation.pipeline.node;

import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;

public class FilteredSourceNode extends SourceNode {

    private final Predicate<DataSetRow> filter;

    public FilteredSourceNode(Predicate<DataSetRow> filter) {
        this.filter = filter;
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        if (filter.test(row)) {
            super.receive(row, metadata);
        }
    }

}
