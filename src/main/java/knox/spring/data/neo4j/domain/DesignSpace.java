package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class DesignSpace {
	
    @GraphId
    Long id;
    
    String spaceID;
    
    int idIndex;
    
    @Relationship(type = "CONTAINS") 
    Set<Node> nodes;
    
    @Relationship(type = "CONTAINS") 
    Set<Branch> branches;
    
    @Relationship(type = "SELECTS") 
    Branch headBranch;

    public DesignSpace() {
    	
    }
    
    public String getSpaceID() {
    	return spaceID;
    }
    
    public int getIDIndex() {
    	return idIndex;
    }
    
    public Set<Node> getNodes() {
    	return nodes;
    }
    
    public Set<Branch> getBranches() {
    	return branches;
    }
    
    public Branch getHeadBranch() {
    	return headBranch;
    }
    
    public Node createNode(String nodeType) {
    	if (nodes == null) {
    		nodes = new HashSet<Node>();
    	}
    	Node node = new Node("n" + idIndex++, nodeType);
    	nodes.add(node);
    	return node;
    }
    
    public Node copyNode(Node node) {
    	return createNode(node.getNodeType());
    }
    
    public boolean hasNodes() {
    	if (nodes == null) {
    		return false;
    	} else {
    		return nodes.size() > 0;
    	}
    }
    
    public boolean hasBranches() {
    	if (branches == null) {
    		return false;
    	} else {
    		return branches.size() > 0;
    	}
    }

}
