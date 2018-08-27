package knox.spring.data.neo4j.operations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Union {
	
	private NodeSpace unionSpace;
	
	public Union(NodeSpace unionSpace) {
		this.unionSpace = unionSpace;
	}
	
	public Union(List<NodeSpace> spaces) {
		unionSpace = new NodeSpace(0);
		
		for (NodeSpace space : spaces) {
    		unionSpace.union(space);
    	}
	}
	
	public Set<Edge> apply() {
		Set<Edge> blankEdges = new HashSet<Edge>();
		
		if (unionSpace.hasNodes()) {
			Set<Node> startNodes = unionSpace.getStartNodes();

			Node primaryStartNode = unionSpace.createStartNode();

			for (Node startNode : startNodes) {
				startNode.deleteStartNodeType();
			}
			
			for (Node startNode : startNodes) {
				blankEdges.add(primaryStartNode.createEdge(startNode));
			}
			
			Set<Node> acceptNodes = unionSpace.getAcceptNodes();
			
			for (Node acceptNode : acceptNodes) {
				acceptNode.deleteAcceptNodeType();
			}

			Node primaryAcceptNode = unionSpace.createAcceptNode();

			for (Node acceptNode : acceptNodes) {
				blankEdges.add(acceptNode.createEdge(primaryAcceptNode));
			}
		}
		
		return blankEdges;
	}
	
	public NodeSpace getSpace() {
		return unionSpace;
	}
	
}
