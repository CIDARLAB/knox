package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "SELECTS")
public class HeadLink {
	
    @GraphId
    Long id;
    
    @StartNode
    DesignSpace tail;
    
    @EndNode
    Branch head;

    public HeadLink() {
    	
    }

    public DesignSpace getTail() {
        return tail;
    }

    public Branch getHead() {
        return head;
    }
    
}
