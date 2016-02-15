package org.talend.dataprep.transformation.pipeline.model;

/**
 * Signals are data-independent events. They may be used to indicate end of stream or interruption.
 */
public enum Signal {
    /**
     * Signal for end of stream: no more data set row will be submitted to the pipeline.
     */
    END_OF_STREAM
}
