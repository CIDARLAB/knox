package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@RelationshipEntity(type = "PRECEDES")
public class Edge {
    @GraphId Long id;

    @StartNode Node tail;

    @EndNode Node head;

    ArrayList<String> componentIDs;
    
    ArrayList<String> componentRoles;

    double weight;

    public Edge() {
    	
    }

    public Edge(Node tail, Node head) {
        this.tail = tail;
        
        this.head = head;
        
        componentIDs = new ArrayList<String>();
        
        componentRoles = new ArrayList<String>();
        
        this.weight = 1.0;
    }
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		this.tail = tail;
		
		this.head = head;
		
		this.componentIDs = componentIDs;
		
		this.componentRoles = componentRoles;
		
        this.weight = 1.0;
	}

    public Edge(Node tail, Node head, ArrayList<String> componentIDs, ArrayList<String> componentRoles,
                double weight) {
        this.tail = tail;
        
        this.head = head;
        
        this.componentIDs = componentIDs;
        
        this.componentRoles = componentRoles;
        
        this.weight = weight;
    }

    public void addComponent(String compID, String compRole) {
        if (!componentIDs.contains(compID)) {
            componentIDs.add(compID);
        }

        if (!componentRoles.contains(compRole)) {
            componentRoles.add(compRole);
        }
    }

    public void setComponent(String compID, String compRole) {
        componentIDs = new ArrayList<String>();
        
        componentRoles = new ArrayList<String>();

        componentIDs.add(compID);
        
        componentIDs.add(compRole);
    }

    public Edge copy(Node tail, Node head) {
    	return new Edge(tail, head, new ArrayList<String>(componentIDs),
    			new ArrayList<String>(componentRoles));
    }

    public boolean deleteComponent(String compID) {
    	return componentIDs.remove(compID);
    }

    public Node getTail() {
    	return tail; 
    }

    public Node getHead() { 
    	return head; 
    }

    public String getComponentID(int i) {
        if (i >= 0 && i < componentIDs.size()) {
            return componentIDs.get(i);
        } else {
            return null;
        }
    }
    
    public ArrayList<String> getComponentIDs() {
    	return componentIDs; 
    }

    public int getNumComponentIDs() { 
    	return componentIDs.size(); 
    }

    public ArrayList<String> getComponentRoles() { 
    	return componentRoles; 
    }

    public boolean hasComponentID(String compID) {
    	return componentIDs.contains(compID);
    }

    public boolean hasComponentIDs() {
    	return componentIDs != null && !componentIDs.isEmpty();
    }

    public boolean hasComponentRole(String compRole) {
    	return componentRoles.contains(compRole);
    }
    
    public boolean hasComponentRole(Set<String> compRoles) {
    	Set<String> tempRoles = new HashSet<String>(componentRoles);
    	
    	tempRoles.retainAll(compRoles);
    	
    	return !tempRoles.isEmpty();
    }

    public boolean hasComponentRoles() {
    	return componentRoles != null && !componentRoles.isEmpty();
    }

//    public boolean hasComponents() {
//        return hasComponentIDs() && hasComponentRoles();
//    }

    public void intersectWithEdge(Edge edge) {
    	componentIDs.retainAll(edge.getComponentIDs());

    	componentRoles.retainAll(edge.getComponentRoles());
    }
    
    public boolean isBlank() {
    	return !hasComponentIDs() && !hasComponentRoles();
    }

    public boolean isIdenticalTo(Edge edge) {
        if (tail.isIdenticalTo(edge.getTail()) && head.isIdenticalTo(edge.getHead())) {
            return hasSameComponents(edge);
        } else {
            return false;
        }
    }
    
    public boolean isMatchingTo(Edge edge, int tolerance, Set<String> roles) {
    	if ((roles.isEmpty() || hasComponentRole(roles) && edge.hasComponentRole(roles)) 
    			&& (tolerance == 0 && hasSameComponents(edge)
    					|| (tolerance == 1 || tolerance == 2) && hasSharedComponents(edge)
    					|| tolerance == 3 && hasSameRoles(edge)
    					|| tolerance == 4 && hasSharedRoles(edge))) {
			return true;
		} else {
			return false;
		}
    }

    public boolean hasSameComponents(Edge edge) {
        if (hasComponentIDs() && edge.hasComponentIDs()) {
            Set<String> compIDs1 = new HashSet<String>(componentIDs);
            
            Set<String> compIDs2 = new HashSet<String>(edge.getComponentIDs());

            return compIDs1.equals(compIDs2) && hasSameRoles(edge);
        } else if (isBlank() && edge.isBlank()) {
        	return true;
        } else {
            return false;
        }
    }

    public boolean hasSharedComponents(Edge edge) {
        if (hasComponentIDs() && edge.hasComponentIDs()) {
            Set<String> compIDs1 = new HashSet<String>(componentIDs);
            
            Set<String> compIDs2 = new HashSet<String>(edge.getComponentIDs());

            compIDs1.retainAll(compIDs2);

            return compIDs1.size() > 0 && hasSharedRoles(edge);
        } else if (isBlank() && edge.isBlank()) {
        	return true;
        } else {
            return false;
        }
    }
    
    public boolean hasSameRoles(Edge edge) {
    	if (hasComponentRoles() && edge.hasComponentRoles()) {
    		Set<String> compRoles1 = new HashSet<String>(componentRoles);

    		Set<String> compRoles2 = new HashSet<String>(edge.getComponentRoles());

    		return compRoles1.equals(compRoles2);
    	} else if (!hasComponentRoles() && !edge.hasComponentRoles()) {
        	return true;
        } else {
            return false;
        }
    }

    public boolean hasSharedRoles(Edge edge) {
    	if (hasComponentRoles() && edge.hasComponentRoles()) {
    			Set<String> compRoles1 = new HashSet<String>(componentRoles);
    			
    			Set<String> compRoles2 = new HashSet<String>(edge.getComponentRoles());

    			compRoles1.retainAll(compRoles2);

    			return compRoles1.size() > 0;
    	} else if (!hasComponentRoles() && !edge.hasComponentRoles()) {
    		return true;
    	} else {
    		return false;
    	}
    }

    public boolean isCyclic() { 
    	return tail.isIdenticalTo(head);
    }

    public void setHead(Node head) {
    	this.head = head; 
    }

    public void setTail(Node tail) { 
    	this.tail = tail; 
    }

    public void unionWithEdge(Edge edge) {
        Set<String> mergedIDs = new HashSet<String>();
        
        mergedIDs.addAll(componentIDs);

        for (String compID : edge.getComponentIDs()) {
        	if (!mergedIDs.contains(compID)) {
        		componentIDs.add(compID);

        		mergedIDs.add(compID);
        	}
        }
        
        Set<String> mergedRoles = new HashSet<String>();
        
        mergedRoles.addAll(componentRoles);

        for (String compRole : edge.getComponentRoles()) {
        	if (!mergedRoles.contains(compRole)) {
        		componentRoles.add(compRole);

        		mergedRoles.add(compRole);
        	}
        }
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() { 
    	return weight; 
    }
}
