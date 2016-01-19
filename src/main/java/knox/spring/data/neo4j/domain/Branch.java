package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class Branch {
	
    @GraphId
    Long id;
    
    String branchID;
    
    @Relationship(type = "POINTS") 
    CommitLink commitLink;

    public Branch() {
    	
    }
    
    public CommitLink getCommitLink() {
    	return commitLink;
    }
    
    public String getBranchID() {
    	return branchID;
    }

}
