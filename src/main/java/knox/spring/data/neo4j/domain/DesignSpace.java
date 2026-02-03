package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import java.util.Stack;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Property;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class DesignSpace extends NodeSpace {
    @Property 
	private String spaceID;

	@Property 
	private String groupID;

    @Relationship(type = "ARCHIVES") 
	Set<Branch> branches;

    @Relationship(type = "SELECTS") 
	Branch headBranch;

    int commitIndex;

    public DesignSpace() {
    	
    }

    public DesignSpace(String spaceID) {
        super(0);

        this.spaceID = spaceID;

		this.groupID = "none";
        
        branches = new HashSet<Branch>();
    }

	public DesignSpace(String spaceID, String groupID) {
        super(0);

        this.spaceID = spaceID;

		this.groupID = groupID;
		if (this.groupID.equals("")) {
			this.groupID = "none";
		}
        
        branches = new HashSet<Branch>();
    }
    
    public DesignSpace(String spaceID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
    	super(componentIDs, componentRoles);
    	
    	this.spaceID = spaceID;
    	
    	branches = new HashSet<Branch>();
    }

    public void addBranch(Branch branch) {
        branches.add(branch);
    }

    public boolean containsCommit(Commit commit) {
    	for (Branch branch : branches) {
    		if (branch.containsCommit(commit)) {
    			return true;
    		}
    	}

        return false;
    }
    
    public void updateCommitIDs() {
    	commitIndex = 0;
    	
    	for (Commit commit : getCommits()) {
    		commit.setCommitID("c" + commitIndex++);
    	}
    }
    
    public void commitToHead() {
    	if (hasHeadBranch()) {
    		Commit commit = createCommit(headBranch);

    		if (headBranch.hasLatestCommit()) {
    			Set<Commit> predecessors = new HashSet<Commit>();

    			predecessors.add(headBranch.getLatestCommit());

    			commit.setPredecessors(predecessors);
    		}
    		
    		headBranch.setLatestCommit(commit);
    		
    		commit.createSnapshot().copyNodeSpace(this);
    	}
    }
    
    public Branch copyVersionHistory(DesignSpace space) {
    	Branch headBranchCopy = null;
    	
    	HashMap<String, Commit> idToCommitCopy = new HashMap<String, Commit>();

    	for (Branch branch : space.getBranches()) {
    		Branch branchCopy = branch.shallowCopy();

    		for (Commit commit : branch.getCommits()) {
    			if (!idToCommitCopy.containsKey(commit.getCommitID())) {
    				idToCommitCopy.put(commit.getCommitID(), commit.copy());
    			}

    			branchCopy.addCommit(idToCommitCopy.get(commit.getCommitID()));
    		}

    		for (Commit commit : branch.getCommits()) {
    			if (commit.hasPredecessors()) {
    				for (Commit predecessor : commit.getPredecessors()) {
    					idToCommitCopy.get(commit.getCommitID())
    					.addPredecessor(idToCommitCopy.get(predecessor.getCommitID()));
    				}
    			}
    		}

    		if (branch.hasLatestCommit() 
    				&& idToCommitCopy.containsKey(branch.getLatestCommit().getCommitID())) {
    			branchCopy.setLatestCommit(idToCommitCopy.get(branch.getLatestCommit().getCommitID()));
    		}

    		addBranch(branchCopy);

    		if (branch.isIdenticalTo(space.getHeadBranch())) {
    			headBranchCopy = branchCopy;
    		}
    	}
        	
    	return headBranchCopy;
    }
    
    public Branch createBranch(String branchID) {
    	Branch branch = new Branch(branchID);
    	
        addBranch(branch);
        
        return branch;
    }

    public Branch createHeadBranch(String branchID) {
        Branch headBranch = createBranch(branchID);
        
        this.headBranch = headBranch;
        
        return headBranch;
    }
    
    public Commit copyCommit(Branch branch, Commit commit) {
        Commit commitCopy = createCommit(branch);

        commitCopy.copySnapshot(commit.getSnapshot());

        return commitCopy;
    }

    public Commit createCommit(Branch branch) {
        Commit commit = new Commit("c" + commitIndex++);

        branch.addCommit(commit);

        return commit;
    }

    public Branch getBranch(String branchID) {
    	for (Branch branch : branches) {
    		if (branch.getBranchID().equals(branchID)) {
    			return branch;
    		}
    	}

    	return null;
    }

    public Set<Branch> getBranches() { 
    	return branches; 
    }

    public Branch getHeadBranch() { 
    	return headBranch; 
    }
    
    public boolean hasHeadBranch() {
    	return headBranch != null;
    }

    public Snapshot getHeadSnapshot() {
    	return headBranch.getLatestCommit().getSnapshot();
    }

    public String getSpaceID() { 
    	return spaceID; 
    }

    public void setSpaceID(String spaceID) { 
    	this.spaceID = spaceID; 
    }

    public boolean hasBranches() {
    	return branches != null && !branches.isEmpty();
    }

    public void setHeadBranch(Branch headBranch) {
        this.headBranch = headBranch;
    }
    
    public void setBranches(Set<Branch> branches) {
    	this.branches = branches;
    }
    
    public Set<Commit> getCommits() {
    	Set<Commit> commits = new HashSet<Commit>();
    	
    	for (Branch branch : branches) {
    		Stack<Commit> commitStack = new Stack<Commit>();
    		
    		commitStack.push(branch.getLatestCommit());

    		while (!commitStack.isEmpty()) {
    			Commit commit = commitStack.pop();

    			commits.add(commit);

    			if (commit.hasPredecessors()) {
    				for (Commit predecessor : commit.getPredecessors()) {
    					commitStack.push(predecessor);
    				}
    			}
    		}
    	}
    	
    	return commits;
    }

	public void setGroupID(String groupID) {
		this.groupID = groupID;
		if (this.groupID == null || this.groupID.equals("")) {
			this.groupID = "none";
		}
	}

	public String getGroupID() {
		return this.groupID;
	}
}
