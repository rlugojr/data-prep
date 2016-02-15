package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.transformation.api.action.metadata.common.ActionMetadata;

@FunctionalInterface
public interface ActionRegistry {
    ActionMetadata get(String name);
}
