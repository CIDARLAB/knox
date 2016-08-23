package knox.spring.data.neo4j.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

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
    
    public Snapshot copySnapshot(Snapshot snapshot) {
    	createSnapshot(snapshot.getIdIndex());
    	
    	HashMap<String, Node> nodeIDToCopy = new HashMap<String, Node>();
    	
    	HashMap<String, Set<Edge>> nodeIDToEdges = new HashMap<String, Set<Edge>>();
    	
    	for (Node node : snapshot.getNodes()) {
    		nodeIDToCopy.put(node.getNodeID(), this.snapshot.copyNodeWithID(node));
    		
    		if (node.hasEdges()) {
    			nodeIDToEdges.put(node.getNodeID(), node.getEdges());
    		}
    	}
    	
    	for (Node nodeCopy : this.snapshot.getNodes()) {
    		if (nodeIDToEdges.containsKey(nodeCopy.getNodeID())) {
    			for (Edge edge : nodeIDToEdges.get(nodeCopy.getNodeID())) {
    				nodeCopy.copyEdge(edge, nodeIDToCopy.get(edge.getHead().getNodeID()));
    			}
    		}
    	}
    	
    	return this.snapshot;
    }
    
    public Snapshot createSnapshot() {
    	snapshot = new Snapshot(0);
    	return snapshot;
    }
    
    public Snapshot createSnapshot(int idIndex) {
    	snapshot = new Snapshot(idIndex);
    	return snapshot;
    }
    
    public Commit findPredecessor(String predecessorID) {
 	   if (hasPredecessors()) {
    		for (Commit predecessor : predecessors) {
        		if (predecessor.getCommitID().equals(predecessorID)) {
        			return predecessor;
        		}
        	}
    		return null;
    	} else {
    		return null;
    	}
    }
    
    public Set<Commit> getHistory() {
    	Set<Commit> history = new HashSet<Commit>();
    	
    	if (hasPredecessors()) {
    		Stack<Commit> commitStack = new Stack<Commit>();
    		
    		commitStack.push(this);
    		
    		while (commitStack.size() > 0) {
    			Commit commit = commitStack.pop();
    			
    			history.add(commit);
    			
    			if (commit.hasPredecessors()) {
    				for (Commit predecessor : commit.getPredecessors()) {
    					commitStack.push(predecessor);
    				}
    			}
    		}
    	}
    	
    	return history;
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
    
    public boolean hasPredecessors() {
    	return predecessors != null && predecessors.size() > 0;
    }
    
    public void addPredecessor(Commit predecessor) {
    	if (predecessors == null) {
    		predecessors = new HashSet<Commit>();
    	}
    	predecessors.add(predecessor);
    }
}
