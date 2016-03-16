package org.talend.dataprep.transformation.pipeline.node;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.transformation.api.action.context.ActionContext;
import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.link.NullLink;

public class CompileNode implements Node {

    private final Action action;

    private final ActionContext actionContext;

    private Link link = NullLink.INSTANCE;

    private int hashCode = 0;

    public CompileNode(Action action, ActionContext actionContext) {
        this.action = action;
        this.actionContext = actionContext;
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        boolean needCompile = actionContext.getActionStatus() == ActionContext.ActionStatus.NOT_EXECUTED;
        if (actionContext.getRowMetadata() == null || hashCode != metadata.hashCode()) {
            actionContext.setRowMetadata(metadata.clone());
            hashCode = metadata.hashCode();
            needCompile = true; // Metadata changed, force re-compile
        }
        if (needCompile) {
            action.getRowAction().compile(actionContext);
        }
        link.emit(row, actionContext.getRowMetadata());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitCompile(this);
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

    public Action getAction() {
        return action;
    }

    public ActionContext getActionContext() {
        return actionContext;
    }
}
