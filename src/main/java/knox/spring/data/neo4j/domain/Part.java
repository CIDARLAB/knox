package knox.spring.data.neo4j.domain;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class Part {
    @Id
    @GeneratedValue
    private Long id;

    @Property
    private String componentID;

    @Property
    private String componentRole;

    @Property
    private String sequence;

    @Property
    private String description;

    @Property
    private int index;

    @Property
    private List<Double> partData;

    @Relationship(type = "Interaction")
    private Set<Interaction> interactions;

    public Part() {}

    public Part(
            String componentID, 
            String componentRole, 
            String sequence, 
            String description, 
            int index, 
            List<Double> partData
    ) {
        this.componentID = componentID;
        this.componentRole = componentRole;
        this.sequence = sequence;
        this.description = description;
        this.index = index;
        this.partData = partData;
        this.interactions = new HashSet<>();
    }

    public void newInteraction(Part targetPart, String interactionType, List<Double> interactionData) {
        if (this.interactions == null) {
            this.interactions = new HashSet<>();
        }
        this.interactions.add(new Interaction(targetPart, interactionType, interactionData));
    }

    public boolean hasInteractions() {
        return this.interactions != null && !this.interactions.isEmpty();
    }

    // Getters and Setters
    public String getComponentID() {
        return componentID;
    }

    public void setComponentID(String componentID) {
        this.componentID = componentID;
    }

    public String getComponentRole() {
        return componentRole;
    }

    public void setComponentRole(String componentRole) {
        this.componentRole = componentRole;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<Double> getPartData() {
        return partData;
    }

    public void setPartData(List<Double> partData) {
        this.partData = partData;
    }

    public Set<Interaction> getInteractions() {
        return interactions;
    }

    public void setInteractions(Set<Interaction> interactions) {
        this.interactions = interactions;
    }
}
