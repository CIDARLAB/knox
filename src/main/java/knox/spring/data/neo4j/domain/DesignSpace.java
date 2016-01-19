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
    Set<NodeLink> nodeLinks;
    
    @Relationship(type = "HAS") 
    Set<BranchLink> branchLinks;
    
    @Relationship(type = "SELECTS") 
    HeadLink headLink;

    public DesignSpace() {
    	
    }
    
    public Set<NodeLink> getNodeLinks() {
    	return nodeLinks;
    }
    
    public String getSpaceID() {
    	return spaceID;
    }
    
    public Set<BranchLink> getBranchLinks() {
    	return branchLinks;
    }
    
    public HeadLink getHeadLink() {
    	return headLink;
    }

}
