package knox.spring.data.neo4j.domain;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class NodeSpace {
	
	@GraphId
    Long id;
	
	int idIndex;
	
	@Relationship(type = "CONTAINS") 
    Set<Node> nodes;
	
	public NodeSpace() {
		
	}
	
	public NodeSpace(int idIndex) {
		this.idIndex = idIndex;
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
	
	public Set<Node> getAcceptNodes() {
    	Set<Node> acceptNodes = new HashSet<Node>();
    	if (hasNodes()) {
    		for (Node node : nodes) {
        		if (node.hasNodeType() && node.getNodeType().equals(Node.NodeType.ACCEPT.getValue())) {
        			acceptNodes.add(node);
        		}
        	}
    	}
    	return acceptNodes;
    }
	
	public int getIDIndex() {
		return idIndex;
	}
    
    public Set<Node> getNodes() {
    	return nodes;
    }
    
    public Node getStartNode() {
    	if (hasNodes()) {
    		for (Node node : nodes) {
        		if (node.hasNodeType() && node.getNodeType().equals(Node.NodeType.START.getValue())) {
        			return node;
        		}
        	}
        	return null;
    	} else {
    		return null;
    	}
    }
    
    public boolean hasNodes() {
    	if (nodes == null) {
    		return false;
    	} else {
    		return nodes.size() > 0;
    	}
    }
    
    public void setIDIndex(int idIndex) {
    	this.idIndex = idIndex;
    }
}
