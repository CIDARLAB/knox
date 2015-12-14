package knox.spring.data.neo4j.domain;

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
    
    String componentID;
    
    String componentRole;

    public Edge() {
    	
    }

    public Node getTail() {
        return tail;
    }

    public Node getHead() {
        return head;
    }
    
    public String getComponentID() {
    	return componentID;
    }
    
    public String getComponentRole() {
    	return componentRole;
    }
    
}
