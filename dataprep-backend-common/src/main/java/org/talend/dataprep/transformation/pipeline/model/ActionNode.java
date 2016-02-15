package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

public class ActionNode implements Node, Monitored {

    private final Action action;

    private final ActionContext actionContext;

    private Link link = NullLink.INSTANCE;

    private long totalTime;

    private int count;

    private int previousMetadataHash;

    public ActionNode(Action action, ActionContext actionContext) {
        this.action = action;
        this.actionContext = actionContext;
    }

    @Override
    public void receive(DataSetRow row, RowMetadata metadata) {
        if (metadata.hashCode() != previousMetadataHash) {
            actionContext.setRowMetadata(metadata);
        }
        final DataSetRow actionRow;
        final long start = System.currentTimeMillis();
        try {
            switch (actionContext.getActionStatus()) {
            case NOT_EXECUTED:
            case OK:
                actionRow = action.getRowAction().apply(row, actionContext);
                break;
            case DONE:
            case CANCELED:
            default:
                actionRow = row;
                break;
            }
        } finally {
            previousMetadataHash = metadata.hashCode();
            totalTime += System.currentTimeMillis() - start;
            count++;
        }
        link.emit(actionRow, actionContext.getRowMetadata());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitAction(this);
        if (link != null) {
            link.accept(visitor);
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
    public long getTotalTime() {
        return totalTime;
    }

    @Override
    public long getCount() {
        return count;
    }

    public Action getAction() {
        return action;
    }

    public ActionContext getActionContext() {
        return actionContext;
    }
}
