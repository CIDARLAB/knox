package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

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
		if (rule.isNextTo()) {
			apply(rule.getOperands().get(0), rule.getOperands().get(1));
		}
	}
	
	private void apply(String subjectID, String objectID) {
		Set<Node> originalNodes;
		
		if (space.hasNodes()) {
			originalNodes = new HashSet<Node>(space.getNodes());
		} else {
			originalNodes = new HashSet<Node>();
		}
		
		NodeSpace subjectSpace = createConstrainedSpace(space, subjectID);
		
		NodeSpace objectSpace = createConstrainedSpace(space, objectID);
		
		NodeSpace subjectObjectSpace = createDoublyConstrainedSpace(space, subjectID, objectID);
		
		HashMap<String, Node> idToNodeCopy1 = space.union(subjectSpace);
		
		HashMap<String, Node> idToNodeCopy2 = space.union(objectSpace);
		
		HashMap<String, Node> idToNodeCopy3 = space.union(subjectObjectSpace);
		
		connectConstrainedSpace(originalNodes, objectID, subjectID, idToNodeCopy1, idToNodeCopy3);
		
		connectConstrainedSpace(originalNodes, subjectID, objectID, idToNodeCopy2, idToNodeCopy3);
		
		connectDoublyConstrainedSpace(originalNodes, objectID, subjectID, idToNodeCopy3);
		
		connectDoublyConstrainedSpace(originalNodes, subjectID, objectID, idToNodeCopy3);
		
		for (Node node : originalNodes) {
			for (Edge edge : getEdgesWithComponentID(node, subjectID, idToNodeCopy1)) {
				edge.detachDeleteComponentID(subjectID);
			}
			
			for (Edge edge : getEdgesWithComponentID(node, objectID, idToNodeCopy2)) {
				edge.detachDeleteComponentID(objectID);
			}
		}
		
		space.deleteUnconnectedNodes();
	}
	
	private Set<Edge> getEdgesWithoutComponentID(Node node, String compID, 
			HashMap<String, Node> idToNodeCopy) {
		Set<Edge> edgesWithoutCompID = new HashSet<Edge>();
		
		for (Edge edge : node.getEdgesWithoutComponentID(compID)) {
			if (idToNodeCopy.containsKey(edge.getHead().getNodeID())) {
				edgesWithoutCompID.add(edge);
			}
		}
		
		return edgesWithoutCompID;
	}
	
	private Set<Edge> getEdgesWithComponentID(Node node, String compID, 
			HashMap<String, Node> idToNodeCopy) {
		Set<Edge> edgesWithCompID = new HashSet<Edge>();
		
		for (Edge edge : node.getEdgesWithComponentID(compID)) {
			if (idToNodeCopy.containsKey(edge.getHead().getNodeID())) {
				edgesWithCompID.add(edge);
			}
		}
		
		return edgesWithCompID;
	}
	
	private void connectDoublyConstrainedSpace(Set<Node> nodes, String subjectID, String objectID, 
			HashMap<String, Node> idToNodeCopy) {
		for (Node node : nodes) {
			for (Edge subjectEdge : getEdgesWithComponentID(node, subjectID, idToNodeCopy)) {
				Set<Edge> objectEdges = getEdgesWithComponentID(subjectEdge.getHead(), objectID, 
						idToNodeCopy);
				
				if (!objectEdges.isEmpty()) {
					Node subjectHeadCopy = space.copyNode(subjectEdge.getHead());

					ArrayList<String> subjectCompIDs = new ArrayList<String>();

					subjectCompIDs.add(subjectID);

					Edge subjectEdgeCopy = idToNodeCopy.get(node.getNodeID()).copyEdge(subjectEdge, 
							subjectHeadCopy);
					
					subjectEdgeCopy.setComponentIDs(subjectCompIDs);

					for (Edge objectEdge : objectEdges) {
						Node objectHeadCopy = space.copyNode(objectEdge.getHead());

						ArrayList<String> objectCompIDs = new ArrayList<String>();

						objectCompIDs.add(objectID);

						Edge objectEdgeCopy = subjectHeadCopy.copyEdge(objectEdge, objectHeadCopy);
						
						objectEdgeCopy.setComponentIDs(objectCompIDs);

						extendNextTo(objectHeadCopy, getEdgesWithComponentID(objectEdge.getHead(), 
								subjectID, idToNodeCopy), subjectID, objectID, idToNodeCopy);
						
						for (Edge edge : getEdgesWithoutComponentID(objectEdge.getHead(), subjectID, idToNodeCopy)) {
							objectHeadCopy.copyEdge(edge, idToNodeCopy.get(edge.getHead().getNodeID()));
						}
					}
				}
			}
		}
	}
	
	private void connectConstrainedSpace(Set<Node> nodes, String subjectID, String objectID, 
			HashMap<String, Node> idToNodeCopy1, HashMap<String, Node> idToNodeCopy2) {
		for (Node node : nodes) {
			for (Edge subjectEdge : getEdgesWithComponentID(node, subjectID, idToNodeCopy1)) {
				Node subjectHeadCopy = space.copyNode(subjectEdge.getHead());
				
				ArrayList<String> subjectCompIDs = new ArrayList<String>();
				
				subjectCompIDs.add(subjectID);
				
				Edge subjectEdgeCopy = node.copyEdge(subjectEdge, subjectHeadCopy);
				
				subjectEdgeCopy.setComponentIDs(subjectCompIDs);
				
				for (Edge objectEdge : getEdgesWithComponentID(subjectEdge.getHead(), objectID, 
						idToNodeCopy1)) {
					if (objectEdge.getNumComponentIDs() > 1) {
						Edge objectEdgeCopy = subjectHeadCopy.copyEdge(objectEdge, 
								idToNodeCopy1.get(objectEdge.getHead().getNodeID()));

						objectEdgeCopy.deleteComponentID(objectID);
					}

					Node objectHeadCopy = space.copyNode(objectEdge.getHead());

					ArrayList<String> objectCompIDs = new ArrayList<String>();

					objectCompIDs.add(objectID);

					Edge objectEdgeCopy = subjectHeadCopy.copyEdge(objectEdge, objectHeadCopy);
					
					objectEdgeCopy.setComponentIDs(objectCompIDs);
					
					extendNextTo(objectHeadCopy, getEdgesWithComponentID(objectEdge.getHead(), 
							subjectID, idToNodeCopy1), subjectID, objectID, idToNodeCopy2);
					
					for (Edge edge : getEdgesWithoutComponentID(objectEdge.getHead(), subjectID, idToNodeCopy1)) {
						objectHeadCopy.copyEdge(edge, idToNodeCopy2.get(edge.getHead().getNodeID()));
					}
				}
			}
		}
	}
	
	private void extendNextTo(Node rootNodeCopy, Set<Edge> edges, String subjectID, String objectID,
			HashMap<String, Node> idToNodeCopy) {
		Stack<Node> nodeCopyStack = new Stack<Node>();

		for (int i = 0; i < edges.size(); i++) {
			nodeCopyStack.push(rootNodeCopy);
		}
		
		Stack<Edge> edgeStack = new Stack<Edge>();
		
		for (Edge edge : edges) {
			edgeStack.push(edge);
		}
		
		boolean isSubjectEdge = true;
		
		while (!nodeCopyStack.isEmpty()) {
			Node nodeCopy = nodeCopyStack.pop();
			
			Edge edge = edgeStack.pop();
			
			Node headCopy = space.copyNode(edge.getHead());
			
			ArrayList<String> compIDs = new ArrayList<String>();
			
			if (isSubjectEdge) {
				compIDs.add(subjectID);
			} else {
				compIDs.add(objectID);
			}
			
			if (edge.getNumComponentIDs() > 1) {
				Edge edgeCopy = nodeCopy.copyEdge(edge, idToNodeCopy.get(edge.getHead().getNodeID()));
				
				edgeCopy.deleteComponentIDs(compIDs);
			}
			
			Edge edgeCopy = nodeCopy.copyEdge(edge, headCopy);
			
			edgeCopy.setComponentIDs(compIDs);
			
			Set<Edge> headEdges;
			
			if (isSubjectEdge) {
				headEdges = getEdgesWithComponentID(edge.getHead(), objectID, idToNodeCopy);
				
				for (Edge exitEdge : getEdgesWithoutComponentID(edge.getHead(), objectID, idToNodeCopy)) {
					headCopy.copyEdge(exitEdge, idToNodeCopy.get(exitEdge.getHead().getNodeID()));
				}
				
				isSubjectEdge = false;
			} else {
				headEdges = getEdgesWithComponentID(edge.getHead(), subjectID, idToNodeCopy);
				
				for (Edge exitEdge : getEdgesWithoutComponentID(edge.getHead(), subjectID, idToNodeCopy)) {
					headCopy.copyEdge(exitEdge, idToNodeCopy.get(exitEdge.getHead().getNodeID()));
				}
				
				isSubjectEdge = true;
			}

			for (Edge headEdge : headEdges) {
				edgeStack.push(headEdge);

				nodeCopyStack.push(headCopy);
			}
		}
	}
	
	private NodeSpace createConstrainedSpace(NodeSpace space, String constraintID) {
		Set<String> constraintIDs = new HashSet<String>();
		
		constraintIDs.add(constraintID);
		
		NodeSpace constrainedSpace = space.copy();
		
		if (constrainedSpace.hasNodes()) {
			for (Node node : constrainedSpace.getNodes()) {
				node.deleteComponentID(constraintID);
			}
		}

		constrainedSpace.clearStartNodeTypes();
		
		return constrainedSpace;
	}
	
	private NodeSpace createDoublyConstrainedSpace(NodeSpace space, String constraintID1,
			String constraintID2) {
		Set<String> constraintIDs = new HashSet<String>();
		
		constraintIDs.add(constraintID1);
		
		constraintIDs.add(constraintID2);
		
		NodeSpace constrainedSpace = space.copy();
		
		if (constrainedSpace.hasNodes()) {
			for (Node node : constrainedSpace.getNodes()) {
				node.deleteComponentIDs(constraintIDs);
			}
		}

		constrainedSpace.clearStartNodeTypes();
		
		return constrainedSpace;
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
