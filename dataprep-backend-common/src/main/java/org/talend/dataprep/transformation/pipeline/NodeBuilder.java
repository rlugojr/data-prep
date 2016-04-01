package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.model.FilteredSourceNode;
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

    public NodeBuilder to() {
        state = state.next(n -> new BasicLink(n[0]));
        return this;
    }

    public NodeBuilder toMany() {
        state = state.next(CloneLink::new);
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
            previousNode.setLink(linkFunction.apply(node));
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
