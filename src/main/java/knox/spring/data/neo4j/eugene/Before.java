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
		String subjectID;

		String objectID;

		if (rule.isAfter()) {
			subjectID = rule.getOperands().get(1);

			objectID = rule.getOperands().get(0);
		} else {
			subjectID = rule.getOperands().get(0);

			objectID = rule.getOperands().get(1);
		}

		NodeSpace constrainedSpace = space.copy();

		if (constrainedSpace.hasNodes()) {
			for (Node node : constrainedSpace.getNodes()) {
				node.deleteComponentID(subjectID);
				
				node.deleteStartNodeType();
			}
		}

		Set<Node> originalNodes;
		
		if (space.hasNodes()) {
			originalNodes = new HashSet<Node>(space.getNodes());
		} else {
			originalNodes = new HashSet<Node>();
		}
		
		HashMap<String, Node> idToNodeCopy = space.unionNodes(constrainedSpace);

		for (Node node : originalNodes) {
			node.deleteComponentID(objectID);
			
			for (Edge edge : node.getEdgesWithComponentID(objectID)) {
				ArrayList<String> compIDs = new ArrayList<String>();

				compIDs.add(objectID);

				node.copyEdge(edge, idToNodeCopy.get(edge.getHead().getNodeID()),
						compIDs);
			}
		}
		
		space.deleteUnreachableNodes();
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
