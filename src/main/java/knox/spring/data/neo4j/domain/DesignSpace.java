package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
public class DesignSpace extends NodeSpace {
    
    String spaceID;
    
    @Relationship(type = "ARCHIVES") 
    Set<Branch> branches;
    
    @Relationship(type = "SELECTS") 
    Branch headBranch;
    
    int mergeIndex;

    public DesignSpace() {
    	
    }
    
    public DesignSpace(String spaceID) {
    	super(0);
    	this.spaceID = spaceID;
    	this.mergeIndex = 0;
    }
    
    public DesignSpace(String spaceID, int idIndex, int mergeIndex) {
    	super(idIndex);
    	this.spaceID = spaceID;
    	this.mergeIndex = mergeIndex;
    }
    
    public void addBranch(Branch branch) {
    	if (branches == null) {
    		branches = new HashSet<Branch>();
    	}
    	branches.add(branch);
    }
    
    public Branch createBranch(String branchID, int idIndex) {
    	Branch branch = new Branch(branchID, idIndex);
    	addBranch(branch);
    	return branch;
    }
    
    public Branch createHeadBranch(String branchID) {
    	Branch headBranch = createBranch(branchID, 0);
    	this.headBranch = headBranch;
    	return headBranch;
    }
    
    public Branch findBranch(String branchID) {
    	if (hasBranches()) {
    		for (Branch branch : branches) {
        		if (branch.getBranchID().equals(branchID)) {
        			return branch;
        		}
        	}
    		return null;
    	} else {
    		return null;
    	}
    }
    
    public Set<Branch> getBranches() {
    	return branches;
    }
    
    public Branch getHeadBranch() {
    	return headBranch;
    }
    
    public int getMergeIndex() {
    	return mergeIndex;
    }
    
    public String getSpaceID() {
    	return spaceID;
    }
    
    public boolean hasBranches() {
    	if (branches == null) {
    		return false;
    	} else {
    		return branches.size() > 0;
    	}
    }
    
    public void setHeadBranch(Branch headBranch) {
    	this.headBranch = headBranch;
    }
}
