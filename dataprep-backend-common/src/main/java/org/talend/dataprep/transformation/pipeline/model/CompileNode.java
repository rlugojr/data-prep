package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

public class CompileNode implements Node {

    private final Action action;

    private final ActionContext actionContext;

    private Link link = NullLink.INSTANCE;

    public CompileNode(Action action, ActionContext actionContext) {
        this.action = action;
        this.actionContext = actionContext;
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        if (actionContext.getRowMetadata() == null) {
            actionContext.setRowMetadata(metadata);
        }
        if (actionContext.getActionStatus() == ActionContext.ActionStatus.NOT_EXECUTED) {
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
