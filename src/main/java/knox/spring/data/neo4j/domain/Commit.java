package knox.spring.data.neo4j.domain;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class Commit {
	
    @GraphId
    Long id;
    
    String commitID;
    
    @Relationship(type = "SUCCEEDS")
    Set<Commit> predecessors;
    
    @Relationship(type = "CONTAINS") 
    Snapshot snapshot;
    
    public Commit() {
    	
    }
 
    public Commit(String commitID) {
    	this.commitID = commitID;
    }
    
    public Snapshot createSnapshot() {
    	snapshot = new Snapshot();
    	return snapshot;
    }
    
    public Set<Commit> getPredecessors() {
    	return predecessors;
    }
    
    public Snapshot getSnapshot() {
    	return snapshot;
    }

    public String getCommitID() {
    	return commitID;
    }
    
    public void addPredecessor(Commit predecessor) {
    	if (predecessors == null) {
    		predecessors = new HashSet<Commit>();
    	}
    	predecessors.add(predecessor);
    }
}
