package knox.spring.data.neo4j.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// import org.neo4j.ogm.annotation.*;

// import com.fasterxml.jackson.annotation.JsonIdentityInfo;
// import com.voodoodyne.jackson.jsog.JSOGGenerator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.neo4j.ogm.annotation.Relationship;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class DesignSpace extends NodeSpace {
    String spaceID;

    @Relationship(type = "ARCHIVES") Set<Branch> branches;

    @Relationship(type = "SELECTS") Branch headBranch;

    int mergeIndex;

    public DesignSpace() {}

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

    public boolean containsCommit(Commit commit) {
        if (hasBranches()) {
            for (Branch branch : branches) {
                if (branch.containsCommit(commit)) {
                    return true;
                }
            }
        }

        return false;
    }

    public DesignSpace copy(String copyID) {
        DesignSpace spaceCopy = new DesignSpace(copyID, idIndex, mergeIndex);

        if (hasNodes()) {
            HashMap<String, Node> idToNodeCopy = new HashMap<String, Node>();

            for (Node node : nodes) {
                idToNodeCopy.put(node.getNodeID(),
                                 spaceCopy.copyNodeWithID(node));
            }

            for (Node node : nodes) {
                if (node.hasEdges()) {
                    Node nodeCopy = idToNodeCopy.get(node.getNodeID());

                    for (Edge edge : node.getEdges()) {
                        nodeCopy.copyEdge(edge, idToNodeCopy.get(edge.getHead().getNodeID()));
                    }
                }
            }
        }

        return spaceCopy;
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

    public Set<Branch> getBranches() { return branches; }

    public Branch getHeadBranch() { return headBranch; }

    public int getMergeIndex() { return mergeIndex; }

    public String getSpaceID() { return spaceID; }

    public void setSpaceID(String spaceID) { this.spaceID = spaceID; }

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
