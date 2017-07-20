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

    public Edge() {}

    public Edge(Node tail, Node head) {
        this.tail = tail;
        this.head = head;
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
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		this.tail = tail;
		this.head = head;
		this.componentIDs = componentIDs;
		this.componentRoles = componentRoles;
        this.weight = 1.0;
	}

    public void addComponent(String compID, String compRole) {
        if (!hasComponentIDs()) {
            componentIDs = new ArrayList<String>();
            componentRoles = new ArrayList<String>();
        }

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
        if (hasComponentIDs() && hasComponentRoles()) {
            return new Edge(tail, head, new ArrayList<String>(componentIDs),
                            new ArrayList<String>(componentRoles));
        } else {
            return new Edge(tail, head);
        }
    }

    public boolean deleteComponent(String compID) {
        if (hasComponentIDs()) {
            boolean result = componentIDs.remove(compID);

            if (componentIDs.size() == 0) {
                componentIDs = null;
                componentRoles = null;
            }

            return result;

        } else {
            return false;
        }
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
        if (hasComponentIDs() && edge.hasComponentIDs()) {
            componentIDs.retainAll(edge.getComponentIDs());
        }

        if (hasComponentRoles() && edge.hasComponentRoles()) {
            componentRoles.retainAll(edge.getComponentRoles());
        }

        if (componentIDs != null && componentIDs.size() == 0) {
            componentIDs = null;
            componentRoles = null;
        }
    }

    public boolean isComponentEdge() {
        return hasComponentIDs() && hasComponentRoles();
    }

    public boolean isIdenticalTo(Edge edge) {
        if (tail.isIdenticalTo(edge.getTail()) && head.isIdenticalTo(edge.getHead())) {
            return hasSameComponents(edge);
        } else {
            return false;
        }
    }
    
    public boolean isMatchingTo(Edge edge, int tolerance) {
    	if (tolerance == 0 && hasSameComponents(edge)
    			|| tolerance == 1 && hasSharedComponents(edge)
				|| tolerance == 2 && hasSharedComponents(edge)
				|| tolerance == 3 && hasSameRoles(edge)
				|| tolerance == 4 && hasSharedRoles(edge)) {
			return true;
		} else {
			return false;
		}
    }

    public boolean hasSameComponents(Edge edge) {
        if (hasComponentIDs() && edge.hasComponentIDs()) {
            Set<String> compIDs1 = new HashSet<String>(componentIDs);
            Set<String> compIDs2 = new HashSet<String>(edge.getComponentIDs());

            return compIDs1.equals(compIDs2);
        } else {
            return false;
        }
    }

    public boolean hasSharedComponents(Edge edge) {
        if (hasComponentIDs() && edge.hasComponentIDs()) {
            Set<String> compIDs1 = new HashSet<String>(componentIDs);
            Set<String> compIDs2 = new HashSet<String>(edge.getComponentIDs());

            compIDs1.retainAll(compIDs2);

            return compIDs1.size() > 0;
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
        Set<String> mergedCompIDs = new HashSet<String>();
        
        Set<String> mergedCompRoles = new HashSet<String>();

        if (hasComponentIDs() && edge.hasComponentIDs()) {
            mergedCompIDs.addAll(componentIDs);

            for (String compID : edge.getComponentIDs()) {
                if (!mergedCompIDs.contains(compID)) {
                    componentIDs.add(compID);
                    
                    mergedCompIDs.add(compID);
                }
            }
        }

        if (hasComponentRoles() && edge.hasComponentRoles()) {
            mergedCompRoles.addAll(componentRoles);

            for (String compRole : edge.getComponentRoles()) {
                if (!mergedCompRoles.contains(compRole)) {
                    componentRoles.add(compRole);
                    
                    mergedCompRoles.add(compRole);
                }
            }
        }
    }

    public void setProbability(double weight) {
        this.weight = weight;
    }

    public double getWeight() { 
    	return weight; 
    }
}
