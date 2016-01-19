package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "ARCHIVES")
public class NodeArchive {
	
    @GraphId
    Long id;
    
    @StartNode
    Commit tail;
    
    @EndNode
    Node head;

    public NodeArchive() {
    	
    }

    public Commit getTail() {
        return tail;
    }

    public Node getHead() {
        return head;
    }
    
}
