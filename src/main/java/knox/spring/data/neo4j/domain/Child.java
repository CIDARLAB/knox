package knox.spring.data.neo4j.domain;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@RelationshipEntity(type = "CONTAINS")
public class Child {
	
    @GraphId
    Long id;
    
    @StartNode
    DesignSpace parent;
    
    @EndNode
    Node child;

    public Child() {
    	
    }

    public DesignSpace getParent() {
        return parent;
    }

    public Node getChild() {
        return child;
    }
    
}
