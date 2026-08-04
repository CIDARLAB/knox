package knox.spring.data.neo4j.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Property;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class DesignGroup {
    @Id
	@GeneratedValue
    Long id;

    @Property
    private String groupID;

    // Load DesignSpaces through Neo4j Queries
    //@Relationship(type = "CONTAINS")
    //private List<DesignSpace> designSpaces;

    public DesignGroup() {}

    public DesignGroup(String groupID) {
        this.groupID = groupID;
    }

    public String getGroupID() {
        return this.groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }
}
