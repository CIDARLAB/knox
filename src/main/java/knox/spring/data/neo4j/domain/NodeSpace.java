package knox.spring.data.neo4j.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class NodeSpace {
	
	@GraphId
    Long id;
	
	int idIndex;
	
	@Relationship(type = "CONTAINS") 
    Set<Node> nodes;
	
	public NodeSpace() {
		
	}
	
	public NodeSpace(int idIndex) {
		this.idIndex = idIndex;
	}
	
	public void addNode(Node node) {
		if (nodes == null) {
			nodes = new HashSet<Node>();
		}
		nodes.add(node);
	}
	
	public Node createNode() {
		Node node = new Node("n" + idIndex++);
		addNode(node);
		return node;
	}
	
	public Node createNode(String nodeType) {
		Node node = new Node("n" + idIndex++, nodeType);
		addNode(node);
		return node;
	}

	public Node copyNode(Node node) {
		return createNode(node.getNodeType());
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
	
	public int getIDIndex() {
		return idIndex;
	}
	
	public Set<Edge> getMinimizableEdges(Node node, HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	Set<Edge> minimizableEdges = new HashSet<Edge>();
    	
    	if (nodeIDToIncomingEdges.containsKey(node.getNodeID())) {
    		Set<Edge> incomingEdges = nodeIDToIncomingEdges.get(node.getNodeID());
    		
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
	
//	public Set<Edge> getMinimizableEdges(Node node, HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
//    	Set<Edge> minimizableEdges = new HashSet<Edge>();
//    	if (node.hasEdges()) {
//    		for (Edge edge : node.getEdges()) {
//    			Node successor = edge.getHead();
//    			if (!edge.hasComponents() 
//    					&& !edge.isCyclic()
//    					&& nodeIDToIncomingEdges.get(successor.getNodeID()).size() == 1
//    					&& !(edge.getTail().isStartNode() && successor.isAcceptNode() 
//						|| edge.getTail().isAcceptNode() && successor.isStartNode())) {
//    				Iterator<Edge> incomingEdges = nodeIDToIncomingEdges.get(successor.getNodeID()).iterator();
//    				boolean minimizable = true;
//    				while (minimizable && incomingEdges.hasNext()) {
//    					Edge incomingEdge = incomingEdges.next();
//    					if (!incomingEdge.hasComponents() && incomingEdge.getTail().getEdges().size() > 1) {
//    						minimizable = false;
//    					}
//    				} 
//    				if (minimizable) {
//    					minimizableEdges.add(edge);
//    				}
//    			}
//    		}
//    	}
//    	return minimizableEdges;
//    }
    
    public Set<Node> getNodes() {
    	return nodes;
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
