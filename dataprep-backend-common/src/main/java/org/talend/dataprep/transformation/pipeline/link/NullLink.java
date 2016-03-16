package org.talend.dataprep.transformation.pipeline.link;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * Equivalent for a /dev/null: don't emit nor propagate information to any other Node.
 */
public class NullLink implements Link {

    public static final Link INSTANCE = new NullLink();

    private NullLink() {
    }

    @Override
    public void emit(DataSetRow row, RowMetadata metadata) {
        // Nothing to do
    }

    @Override
    public void accept(Visitor visitor) {
        // Nothing to do
    }

    @Override
    public void signal(Signal signal) {
        // Nothing to do
    }
}
