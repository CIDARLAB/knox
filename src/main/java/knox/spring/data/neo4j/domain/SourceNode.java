package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class SourceNode {
	
    @GraphId 
    Long id;

    @Relationship(type="PRECEDES") 
    Collection<Node> successors;

    public SourceNode() {
    	
    }

    public Collection<Node> getSuccessors() {
        return successors;
    }
    
}
