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
	
	public void apply(NodeSpace space) {
		if (!concatenationSpace.hasNodes()) {
			concatenationSpace.union(space);
		} else if (space.hasNodes()){
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
				acceptNode.createEdge(startNode);
			}
			
			HashMap<String, Set<Edge>> nodeIDToIncomingEdges = concatenationSpace.mapNodeIDsToIncomingEdges();
			
			if (acceptNodes.size() == 1) {
				Node acceptNode = acceptNodes.iterator().next();

				if (acceptNode.getNumEdges() > 1) {
					acceptNode.deleteEdges(startNode);

					startNode.copyEdges(acceptNode);
				}
				
				for (Edge edge : nodeIDToIncomingEdges.get(acceptNode.getNodeID())) {
					edge.getTail().copyEdge(edge, startNode);
					
					edge.delete();
				}
				
				concatenationSpace.deleteNode(acceptNode);
			} else {
				for (Node acceptNode : acceptNodes) {
					if (acceptNode.getNumEdges() == 1) {
						for (Edge edge : nodeIDToIncomingEdges.get(acceptNode.getNodeID())) {
							edge.getTail().copyEdge(edge, startNode);

							edge.delete();
						}

						concatenationSpace.deleteNode(acceptNode);
					}
				}
			}
		}
	}
	
	public NodeSpace getConcatenationSpace() {
		return concatenationSpace;
	}
	
}
