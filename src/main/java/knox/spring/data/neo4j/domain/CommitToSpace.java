package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "CONTAINS")
public class CommitToSpace {
	
    @GraphId
    Long id;
    
    @StartNode
    Commit tail;
    
    @EndNode
    DesignSpace head;

    public CommitToSpace() {
    	
    }

    public Commit getTail() {
        return tail;
    }

    public DesignSpace getHead() {
        return head;
    }
    
}
