package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public class PipelineConsoleDump extends Visitor {

    private final StringBuilder builder;

    int indent;

    public PipelineConsoleDump(StringBuilder builder) {
        this.builder = builder;
        indent = 0;
    }

    private void buildMonitorInformation(Monitored monitored) {
        final long totalTime = monitored.getTotalTime();
        final long count = monitored.getCount();
        double speed = totalTime > 0 ? Math.round(((double) count * 1000) / totalTime) : Double.POSITIVE_INFINITY;

        builder.append("(").append(monitored.getTotalTime()).append(" ms - ").append(monitored.getCount())
                .append(" rows - ").append(speed).append(" rows/s) ");
    }

    @Override
    public void visitAction(ActionNode actionNode) {
        buildMonitorInformation(actionNode);
        builder.append("ACTION").append(" [").append(actionNode.getAction().getName()).append("] ").append("(status: ")
                .append(actionNode.getActionContext().getActionStatus()).append(")").append('\n');
    }

    @Override
    public void visitCompile(CompileNode compileNode) {
        builder.append("COMPILE").append(" [").append(compileNode.getAction().getName()).append("] ").append("(status: ")
                .append(compileNode.getActionContext().getActionStatus()).append(")").append('\n');
    }

    @Override
    public void visitInlineAnalysis(InlineAnalysisNode inlineAnalysisNode) {
        buildMonitorInformation(inlineAnalysisNode);
        builder.append("INLINE ANALYSIS").append('\n');
    }

    @Override
    public void visitSource(SourceNode sourceNode) {
        builder.append("-> SOURCE").append('\n');
    }

    @Override
    public void visitBasicLink(BasicLink basicLink) {
        builder.append("-> ");
        basicLink.getTarget().accept(this);
    }

    @Override
    public void visitDelayedAnalysis(DelayedAnalysisNode delayedAnalysisNode) {
        buildMonitorInformation(delayedAnalysisNode);
        builder.append("DELAYED ANALYSIS").append('\n');
    }

    @Override
    public void visitPipeline(Pipeline pipeline) {
        builder.append("PIPELINE {").append('\n');
        pipeline.getNode().accept(this);
        builder.append('\n').append('}').append('\n');
    }

    @Override
    public void visitNode(Node node) {
        builder.append("UNKNOWN NODE").append('\n');
        node.getLink().accept(this);
    }

    @Override
    public void visitCloneLink(CloneLink cloneLink) {
        builder.append("->").append('\n');
        final Node[] nodes = cloneLink.getNodes();
        for (int i = 0; i < nodes.length; i++) {
            builder.append("\t-(").append(i + 1).append("/").append(nodes.length).append(")-> ");
            nodes[i].accept(this);
        }
    }

}
