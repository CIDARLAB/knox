package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Before {
	
	NodeSpace space;
	
	Rule rule;
	
	public Before(NodeSpace space, Rule rule) {
		this.space = space;
		
		this.rule = rule;
	}
	
	public void apply() {
		if (space.hasNodes()) {
			String subjectID;
			
			String objectID;
			
			if (rule.isAfter()) {
				subjectID = rule.getOperands().get(1);
				
				objectID = rule.getOperands().get(0);
			} else {
				subjectID = rule.getOperands().get(0);
				
				objectID = rule.getOperands().get(1);
			}
			
			Set<Node> nodes = space.getNodes();

			NodeSpace tempSpace = space.copy();
			
			for (Node node : tempSpace.getNodes()) {
				if (node.hasEdges()) {
					for (Edge edge : node.getEdges()) {
						if (edge.hasComponentID(subjectID)) {
							edge.deleteComponentID(subjectID);
						}
					}
				}
				
				if (node.isStartNode()) {
					node.clearStartNodeType();
				}
			}

			HashMap<String, Node> idToNodeCopy = space.unionNodes(tempSpace);
			
			for (Node node : nodes) {
				if (node.hasEdges()) {
					for (Edge edge : node.getEdges()) {
						if (edge.hasComponentID(objectID)) {
							ArrayList<String> compIDs = new ArrayList<String>();
							
							compIDs.add(objectID);
							
							node.createEdge(idToNodeCopy.get(edge.getHead().getNodeID()),
									compIDs, edge.getComponentRoles());
						}
					}
				}
			}
		}
		
		space.deleteUnreachableNodes();
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
