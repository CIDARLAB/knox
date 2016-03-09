package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class Commit {
	
    @GraphId
    Long id;
    
    String commitID;
    
    @Relationship(type = "CONTAINS") 
    Snapshot snapshot;
    
    public Commit() {
    	
    }
    
    public Snapshot getSnapshot() {
    	return snapshot;
    }

    public String getCommitID() {
    	return commitID;
    }
}
