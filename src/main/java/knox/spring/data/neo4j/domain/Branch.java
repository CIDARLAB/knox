package knox.spring.data.neo4j.domain;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NodeEntity
public class Branch {
    @GraphId Long id;

    String branchID;

    int idIndex;

    @Relationship(type = "CONTAINS") Set<Commit> commits;

    @Relationship(type = "LATEST") Commit latestCommit;

    public Branch() {}
    
    public Branch(String branchID) {
        this.branchID = branchID;
        
        idIndex = 0;
    }

    public Branch(String branchID, int idIndex) {
        this.branchID = branchID;
        this.idIndex = idIndex;
    }

    public void addCommit(Commit commit) {
        if (!hasCommits()) {
            commits = new HashSet<Commit>();
        }
        commits.add(commit);
    }

    public boolean containsCommit(Commit commit) {
        if (hasCommits()) {
            return commits.contains(commit);
        } else {
            return false;
        }
    }
    
    public Branch copy() {
    	return new Branch(branchID, idIndex);
    }

    public Commit copyCommit(Commit commit) {
        Commit commitCopy = createCommit();

        commitCopy.copySnapshot(commit.getSnapshot());

        return commitCopy;
    }

    public Commit createCommit() {
        Commit commit = new Commit("c" + idIndex++);

        addCommit(commit);

        return commit;
    }

    public boolean deleteCommits(Set<Commit> deletedCommits) {
        if (hasCommits()) {
            return commits.removeAll(deletedCommits);
        } else {
            return false;
        }
    }

    public Set<Commit> getCommits() { 
    	return commits; 
    }

    public void setCommits(Set<Commit> commits) {
    	this.commits = commits;
    }
    
    public Commit getLatestCommit() { 
    	return latestCommit; 
    }
    
    public void clearLatestCommit() {
    	latestCommit = null;
    }
    
    public void clearCommits() {
    	commits = null;
    }

    public String getBranchID() { 
    	return branchID; 
    }

    public int getIdIndex() { 
    	return idIndex; 
    }
    
    public void setIDIndex(int idIndex) {
    	this.idIndex = idIndex;
    }

    public boolean hasCommits() {
        if (commits == null) {
            return false;
        } else {
            return commits.size() > 0;
        }
    }

    public Set<Commit> retainCommits(Set<Commit> retainedCommits) {
        Set<Commit> diffCommits = new HashSet<Commit>();

        if (hasCommits()) {
            for (Commit commit : commits) {
                if (!retainedCommits.contains(commit)) {
                    diffCommits.add(commit);
                }
            }

            deleteCommits(diffCommits);
        }

        return diffCommits;
    }
    
    public boolean isIdenticalTo(Branch branch) {
    	return branch.getBranchID().equals(branchID);
    }

    public void setLatestCommit(Commit commit) { 
    	latestCommit = commit; 
    }
}
