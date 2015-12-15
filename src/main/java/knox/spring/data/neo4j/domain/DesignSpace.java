package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class DesignSpace {
	
    @GraphId
    Long id;
    
    String displayID;
    
    @Relationship(type="CONTAINS") 
    Collection<Node> children;

    public DesignSpace() {
    	
    }
    
    public Collection<Node> getChildren() {
    	return children;
    }

}
