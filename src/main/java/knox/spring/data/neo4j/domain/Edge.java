package knox.spring.data.neo4j.domain;

import java.util.ArrayList;

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
    
    public boolean hasRoles() {
    	return componentRoles != null && componentRoles.size() > 0;
    }
    
}
