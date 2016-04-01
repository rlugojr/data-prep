package org.talend.dataprep.transformation.pipeline.node;

import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.link.NullLink;

public class TerminalNode extends BasicNode {

    public TerminalNode() {
        link = NullLink.INSTANCE;
    }

    @Override
    public void setLink(Link link) {
        throw new UnsupportedOperationException();
    }
}
