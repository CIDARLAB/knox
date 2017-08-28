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

public class NextTo {
	
	NodeSpace space;
	
	private static final Logger LOG = LoggerFactory.getLogger(NextTo.class);
	
	Rule rule;
	
	public NextTo(NodeSpace space, Rule rule) {
		this.space = space;
		
		this.rule = rule;
	}
	
	public void apply() {
		String subjectID = rule.getOperands().get(0);

		String objectID = rule.getOperands().get(1);

		NodeSpace subjectConstrainedSpace = createConstrainedSpace(space, subjectID);
		
		NodeSpace objectConstrainedSpace = createConstrainedSpace(space, objectID);
		
		NodeSpace doublyConstrainedSpace = createDoublyConstrainedSpace(space, subjectID, objectID);
		
		Set<Node> originalNodes;
		
		if (space.hasNodes()) {
			originalNodes = new HashSet<Node>(space.getNodes());
		} else {
			originalNodes = new HashSet<Node>();
		}
		
		connectConstrainedSpace(originalNodes, subjectConstrainedSpace, doublyConstrainedSpace,
				objectID);
		
		connectConstrainedSpace(originalNodes, objectConstrainedSpace, doublyConstrainedSpace,
				subjectID);

		space.deleteUnreachableNodes();
	}
	
	private void connectConstrainedSpace(Set<Node> nodes, NodeSpace constrainedSpace, 
			NodeSpace doublyConstrainedSpace, String constraintID) {
		HashMap<String, Node> idToNodeCopy = space.unionNodes(constrainedSpace);
		
		for (Node node : nodes) {
			
		}
	}
	
	private void extendNext(Node node, NodeSpace constrainedSpace, NodeSpace doublyConstrainedSpace) {
		
	}
	
	private NodeSpace createConstrainedSpace(NodeSpace space, String constraintID) {
		Set<String> constraintIDs = new HashSet<String>();
		
		constraintIDs.add(constraintID);
		
		NodeSpace constrainedSpace = space.copy(constraintIDs);

		constrainedSpace.clearStartNodeTypes();
		
		return constrainedSpace;
	}
	
	private NodeSpace createDoublyConstrainedSpace(NodeSpace space, String constraintID1,
			String constraintID2) {
		Set<String> constraintIDs = new HashSet<String>();
		
		constraintIDs.add(constraintID1);
		
		constraintIDs.add(constraintID2);
		
		NodeSpace constrainedSpace = space.copy(constraintIDs);

		constrainedSpace.clearStartNodeTypes();
		
		return constrainedSpace;
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
