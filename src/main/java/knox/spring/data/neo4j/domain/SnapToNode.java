package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "CONTAINS")
public class SnapToNode {
	
    @GraphId
    Long id;
    
    @StartNode
    Snapshot tail;
    
    @EndNode
    Node head;

    public SnapToNode() {
    	
    }

    public Snapshot getTail() {
        return tail;
    }

    public Node getHead() {
        return head;
    }
    
}
