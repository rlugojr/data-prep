package org.talend.dataprep.transformation.pipeline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.talend.dataprep.transformation.api.action.metadata.common.ActionMetadata;

@Component
public class SpringActionRegistry implements ActionRegistry { // NOSONAR

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public ActionMetadata get(String name) {
        return applicationContext.getBean(ActionMetadata.ACTION_BEAN_PREFIX + name, ActionMetadata.class);
    }
}
