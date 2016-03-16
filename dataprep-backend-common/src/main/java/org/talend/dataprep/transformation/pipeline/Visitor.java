package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.link.MonitorLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public interface Visitor {

    void visitAction(ActionNode actionNode);

    void visitCompile(CompileNode compileNode);

    void visitInlineAnalysis(InlineAnalysisNode inlineAnalysisNode);

    void visitSource(SourceNode sourceNode);

    void visitBasicLink(BasicLink basicLink);

    void visitMonitorLink(MonitorLink monitorLink);

    void visitDelayedAnalysis(DelayedAnalysisNode delayedAnalysisNode);

    void visitPipeline(Pipeline pipeline);

    void visitNode(Node node);

    void visitCloneLink(CloneLink cloneLink);

}
