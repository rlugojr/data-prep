package org.talend.dataprep.transformation.pipeline;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.transformation.api.action.DataSetRowAction;
import org.talend.dataprep.transformation.api.action.context.ActionContext;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.link.NullLink;
import org.talend.dataprep.transformation.pipeline.node.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PipelineTest {

    private TestOutput output;

    @Before
    public void setUp() throws Exception {
        output = new TestOutput();
    }

    @Test
    public void testCompileAction() throws Exception {
        // Given
        final Action mockAction = new Action() {
            @Override
            public DataSetRowAction getRowAction() {
                return new DataSetRowAction() {
                    @Override
                    public void compile(ActionContext actionContext) {
                        actionContext.get("ExecutedCompile", p -> true);
                    }

                    @Override
                    public DataSetRow apply(DataSetRow dataSetRow, ActionContext context) {
                        return dataSetRow;
                    }
                };
            }
        };
        final ActionContext actionContext = new ActionContext(new TransformationContext());
        final Node node = NodeBuilder.source().to().node(new CompileNode(mockAction, actionContext)).to().node(output).build();
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row = new DataSetRow(rowMetadata);

        // When
        assertFalse(actionContext.has("ExecutedCompile"));
        node.receive(row, rowMetadata);

        // Then
        assertTrue(actionContext.has("ExecutedCompile"));
        assertTrue(actionContext.get("ExecutedCompile"));
    }


    @Test
    public void testAction() throws Exception {
        // Given
        final Action mockAction = new Action() {
            @Override
            public DataSetRowAction getRowAction() {
                return (r, context) -> {
                    context.get("ExecutedApply", p -> true);
                    return r;
                };
            }
        };
        final ActionContext actionContext = new ActionContext(new TransformationContext());
        final Node node = NodeBuilder.source().to().node(new ActionNode(mockAction, actionContext)).to().node(output).build();
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row = new DataSetRow(rowMetadata);

        // When
        assertFalse(actionContext.has("ExecutedApply"));
        node.receive(row, rowMetadata);

        // Then
        assertTrue(actionContext.has("ExecutedApply"));
        assertTrue(actionContext.get("ExecutedApply"));
    }

    @Test
    public void testCanceledAction() throws Exception {
        // Given
        final Action mockAction = new Action() {
            @Override
            public DataSetRowAction getRowAction() {
                return (r, context) -> {
                    context.get("Executed", p -> true);
                    return r;
                };
            }
        };
        final ActionContext actionContext = new ActionContext(new TransformationContext());
        actionContext.setActionStatus(ActionContext.ActionStatus.CANCELED);
        final Node node = NodeBuilder.source().to().node(new ActionNode(mockAction, actionContext)).to().node(output).build();
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row = new DataSetRow(rowMetadata);

        // When
        node.receive(row, rowMetadata);

        // Then
        assertFalse(actionContext.has("Executed"));
    }

    @Test
    public void testCloneLink() throws Exception {
        // Given
        final TestOutput output2 = new TestOutput();
        final Node node = NodeBuilder.source().toMany().nodes(output, output2).build();
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row1 = new DataSetRow(rowMetadata);
        final DataSetRow row2 = row1.clone();
        row1.setTdpId(1L);
        row2.setTdpId(2L);

        // When
        node.receive(row1, rowMetadata);
        node.receive(row2, rowMetadata);
        node.signal(Signal.END_OF_STREAM);

        // Then
        assertEquals(2, output.getCount());
        assertEquals(2, output2.getCount());
        assertEquals(row2, output.getRow());
        assertEquals(row2, output2.getRow());
        assertEquals(rowMetadata, output.getMetadata());
        assertEquals(rowMetadata, output2.getMetadata());
        assertEquals(Signal.END_OF_STREAM, output.getSignal());
        assertEquals(Signal.END_OF_STREAM, output2.getSignal());
    }

    @Test
    public void testSourceNode() throws Exception {
        // Given
        final Node node = NodeBuilder.source().to().node(output).build();
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row1 = new DataSetRow(rowMetadata);
        final DataSetRow row2 = row1.clone();
        row1.setTdpId(1L);
        row2.setTdpId(2L);

        // When
        node.receive(row1, rowMetadata);
        node.receive(row2, rowMetadata);

        // Then
        assertEquals(2, output.getCount());
        assertEquals(row2, output.getRow());
        assertEquals(rowMetadata, output.getMetadata());
    }

    @Test
    public void testFilteredSourceNode() throws Exception {
        // Given
        final Node node = NodeBuilder.filteredSource(r -> r.getTdpId() == 2).to().node(output).build();
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row1 = new DataSetRow(rowMetadata);
        final DataSetRow row2 = row1.clone();
        row1.setTdpId(1L);
        row2.setTdpId(2L);

        // When
        node.receive(row1, rowMetadata);
        node.receive(row2, rowMetadata);

        // Then
        assertEquals(1, output.getCount());
        assertEquals(row1, output.getRow());
        assertEquals(rowMetadata, output.getMetadata());
    }

    @Test
    public void testPipeline() throws Exception {
        // Given
        final Pipeline pipeline = new Pipeline(NodeBuilder.source().to().node(output).build());
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row = new DataSetRow(rowMetadata);

        // When
        pipeline.receive(row, rowMetadata);
        pipeline.signal(Signal.END_OF_STREAM);

        // Then
        assertEquals(1, output.getCount());
        assertEquals(row, output.getRow());
        assertEquals(rowMetadata, output.getMetadata());
        assertEquals(Signal.END_OF_STREAM, output.getSignal());
    }

    @Test
    public void testSignals() throws Exception {
        // Given
        final TestOutput output = new TestOutput();
        final Node node = NodeBuilder.source().to().node(output).build();

        for (final Signal signal : Signal.values()) {
            // When
            node.signal(signal);

            // Then
            assertEquals(signal, output.getSignal());
        }
    }

    @Test
    public void testSink() throws Exception {
        // Given
        final Node node = NodeBuilder.source().sink() //
                .node(new BasicNode()) //
                .to() //
                .node(output) //
                .sink()
                .build();
        final RowMetadata rowMetadata = new RowMetadata();
        final DataSetRow row = new DataSetRow(rowMetadata);

        // When
        node.receive(row, rowMetadata);

        // Then
        assertEquals(0, output.getCount());
        assertEquals(null, output.getRow());
        assertEquals(null, output.getMetadata());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void terminalNodeLink() throws Exception {
        Node terminalNode = new TerminalNode();
        terminalNode.setLink(null);
    }

    @Test
    public void terminalNode() throws Exception {
        Node terminalNode = new TerminalNode();
        assertEquals(terminalNode.getLink(), NullLink.INSTANCE);
    }

    @Test(expected = TDPException.class)
    public void testOutputAfterTerminalNode() throws Exception {
        // When
        // It is illegal to continue pipeline after a TerminalNode
        NodeBuilder.source().to().node(NullNode.INSTANCE).to().node(output).build();
    }

    @Test(expected = TDPException.class)
    public void testMultipleLinks() throws Exception {
        // When
        // It is illegal to leave a to() with no following node() call
        NodeBuilder.source().to().to().node(output).build(); // Call twice to()
    }

    @Test
    public void testVisitorAndToString() throws Exception {
        final Node node = NodeBuilder.source() //
                .to() //
                .node(new BasicNode()) //
                .toMany()
                .nodes(new BasicNode())
                .to()
                .node(new ActionNode(new Action(), new ActionContext(new TransformationContext())))
                .to() //
                .node(output) //
                .build();
        final Pipeline pipeline = new Pipeline(node);
        final TestVisitor visitor = new TestVisitor();

        // When
        pipeline.accept(visitor);

        // Then
        assertThat(visitor.traversedClasses, CoreMatchers.hasItem(Pipeline.class));
        assertThat(visitor.traversedClasses, CoreMatchers.hasItem(SourceNode.class));
        assertThat(visitor.traversedClasses, CoreMatchers.hasItem(BasicLink.class));
        assertThat(visitor.traversedClasses, CoreMatchers.hasItem(BasicNode.class));
        assertThat(visitor.traversedClasses, CoreMatchers.hasItem(CloneLink.class));
        assertThat(visitor.traversedClasses, CoreMatchers.hasItem(ActionNode.class));
        assertNotNull(pipeline.toString());
    }

    private static class TestOutput extends TerminalNode {

        private DataSetRow row;

        private RowMetadata metadata;

        private int count;

        private Signal signal;

        @Override
        public void receive(DataSetRow row, RowMetadata metadata) {
            count++;
            this.row = row;
            this.metadata = metadata;
        }

        @Override
        public void signal(Signal signal) {
            this.signal = signal;
            super.signal(signal);
        }

        public Signal getSignal() {
            return signal;
        }

        public DataSetRow getRow() {
            return row;
        }

        public RowMetadata getMetadata() {
            return metadata;
        }

        public int getCount() {
            return count;
        }
    }

    private static class TestVisitor extends Visitor {

        List<Class> traversedClasses = new ArrayList<>();

        @Override
        public void visitAction(ActionNode actionNode) {
            traversedClasses.add(actionNode.getClass());
            super.visitAction(actionNode);
        }

        @Override
        public void visitCompile(CompileNode compileNode) {
            traversedClasses.add(compileNode.getClass());
            super.visitCompile(compileNode);
        }

        @Override
        public void visitInlineAnalysis(InlineAnalysisNode inlineAnalysisNode) {
            traversedClasses.add(inlineAnalysisNode.getClass());
            super.visitInlineAnalysis(inlineAnalysisNode);
        }

        @Override
        public void visitSource(SourceNode sourceNode) {
            traversedClasses.add(sourceNode.getClass());
            super.visitSource(sourceNode);
        }

        @Override
        public void visitBasicLink(BasicLink basicLink) {
            traversedClasses.add(basicLink.getClass());
            super.visitBasicLink(basicLink);
        }

        @Override
        public void visitDelayedAnalysis(DelayedAnalysisNode delayedAnalysisNode) {
            traversedClasses.add(delayedAnalysisNode.getClass());
            super.visitDelayedAnalysis(delayedAnalysisNode);
        }

        @Override
        public void visitPipeline(Pipeline pipeline) {
            traversedClasses.add(pipeline.getClass());
            super.visitPipeline(pipeline);
        }

        @Override
        public void visitNode(Node node) {
            traversedClasses.add(node.getClass());
            super.visitNode(node);
        }

        @Override
        public void visitCloneLink(CloneLink cloneLink) {
            traversedClasses.add(cloneLink.getClass());
            super.visitCloneLink(cloneLink);
        }
   }
}