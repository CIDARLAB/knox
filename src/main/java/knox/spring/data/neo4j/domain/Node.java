package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


//import org.neo4j.ogm.annotation.*;
//
//import com.fasterxml.jackson.annotation.JsonIdentityInfo;
//import com.voodoodyne.jackson.jsog.JSOGGenerator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

//@JsonIdentityInfo(generator=JSOGGenerator.class)
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
@NodeEntity
public class Node {
	
    @GraphId 
    Long id;
    
    String nodeID;

    @Relationship(type = "PRECEDES") 
    Set<Edge> edges;
    
    String nodeType;

    public Node() {
    	
    }
    
    public Node(String nodeID) {
    	this.nodeID = nodeID;
    }
    
    public Node(String nodeID, String nodeType) {
    	this.nodeID = nodeID;
    	this.nodeType = nodeType;
    }
    
    public void addEdge(Edge edge) {
    	if (edges == null) {
    		edges = new HashSet<Edge>();
    	}
    	edges.add(edge);
    }
    
    public void clearNodeType() {
    	nodeType = null;
    }
    
    public Edge copyEdge(Edge edge) {
    	return copyEdge(edge, edge.getHead());
    }
    
    public Edge copyEdge(Edge edge, Node head) {
    	if (hasEdge(head)) {
			Edge existingEdge = getEdge(head);
			
			existingEdge.unionWithEdge(edge);
			
			return existingEdge;
		} else if (edge.hasComponentIDs() && edge.hasComponentRoles()) {
    		return createEdge(head, new ArrayList<String>(edge.getComponentIDs()), new ArrayList<String>(edge.getComponentRoles()));
    	} else {
    		return createEdge(head);
    	}
    }
    
    public Edge createEdge(Node head) {
    	Edge edge = new Edge(this, head);
    	addEdge(edge);
    	return edge;
    }
    
    public Edge createEdge(Node head, ArrayList<String> compIDs, ArrayList<String> compRoles) {
    	Edge edge = new Edge(this, head, compIDs, compRoles);
    	addEdge(edge);
    	return edge;
    }
    
    
    
    public Long getGraphID() {
    	return id;
    }
    
    public String getNodeID() {
    	return nodeID;
    }
    
    public int getNumEdges() {
    	return edges.size();
    }

    public Set<Edge> getEdges() {
        return edges;
    }
    
//    public Set<Edge> getIncomingEdges() {
//    	Set<Edge> incomingEdges = new HashSet<Edge>();
//    	
//    	for (Edge edge : edges) {
//    		if (edge.getHead().getNodeID().equals(nodeID)) {
//    			incomingEdges.add(edge);
//    		}
//    	}
//    	
//    	return incomingEdges;
//    }
    
    public String getNodeType() {
    	return nodeType;
    }
    
//    public Set<Edge> getOutgoingEdges() {
//    	Set<Edge> outgoingEdges = new HashSet<Edge>();
//    	
//    	for (Edge edge : edges) {
//    		if (edge.getTail().getNodeID().equals(nodeID)) {
//    			outgoingEdges.add(edge);
//    		}
//    	}
//    	
//    	return outgoingEdges;
//    }
    
    public boolean hasComponentID(String compID) {
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.hasComponentID(compID)) {
    				return true;
    			}
    		}
    		
    		return false;
    	} else {
    		return false;
    	}
    }
    
    public boolean hasEdges() {
    	if (edges == null) {
    		return false;
    	} else {
    		return edges.size() > 0;
    	}
    }
    
    public Edge getEdge(Node head) {
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead().equals(head)) {
    				return edge;
    			}
    		}
    		
    		return null;
    	} else {
    		return null;
    	}
    }
    
    public boolean hasEdge(Node head) {
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead().equals(head)) {
    				return true;
    			}
    		}
    		
    		return false;
    	} else {
    		return false;
    	}
    }
    
    public Set<Edge> getMatchingEdges(Edge edge, int strength) {
    	Set<Edge> matchingEdges = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge e : edges) {
    			if (edge.isMatchingTo(e, strength)) {
    				matchingEdges.add(e);
    			}
    		}
    	}
    	
    	return matchingEdges;
    }
    
    public boolean hasEdge(Edge edge) {
    	if (hasEdges()) {
    		for (Edge e : edges) {
    			if (edge.isIdenticalTo(e)) {
    				return true;
    			}
    		}
    		return false;
    	} else {
    		return false;
    	}
    }
    
    public boolean hasConflictingNodeType(Node node) {
    	return hasNodeType() && (!node.hasNodeType() || !nodeType.equals(node.getNodeType()));
    }
    
    public boolean hasNodeType() {
    	return nodeType != null;
    }
    
    public boolean isAcceptNode() {
    	return hasNodeType() && nodeType.equals(NodeType.ACCEPT.getValue());
    }
    
    public boolean isStartNode() {
    	return hasNodeType() && nodeType.equals(NodeType.START.getValue());
    }
    
    public boolean deleteEdges(Set<Edge> edges) {
    	if (hasEdges()) {
    		return this.edges.removeAll(edges);
    	} else {
    		return false;
    	}
    }
    
    public void clearEdges() {
    	if (hasEdges()) {
    		edges = null;
    	}
    }
    
    public enum NodeType {
    	START ("start"),
    	ACCEPT ("accept");
    	
    	private final String value;
    	
    	NodeType(String value) {
    		this.value = value;
    	}
    	
    	public String getValue() {
    		return value;
    	}
    }
    
    public void setNodeType(String nodeType) {
    	this.nodeType = nodeType;
    }
}
