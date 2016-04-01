package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.link.NullLink;
import org.talend.dataprep.transformation.pipeline.node.FilteredSourceNode;
import org.talend.dataprep.transformation.pipeline.node.SourceNode;

import java.util.function.Function;
import java.util.function.Predicate;

public class NodeBuilder {

    private final Node sourceNode;

    private State state;

    private NodeBuilder(Node sourceNode) {
        this.sourceNode = sourceNode;
        state = new NodeState(sourceNode);
    }

    public static NodeBuilder source() {
        return new NodeBuilder(new SourceNode());
    }

    public static NodeBuilder filteredSource(Predicate<DataSetRow> filter) {
        return new NodeBuilder(new FilteredSourceNode(filter));
    }

    public NodeBuilder sink() {
        state = state.next(n -> NullLink.INSTANCE);
        return this;
    }

    public NodeBuilder to() {
        try {
            state = state.next(n -> new BasicLink(n[0]));
        } catch (IllegalStateException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, new Exception("Each to() must be followed by node().", e));
        }
        return this;
    }

    public NodeBuilder toMany() {
        try {
            state = state.next(CloneLink::new);
        } catch (Exception e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, new Exception("Each toMany() must be followed by nodes().", e));
        }
        return this;
    }

    public Node build() {
        return sourceNode;
    }

    public NodeBuilder node(Node node) {
        state = state.next(node);
        return this;
    }

    public NodeBuilder nodes(Node... nodes) {
        state = state.next(nodes);
        return this;
    }

    private interface State {

        State next(Function<Node[], Link> link);

        State next(Node... node);

        Node getNode();

    }

    private static class LinkState implements State {

        private final Node previousNode;

        private final Function<Node[], Link> linkFunction;

        private LinkState(Node previousNode, Function<Node[], Link> linkFunction) {
            this.previousNode = previousNode;
            this.linkFunction = linkFunction;
        }

        @Override
        public State next(Function<Node[], Link> link) {
            throw new IllegalStateException();
        }

        @Override
        public State next(Node... node) {
            try {
                previousNode.setLink(linkFunction.apply(node));
            } catch (UnsupportedOperationException e) {
                throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, new Exception("Unable to specify a new output after a terminal node.", e));
            }
            return new NodeState(node);
        }

        @Override
        public Node getNode() {
            return previousNode;
        }
    }

    private static class NodeState implements State {

        private final Node[] node;

        private NodeState(Node... node) {
            this.node = node;
        }

        @Override
        public State next(Function<Node[], Link> link) {
            return new LinkState(node[0], link);
        }

        @Override
        public State next(Node... node) {
            throw new IllegalStateException();
        }

        @Override
        public Node getNode() {
            return node[0];
        }
    }
}
