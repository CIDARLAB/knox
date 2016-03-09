package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

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
    
    public String getNodeID() {
    	return nodeID;
    }

    public Set<Edge> getEdges() {
        return edges;
    }
    
    public String getNodeType() {
    	return nodeType;
    }
    
    public void addEdge(Edge edge) {
    	if (edges == null) {
    		edges = new HashSet<Edge>();
    	}
    	edges.add(edge);
    }
    
    public boolean hasEdges() {
    	if (edges == null) {
    		return false;
    	} else {
    		return edges.size() > 0;
    	}
    }
    
    public boolean hasEdge(Edge targetEdge) {
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (targetEdge.isIdenticalTo(edge)) {
    				return true;
    			}
    		}
    		return false;
    	} else {
    		return false;
    	}
    }
    
}
