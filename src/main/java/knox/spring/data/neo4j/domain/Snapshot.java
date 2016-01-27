package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class Snapshot {
	
    @GraphId
    Long id;
    
    String spaceID;
    
    int idIndex;
    
    @Relationship(type = "CONTAINS") 
    Set<SnapToNode> snapToNode;

    public Snapshot() {
    	
    }
    
    public String getSpaceID() {
    	return spaceID;
    }
    
    public int getIDIndex() {
    	return idIndex;
    }
    
    public Set<SnapToNode> getSnapToNode() {
    	return snapToNode;
    }

}
