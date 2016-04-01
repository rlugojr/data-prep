package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public abstract class Visitor {

    private void doNodeVisit(Node node) {
        if (node != null && node.getLink() != null) {
            node.getLink().accept(this);
        }
    }

    public void visitAction(ActionNode actionNode) {
        doNodeVisit(actionNode);
    }

    public void visitCompile(CompileNode compileNode) {
        doNodeVisit(compileNode);
    }

    public void visitInlineAnalysis(InlineAnalysisNode inlineAnalysisNode) {
        doNodeVisit(inlineAnalysisNode);
    }

    public void visitSource(SourceNode sourceNode) {
        doNodeVisit(sourceNode);
    }

    public void visitBasicLink(BasicLink basicLink) {
        basicLink.getTarget().accept(this);
    }

    public void visitDelayedAnalysis(DelayedAnalysisNode delayedAnalysisNode) {
        doNodeVisit(delayedAnalysisNode);
    }

    public void visitPipeline(Pipeline pipeline) {
        pipeline.getNode().accept(this);
    }

    public void visitNode(Node node)  {
        doNodeVisit(node);
    }

    public void visitCloneLink(CloneLink cloneLink) {
        final Node[] nodes = cloneLink.getNodes();
        for (Node node : nodes) {
            node.accept(this);
        }
    }


}
