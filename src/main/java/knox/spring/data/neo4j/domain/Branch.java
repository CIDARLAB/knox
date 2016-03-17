package knox.spring.data.neo4j.domain;

import java.util.HashSet;
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
    Set<Commit> commits;
    
    @Relationship(type = "LATEST") 
    Commit latestCommit;

    public Branch() {
    	
    }
    
    public Branch(String branchID, int idIndex) {
    	this.branchID = branchID;
    	this.idIndex = idIndex;
    }
    
    public void addCommit(Commit commit) {
    	if (commits == null) {
    		commits = new HashSet<Commit>();
    	}
    	commits.add(commit);
    }
    
   public Commit createCommit() {
	   latestCommit = new Commit("c" + idIndex++);
	   addCommit(latestCommit);
	   return latestCommit;
   }
    
    public Set<Commit> getCommits() {
    	return commits;
    }
    
    public Commit getLatestCommit() {
    	return latestCommit;
    }
    
    public String getBranchID() {
    	return branchID;
    }
    
    public int getIDIndex() {
    	return idIndex;
    }
    
    public boolean hasCommits() {
    	if (commits == null) {
    		return false;
    	} else {
    		return commits.size() > 0;
    	}
    }

}
