package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "PRECEDES")
public class Edge {
	
    @GraphId
    Long id;
    
    @StartNode
    Node tail;
    
    @EndNode
    Node head;
    
    ArrayList<String> componentIDs;
    
    ArrayList<String> componentRoles;

    public Edge() {
    	
    }
    
    public Edge(Node tail, Node head) {
    	this.tail = tail;
    	this.head = head;
    }
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
    	this.tail = tail;
    	this.head = head;
    	this.componentIDs = componentIDs;
    	this.componentRoles = componentRoles;
    }
    
    public Edge copy(Node tail, Node head) {
    	return new Edge(tail, head, new ArrayList<String>(componentIDs), new ArrayList<String>(componentRoles));
    }

    public Node getTail() {
        return tail;
    }

    public Node getHead() {
        return head;
    }
    
    public ArrayList<String> getComponentIDs() {
    	return componentIDs;
    }
    
    public ArrayList<String> getComponentRoles() {
    	return componentRoles;
    }
    
    public boolean hasComponentID(String compID) {
    	if (hasComponentIDs()) {
    		return componentIDs.contains(compID);
    	} else {
    		return false;
    	}
    }
    
    public boolean hasComponentIDs() {
    	if (componentIDs == null) {
    		return false;
    	} else {
    		return componentIDs.size() > 0;
    	}
    }
    
    public boolean hasComponentRole(String compRole) {
    	if (hasComponentRoles()) {
    		return componentRoles.contains(compRole);
    	} else {
    		return false;
    	}
    }
    
    public boolean hasComponentRoles() {
    	if (componentRoles == null) {
    		return false;
    	} else {
    		return componentRoles.size() > 0;
    	}
    }
    
    public boolean hasComponents() {
    	return hasComponentIDs() && hasComponentRoles();
    }
    
    public void intersectWithEdge(Edge edge) {
    	Set<String> intersectedCompIDs = new HashSet<String>();
    	
    	if (hasComponentIDs()) {
    		Set<String> tempIDs = new HashSet<String>(componentIDs);
    		
    		if (edge.hasComponentIDs()) {
    			for (String compID : edge.getComponentIDs()) {
    				if (tempIDs.contains(compID)) {
    					intersectedCompIDs.add(compID);
    				}
    			}
    		}
    	}
    	
    	Set<String> intersectedCompRoles = new HashSet<String>();
    	
    	if (hasComponentRoles()) {
    		Set<String> tempRoles = new HashSet<String>(componentRoles);
    		
    		if (edge.hasComponentRoles()) {
    			for (String compRole : edge.getComponentRoles()) {
    				if (tempRoles.contains(compRole)) {
    					intersectedCompRoles.add(compRole);
    				}
    			}
    		}
    	}
    	componentIDs = new ArrayList<String>(intersectedCompIDs);
    	componentRoles = new ArrayList<String>(intersectedCompRoles);
    }
    
    public boolean isIdenticalTo(Edge edge) {
    	if (tail.equals(edge.getTail()) && head.equals(edge.getHead())) {
    		return hasSameComponents(edge);
    	} else {
    		return false;
    	}
    }
    
    public boolean hasSameComponents(Edge edge) {
    	if (hasComponentIDs() && edge.hasComponentIDs() && hasComponentRoles() 
    			&& edge.hasComponentRoles()) {
        	Set<String> compIDs1 = new HashSet<String>();
    		Set<String> compIDs2 = new HashSet<String>();
    		compIDs1.addAll(componentIDs);
    		compIDs2.addAll(edge.getComponentIDs());
    		Set<String> compRoles1 = new HashSet<String>();
    		Set<String> compRoles2 = new HashSet<String>();
    		compRoles1.addAll(componentRoles);
    		compRoles2.addAll(edge.getComponentRoles());
    		return compIDs1.equals(compIDs2) && compRoles1.equals(compRoles2);
    	} else if (!hasComponentIDs() && !edge.hasComponentIDs() && !hasComponentRoles() 
    			&& !edge.hasComponentRoles()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public boolean hasSameComponentRoles(Edge edge) {
    	if (hasComponentRoles() && edge.hasComponentRoles()) {
    		Set<String> compRoles1 = new HashSet<String>();
    		Set<String> compRoles2 = new HashSet<String>();
    		compRoles1.addAll(componentRoles);
    		compRoles2.addAll(edge.getComponentRoles());
    		return compRoles1.equals(compRoles2);
    	} else if (!hasComponentRoles() && !edge.hasComponentRoles()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public boolean isCyclic() {
    	return tail.equals(head);
    }
    
    public void setHead(Node head) {
    	this.head = head;
    }
    
    public void setTail(Node tail) {
    	this.tail = tail;
    }
    
    public void unionWithEdge(Edge edge) {
    	Set<String> mergedCompIDs = new HashSet<String>();
    	Set<String> mergedCompRoles = new HashSet<String>();
    	
    	if (hasComponentIDs()) {
    		mergedCompIDs.addAll(componentIDs);
    	}
    	
    	if (edge.hasComponentIDs()) {
    		mergedCompIDs.addAll(edge.getComponentIDs());
    	}
    	
    	if (hasComponentRoles()) {
    		mergedCompRoles.addAll(componentRoles);
    	}
    	
    	if (edge.hasComponentRoles()) {
    		mergedCompRoles.addAll(edge.getComponentRoles());
    	}
    	
    	componentIDs = new ArrayList<String>(mergedCompIDs);
    	componentRoles = new ArrayList<String>(mergedCompRoles);
    }
    
}
