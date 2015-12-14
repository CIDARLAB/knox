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
    
    String displayID;

    @Relationship(type="PRECEDES") 
    Collection<Node> successors;
    
    boolean accept;

    public Node() {
    	
    }
    
    public String getDisplayID() {
    	return displayID;
    }

    public Collection<Node> getSuccessors() {
        return successors;
    }
    
    boolean getAccept() {
    	return accept;
    }
    
}
