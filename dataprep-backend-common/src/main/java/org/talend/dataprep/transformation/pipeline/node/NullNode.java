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
public class NullNode extends TerminalNode {

    public static final Node INSTANCE = new NullNode();

    private NullNode() {
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        // Nothing to do
    }

}
