package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class Node {
	
    @GraphId 
    Long id;
    
    String nodeID;

    @Relationship(type="PRECEDES") 
    Collection<Node> successors;
    
    String nodeType;

    public Node() {
    	
    }
    
    public String getNodeID() {
    	return nodeID;
    }

    public Collection<Node> getSuccessors() {
        return successors;
    }
    
    String getNodeType() {
    	return nodeType;
    }
    
}
