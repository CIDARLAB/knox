package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "CONTAINS")
public class SpaceToNode {
	
    @GraphId
    Long id;
    
    @StartNode
    DesignSpace tail;
    
    @EndNode
    Node head;

    public SpaceToNode() {
    	
    }

    public DesignSpace getTail() {
        return tail;
    }

    public Node getHead() {
        return head;
    }
    
}
