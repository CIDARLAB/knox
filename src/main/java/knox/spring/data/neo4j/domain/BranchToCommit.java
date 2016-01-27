package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "CONTAINS")
public class BranchToCommit {
	
    @GraphId
    Long id;
    
    @StartNode
    Branch tail;
    
    @EndNode
    Commit head;

    public BranchToCommit() {
    	
    }

    public Branch getTail() {
        return tail;
    }

    public Commit getHead() {
        return head;
    }
    
}
