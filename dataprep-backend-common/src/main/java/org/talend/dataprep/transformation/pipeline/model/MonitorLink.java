package org.talend.dataprep.transformation.pipeline.model;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;

public class MonitorLink implements Link {

    private final Link delegate;

    private long count = 0;

    private long totalTime = 1;

    private MonitorLink(Link delegate) {
        this.delegate = delegate;
    }

    public static Link monitorLink(Link link) {
        return new MonitorLink(link);
    }

    @Override
    public void emit(DataSetRow row, RowMetadata metadata) {
        final long start = System.currentTimeMillis();
        try {
            delegate.emit(row, metadata);
        } finally {
            totalTime += System.currentTimeMillis() - start;
            count++;
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMonitorLink(this);
    }

    @Override
    public void signal(Signal signal) {
        delegate.signal(signal);
    }

    public long getCount() {
        return count;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public Link getDelegate() {
        return delegate;
    }
}
