package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import knox.spring.data.neo4j.domain.Node.NodeType;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class DesignSpace {
	
	@GraphId
    Long id;
    
    String spaceID;
    
    int idIndex;
    
	@Relationship(type = "CONTAINS") 
    Set<Node> nodes;

    public DesignSpace() {
    	
    }
    
    public DesignSpace(String spaceID) {
    	this.spaceID = spaceID;
    }
    
    public DesignSpace(String spaceID, int idIndex, int mergeIndex) {
    	this.spaceID = spaceID;
    	this.idIndex = idIndex;
    }
    
    public String getSpaceID() {
    	return spaceID;
    }
    
    public void addNode(Node node) {
		if (nodes == null) {
			nodes = new HashSet<Node>();
		}
		nodes.add(node);
	}
	
	public Set<Node> removeAllNodes() {
		Set<Node> removedNodes = nodes;
		nodes.clear();
		return removedNodes;
	}
	
	public Node copyNodeWithEdges(Node node) {
		Node nodeCopy = copyNode(node);
		
		if (node.hasEdges()) {
			for (Edge edge : node.getEdges()) {
				nodeCopy.copyEdge(edge);
			}
		}
		
		return nodeCopy;
	}
	
	public Node copyNodeWithID(Node node) {
		if (node.hasNodeType()) {
			return createTypedNode(node.getNodeID(), node.getNodeType());
		} else {
			return createNode(node.getNodeID());
		} 
	}
	
	public Node copyNode(Node node) {
		if (node.hasNodeType()) {
			return createTypedNode(node.getNodeType());
		} else {
			return createNode();
		}
	}
	
	public Node createNode() {
		Node node = new Node("n" + idIndex++);
		addNode(node);
		return node;
	}
	
	public Node createNode(String nodeID) {
		Node node = new Node(nodeID);
		addNode(node);
		return node;
	}
	
	public Node createTypedNode(String nodeType) {
		Node node = new Node("n" + idIndex++, nodeType);
		addNode(node);
		return node;
	}
	
	public Node createTypedNode(String nodeID, String nodeType) {
		Node node = new Node(nodeID, nodeType);
		addNode(node);
		return node;
	}
	
	public Node createAcceptNode() {
		return createTypedNode(NodeType.ACCEPT.getValue());
	}
	
	public Node createStartNode() {
		return createTypedNode(NodeType.START.getValue());
	}
	
	public boolean deleteNodes(Set<Node> deletedNodes) {
		if (hasNodes()) {
    		return nodes.removeAll(deletedNodes);
    	} else {
    		return false;
    	}
	}
	
	public boolean detachDeleteNodes(Set<Node> deletedNodes) {	
		if (hasNodes()) {
			for (Node node : nodes) {
				if (node.hasEdges() && !deletedNodes.contains(node)) {
					Set<Edge> deletedEdges = new HashSet<Edge>();
					
					for (Edge edge : node.getEdges()) {
						if (deletedNodes.contains(edge.getHead())) {
							deletedEdges.add(edge);
						}
					}
					
					node.deleteEdges(deletedEdges);
				}
			}
    		return deleteNodes(deletedNodes);
    	} else {
    		return false;
    	}
	}
	
	public Set<Node> getAcceptNodes() {
    	Set<Node> acceptNodes = new HashSet<Node>();
    	if (hasNodes()) {
    		for (Node node : nodes) {
        		if (node.isAcceptNode()) {
        			acceptNodes.add(node);
        		}
        	}
    	}
    	return acceptNodes;
    }
	
	 public Set<Edge> getMinimizableEdges(Node node, HashMap<String, Set<Edge>> nodeIDsToIncomingEdges) {
		 Set<Edge> minimizableEdges = new HashSet<Edge>();

		 if (nodeIDsToIncomingEdges.containsKey(node.getNodeID())) {
			 Set<Edge> incomingEdges = nodeIDsToIncomingEdges.get(node.getNodeID());

			 if (incomingEdges.size() == 1) {
				 Edge incomingEdge = incomingEdges.iterator().next();

				 Node predecessor = incomingEdge.getTail();

				 if (!incomingEdge.hasComponents() && !incomingEdge.isCyclic()
						 && (predecessor.getNumEdges() == 1 || !node.hasConflictingNodeType(predecessor))) {
					 minimizableEdges.add(incomingEdge);
				 }
			 } else if (incomingEdges.size() > 1) {
				 for (Edge incomingEdge : incomingEdges) {
					 Node predecessor = incomingEdge.getTail();

					 if (!incomingEdge.hasComponents() && !incomingEdge.isCyclic() 
							 && predecessor.getNumEdges() == 1 && !predecessor.hasConflictingNodeType(node)) {
						 minimizableEdges.add(incomingEdge);
					 }
				 }
			 }
		 }

		 return minimizableEdges;
	 }
	
	public int getIdIndex() {
		return idIndex;
	}
	
	public int getNumNodes() {
		if (hasNodes()) {
			return nodes.size();
		} else {
			return 0;
		}
	}
	
	public Node getNode(String nodeID) {
		if (hasNodes()) {
			for (Node node : nodes) {
				if (node.getNodeID().equals(nodeID)) {
					return node;
				}
			}
			return null;
		} else {
			return null;
		}
	}
    
    public Set<Node> getNodes() {
    	return nodes;
    }
    
    public int getSize() {
    	if (hasNodes()) {
    		return nodes.size();
    	} else {
    		return 0;
    	}
    }
    
    public Node getStartNode() {
    	if (hasNodes()) {
    		for (Node node : nodes) {
        		if (node.isStartNode()) {
        			return node;
        		}
        	}
        	return null;
    	} else {
    		return null;
    	}
    }
    
    public Set<Node> getStartNodes() {
    	Set<Node> startNodes = new HashSet<Node>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
        		if (node.isStartNode()) {
        			startNodes.add(node);
        		}
        	}
    	} 
    	
    	return startNodes;
    }
    
    public Set<Node> getTypedNodes() {
    	Set<Node> typedNodes = new HashSet<Node>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
        		if (node.hasNodeType()) {
        			typedNodes.add(node);
        		}
        	}
    	} 
    	
    	return typedNodes;
    }
    
    public boolean hasNodes() {
    	if (nodes == null) {
    		return false;
    	} else {
    		return nodes.size() > 0;
    	}
    }
    
    public HashMap<String, Set<Edge>> mapNodeIDsToIncomingEdges() {
    	HashMap<String, Set<Edge>> nodeIDToIncomingEdges = new HashMap<String, Set<Edge>>();
		if (hasNodes()) {
			for (Node node : nodes) {
	    		if (node.hasEdges()) {
	    			for (Edge edge : node.getEdges()) {
	    				Node successor = edge.getHead();
	    				if (!nodeIDToIncomingEdges.containsKey(successor.getNodeID())) {
	    					nodeIDToIncomingEdges.put(successor.getNodeID(), new HashSet<Edge>());
	    				}
	    				nodeIDToIncomingEdges.get(successor.getNodeID()).add(edge);
	    			}
	    		}
	    	}
		}
		return nodeIDToIncomingEdges;
	}
    
    public HashMap<String, Node> mapNodeIDsToNodes() {
    	HashMap<String, Node> nodeIDToNode = new HashMap<String, Node>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			nodeIDToNode.put(node.getNodeID(), node);
    		}
    	}
 
    	return nodeIDToNode;
    }
    
    public Set<Node> getOtherNodes(Set<Node> nodes) {
    	Set<Node> diffNodes = new HashSet<Node>();

    	if (hasNodes()) {
    		for (Node node : this.nodes) {
    			if (!nodes.contains(node)) {
    				diffNodes.add(node);
    			}
    		}
    	}

    	return diffNodes;
    }
    
    public Set<Node> retainNodes(Set<Node> retainedNodes) {
    	Set<Node> diffNodes = getOtherNodes(retainedNodes);
    	
    	if (diffNodes.size() > 0) {
    		deleteNodes(diffNodes);
    	}
    	
    	return diffNodes;
    }
    
    public Set<Edge> getOtherEdges(Set<Edge> edges) {
    	Set<Edge> diffEdges = new HashSet<Edge>();

    	if (hasNodes()) {
    		for (Node node : nodes) {
    			diffEdges.addAll(getOtherEdges(node, edges));
    		}
    	}

    	return diffEdges;
    }
    
    private Set<Edge> getOtherEdges(Node node, Set<Edge> edges) {
    	Set<Edge> diffEdges = new HashSet<Edge>();

    	if (node.hasEdges()) {
    		for (Edge edge : node.getEdges()) {
    			if (!edges.contains(edge)) {
    				diffEdges.add(edge);
    			}
    		}
    	}
    	
    	return diffEdges;
    }
    
    public Set<Edge> retainEdges(Set<Edge> retainedEdges) {
    	Set<Edge> diffEdges = new HashSet<Edge>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			Set<Edge> localDiffEdges = getOtherEdges(node, retainedEdges);
    			
    			if (localDiffEdges.size() > 0) {
    				node.deleteEdges(localDiffEdges);

    				diffEdges.addAll(localDiffEdges);
    			}
    		}
    	}
    	
    	return diffEdges;
    }
    
    public void setIDIndex(int idIndex) {
    	this.idIndex = idIndex;
    }
    
    public Set<Node> removeNodesByID(Set<String> nodeIDs) {
    	Set<Node> removedNodes = new HashSet<Node>();
    	for (Node node : nodes) {
    		if (nodeIDs.contains(node.getNodeID())) {
    			removedNodes.add(node);
    		}
    	}
    	nodes.removeAll(removedNodes);
    	return removedNodes;
    }
}
