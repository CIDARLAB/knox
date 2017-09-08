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
		if (rule.isBefore()) {
			apply(rule.getOperands().get(0), rule.getOperands().get(1));
		} else if (rule.isAfter()) {
			apply(rule.getOperands().get(1), rule.getOperands().get(0));
		}
	}
	
	private void apply(String subjectID, String objectID) {
		NodeSpace constrainedSpace = space.copy();

		if (constrainedSpace.hasNodes()) {
			for (Node node : constrainedSpace.getNodes()) {
				node.deleteComponentID(subjectID);
				
				node.deleteStartNodeType();
			}
		}

		if (space.hasNodes()) {
			Set<Node> originalNodes = new HashSet<Node>(space.getNodes());

			HashMap<String, Node> idToNodeCopy = space.union(constrainedSpace);

			for (Node node : originalNodes) {
				Set<Edge> originalEdges = node.getEdgesWithComponentID(objectID);
				
				node.deleteComponentID(objectID);
				
				for (Edge edge : originalEdges) {
					ArrayList<String> compIDs = new ArrayList<String>();

					compIDs.add(objectID);

					Edge edgeCopy = node.copyEdge(edge, 
							idToNodeCopy.get(edge.getHead().getNodeID()));
					
					edgeCopy.setComponentIDs(compIDs);
				}
			}
		}
		
		space.deleteUnconnectedNodes();
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
