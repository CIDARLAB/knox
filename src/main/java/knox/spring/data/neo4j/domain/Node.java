package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
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
    
    public Edge copyEdge(Edge edge) {
    	return createEdge(edge.getHead(), edge.getComponentIDs(), edge.getComponentRoles());
    }
    
    public Edge createEdge(Node head, ArrayList<String> compIDs, ArrayList<String> compRoles) {
    	Edge edge = new Edge(this, head, compIDs, compRoles);
    	addEdge(edge);
    	return edge;
    }
    
    public String getNodeID() {
    	return nodeID;
    }

    public Set<Edge> getEdges() {
        return edges;
    }
    
    public Set<Edge> getMinimizableEdges() {
    	Set<Edge> minimizableEdges = new HashSet<Edge>();
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (!edge.hasComponents() 
    					&& !(edge.getTail().isStartNode() && edge.getHead().isAcceptNode() 
						|| edge.getHead().isStartNode())) {
    				minimizableEdges.add(edge);
    			}
    		}
    	}
    	return minimizableEdges;
    }
    
    public String getNodeType() {
    	return nodeType;
    }
    
    public boolean hasMinimizableEdges() {
    	return getMinimizableEdges().size() > 0;
    }
    
    public boolean hasEdges() {
    	if (edges == null) {
    		return false;
    	} else {
    		return edges.size() > 0;
    	}
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
    
    public boolean hasNodeType() {
    	return nodeType != null;
    }
    
    public boolean isAcceptNode() {
    	return hasNodeType() && nodeType.equals(NodeType.ACCEPT.getValue());
    }
    
    public boolean isStartNode() {
    	return hasNodeType() && nodeType.equals(NodeType.START.getValue());
    }
    
    public Set<Edge> minimizeEdges() {
    	Set<Edge> minimizableEdges = getMinimizableEdges();
    	if (hasEdges()) {
    		edges.removeAll(minimizableEdges);
    	}
    	return minimizableEdges;
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
