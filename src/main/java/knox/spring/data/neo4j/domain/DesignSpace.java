package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class DesignSpace {
	
    @GraphId
    Long id;
    
    String spaceID;
    
    @Relationship(type = "CONTAINS") 
    Set<Child> children;

    public DesignSpace() {
    	
    }
    
    public Long getID() {
    	return id;
    }
    
    public Set<Child> getChildren() {
    	return children;
    }
    
    public String getSpaceID() {
    	return spaceID;
    }

}
