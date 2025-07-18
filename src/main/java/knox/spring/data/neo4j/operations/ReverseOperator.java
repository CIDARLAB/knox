package knox.spring.data.neo4j.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class ReverseOperator {
    public static void apply(NodeSpace inputSpace, NodeSpace outputSpace, Boolean reverseOrientation) {

        // Make a copy of the inputSpace
        NodeSpace spaceToReverse = new NodeSpace();
        spaceToReverse.copyNodeSpace(inputSpace);

        // Reverse all Edges and change orientation if not a blank edge
        for (Edge edge : spaceToReverse.getEdges()) {
            Node tail = edge.getTail();
            Node head = edge.getHead();

            Edge reverseEdge = head.copyEdge(edge, tail);
            tail.deleteEdge(edge);

            // Reverse part orientation if not blank
            if (reverseOrientation) {
                if (reverseEdge.getOrientation().equals(Edge.Orientation.REVERSE_COMPLEMENT) && !reverseEdge.isBlank()) {
                    reverseEdge.setOrientation(Edge.Orientation.INLINE);
                } else {
                    reverseEdge.setOrientation(Edge.Orientation.REVERSE_COMPLEMENT);
                }
            }
        }

        // Get Start and Accept Nodes
        Set<Node> startNodes = spaceToReverse.getStartNodes();
        Set<Node> acceptNodes = spaceToReverse.getAcceptNodes();

        // Change Start Nodes to Accept Nodes
        for (Node startNode : startNodes) {
            startNode.deleteStartNodeType();
            startNode.addNodeType(Node.NodeType.ACCEPT.getValue());
        }

        // Change Accept Nodes to Start Nodes
        for (Node acceptNode : acceptNodes) {
            acceptNode.deleteAcceptNodeType();
            acceptNode.addNodeType(Node.NodeType.START.getValue());
        }

        // Copy reversed space to output space
		outputSpace.copyNodeSpace(spaceToReverse);
	}
}
