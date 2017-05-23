package knox.spring.data.neo4j.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.GraphId;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Snapshot extends NodeSpace {
    @GraphId Long id;

    public Snapshot() {}

    public Snapshot(int idIndex) { super(idIndex); }
    
    public Snapshot copy() {
    	Snapshot snapCopy = new Snapshot(super.idIndex);
    	
    	snapCopy.copyNodeSpace(this);
    	
    	return snapCopy;
    }
}
