package knox.spring.data.neo4j.operations;

import java.util.HashMap;
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
	
	public void apply() {
		if (unionSpace.hasNodes()) {
			Set<Node> startNodes = unionSpace.getStartNodes();

			Node primaryStartNode = unionSpace.createStartNode();

			for (Node startNode : startNodes) {
				startNode.deleteStartNodeType();
			}

			for (Node startNode : startNodes) {
				primaryStartNode.createEdge(startNode);
			}

			HashMap<String, Set<Edge>> nodeIDToIncomingEdges = unionSpace.mapNodeIDsToIncomingEdges();
			
			if (startNodes.size() == 1) {
				Node startNode = startNodes.iterator().next();
				
				for (Edge edge : nodeIDToIncomingEdges.get(startNode.getNodeID())) {
					edge.getTail().copyEdge(edge, primaryStartNode);
					
					edge.delete();
				}
				
				primaryStartNode.deleteEdges(startNode);
				
				primaryStartNode.copyEdges(startNode);
				
				unionSpace.deleteNode(startNode);
			} else {
				for (Node startNode : startNodes) {
					if (nodeIDToIncomingEdges.containsKey(startNode.getNodeID())
							&& nodeIDToIncomingEdges.get(startNode.getNodeID()).size() == 1) {
						primaryStartNode.deleteEdges(startNode);

						primaryStartNode.copyEdges(startNode);

						unionSpace.deleteNode(startNode);
					}
				}
			}
		}
	}
	
	public NodeSpace getUnionSpace() {
		return unionSpace;
	}
	
}
