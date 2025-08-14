package knox.spring.data.neo4j.domain;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class Branch {
    @Id
    @GeneratedValue
    Long id;

    @Property String branchID;

    @Relationship(type = "CONTAINS") 
    Set<Commit> commits;

    @Relationship(type = "LATEST") 
    Commit latestCommit;

    public Branch() {}
    
    public Branch(String branchID) {
        this.branchID = branchID;
        
        commits = new HashSet<Commit>();
    }
    
    public Branch(String branchID, Commit latestCommit) {
        this.branchID = branchID;
        
        this.latestCommit = latestCommit;
        
        commits = new HashSet<Commit>();
    }

    public void addCommit(Commit commit) {
        commits.add(commit);
    }
    
    public void addAllCommits(Set<Commit> commits) {
        commits.addAll(commits);
    }

    public boolean containsCommit(Commit commit) {
    	return commits.contains(commit);
    }
    
    public Branch shallowCopy() {
    	return new Branch(branchID);
    }
    
    public Branch copy() {
    	Branch branchCopy = new Branch(branchID, latestCommit);
    	
    	branchCopy.addAllCommits(commits);
    	
    	return branchCopy;
    }

    public boolean deleteCommits(Set<Commit> deletedCommits) {
    	return commits.removeAll(deletedCommits);
    }

    public Set<Commit> getCommits() { 
    	return commits; 
    }
    
    public int getNumCommits() {
    	return commits.size();
    }

    public void setCommits(Set<Commit> commits) {
    	this.commits = commits;
    }
    
    public Commit getLatestCommit() { 
    	return latestCommit; 
    }

    public String getBranchID() { 
    	return branchID; 
    }

    public void setBranchID(String branchID) {

        this.branchID = branchID;
    }
    
    public boolean hasLatestCommit() {
    	return latestCommit != null;
    }

    public boolean hasCommits() {
    	return commits != null && !commits.isEmpty();
    }

    public Set<Commit> retainCommits(Set<Commit> retainedCommits) {
        Set<Commit> diffCommits = new HashSet<Commit>();
        
        for (Commit commit : commits) {
        	if (!retainedCommits.contains(commit)) {
        		diffCommits.add(commit);
        	}
        }

        deleteCommits(diffCommits);

        return diffCommits;
    }
    
    public boolean isIdenticalTo(Branch branch) {
    	return branch.getBranchID().equals(branchID);
    }

    public void setLatestCommit(Commit commit) { 
    	latestCommit = commit; 
    }
}
