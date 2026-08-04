package knox.spring.data.neo4j.domain.dto;

import java.util.List;

public class DesignSpaceLinearDAGRepresentation {
    private List<String> compIDs;
    private List<String> compRoles;
    private List<String> orientation;
    private List<Double> weights;

    //Getters & Setters
    public List<String> getCompIDs() {
        return compIDs;
    }

    public void setCompIDs(List<String> compIDs) {
        this.compIDs = compIDs;
    }

    public List<String> getCompRoles() {
        return compRoles;
    }

    public void setCompRoles(List<String> compRoles) {
        this.compRoles = compRoles;
    }

    public List<String> getOrientation() {
        return orientation;
    }

    public void setOrientation(List<String> orientation) {
        this.orientation = orientation;
    }

    public List<Double> getWeights() {
        return weights;
    }

    public void setWeights(List<Double> weights) {
        this.weights = weights;
    }
}
