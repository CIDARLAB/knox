package knox.spring.data.neo4j.operations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Concatenation {

	private NodeSpace concatenationSpace;
	
	public Concatenation() {
		concatenationSpace = new NodeSpace(0);
	}
	
	public Set<Edge> apply(NodeSpace space) {
		Set<Edge> blankEdges = new HashSet<Edge>();
		
		if (space.hasNodes()) {
			if (!concatenationSpace.hasNodes()) {
				concatenationSpace.union(space);
			} else {
				Set<Node> acceptNodes = concatenationSpace.getAcceptNodes();

				Set<Node> originalStartNodes = concatenationSpace.getStartNodes();

				concatenationSpace.union(space);

				Set<Node> startNodes = new HashSet<Node>();

				for (Node startNode : concatenationSpace.getStartNodes()) {
					if (!originalStartNodes.contains(startNode)) {
						startNodes.add(startNode);
					}
				}

				Node startNode = startNodes.iterator().next();

				for (Node acceptNode : acceptNodes) {
					acceptNode.deleteAcceptNodeType();
				}

				startNode.deleteStartNodeType();

				for (Node acceptNode : acceptNodes) {
					blankEdges.add(acceptNode.createEdge(startNode));
				}
			}
		}
		
		return blankEdges;
	}
	
	public NodeSpace getSpace() {
		return concatenationSpace;
	}
	
}
