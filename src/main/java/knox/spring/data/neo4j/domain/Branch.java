package knox.spring.data.neo4j.domain;

import java.util.Set;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class Branch {
	
    @GraphId
    Long id;
    
    String branchID;
    
    int idIndex;
    
    @Relationship(type = "CONTAINS") 
    Set<BranchToCommit> branchToCommit;
    
    @Relationship(type = "LATEST") 
    HeadCommit latestCommit;

    public Branch() {
    	
    }
    
    public Set<BranchToCommit> getBranchToCommit() {
    	return branchToCommit;
    }
    
    public HeadCommit getLatestCommit() {
    	return latestCommit;
    }
    
    public String getBranchID() {
    	return branchID;
    }
    
    public int getIDIndex() {
    	return idIndex;
    }

}
