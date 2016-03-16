package org.talend.dataprep.transformation.pipeline;

/**
 * To be implemented to indicate pipeline element can monitor performance.
 */
public interface Monitored {

    /**
     * @return The total time spent in the pipeline element (in milliseconds).
     */
    long getTotalTime();

    /**
     * @return The number of {@link org.talend.dataprep.api.dataset.DataSetRow row} processed by this pipeline element.
     */
    long getCount();
}
