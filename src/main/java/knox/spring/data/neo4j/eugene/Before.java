package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Before {
	
	NodeSpace space;
	
	private static final Logger LOG = LoggerFactory.getLogger(Before.class);
	
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
			
			NodeSpace tempSpace = space.copy();
			
			for (Node node : tempSpace.getNodes()) {
				if (node.hasEdges()) {
					Set<Edge> deletedEdges = new HashSet<Edge>();
					
					for (Edge edge : node.getEdges()) {
						if (edge.hasComponentID(subjectID)) {
							edge.deleteComponentID(subjectID);
							
							if (!edge.hasComponentIDs()) {
								deletedEdges.add(edge);
							}
						}
					}
					
					node.deleteEdges(deletedEdges);
				}
				
				if (node.isStartNode()) {
					node.clearStartNodeType();
				}
			}
			
			Set<Node> originalNodes = new HashSet<Node>(space.getNodes());

			HashMap<String, Node> idToNodeCopy = space.unionNodes(tempSpace);
			
			for (Node node : originalNodes) {
				if (node.hasEdges()) {
					Set<Edge> deletedEdges = new HashSet<Edge>();
					
					for (Edge edge : node.getEdges()) {
						if (edge.hasComponentID(objectID)) {
							ArrayList<String> compIDs = new ArrayList<String>();
							
							compIDs.add(objectID);
							
							node.createEdge(idToNodeCopy.get(edge.getHead().getNodeID()),
									compIDs, edge.getComponentRoles());
							
							edge.deleteComponentID(objectID);
							
							if (!edge.hasComponentIDs()) {
								deletedEdges.add(edge);
							}
						}
					}
					
					node.deleteEdges(deletedEdges);
				}
			}
		}
		
		space.deleteUnreachableNodes();
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
