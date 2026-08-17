package knox.spring.data.neo4j.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@RelationshipProperties
public class Interaction {
    @RelationshipId
    @GeneratedValue
    private Long id;

    @TargetNode
    private Part targetPart;

    @Property
    private String interactionType;

    @Property
    private List<Double> interactionData;

    public Interaction() {}

    public Interaction(
            Part targetPart,
            String interactionType,
            List<Double> interactionData
    ) {
        this.targetPart = targetPart;
        this.interactionType = interactionType;
        this.interactionData = interactionData;
    }

    // Getters and Setters
    public Part getTargetPart() {
        return targetPart;
    }

    public void setTargetPart(Part targetPart) {
        this.targetPart = targetPart;
    }

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    public List<Double> getInteractionData() {
        return interactionData;
    }

    public void setInteractionData(List<Double> interactionData) {
        this.interactionData = interactionData;
    }
}
