package knox.spring.data.neo4j.domain.dto;

import java.util.*;

public class DesignSpaceEdgeDTO {
    private String spaceID;
    private String tailID;
    private List<String> tailTypes;
    private List<String> componentRoles;
    private List<String> componentIDs;
    private List<Double> weight;
    private String orientation;
    private String headID;
    private List<String> headTypes;

    // Getters and setters
    public String getSpaceID() { return spaceID; }
    public void setSpaceID(String spaceID) { this.spaceID = spaceID; }
    
    public String getTailID() { return tailID; }
    public void setTailID(String tailID) { this.tailID = tailID; }
    
    public List<String> getTailTypes() { return tailTypes; }
    public void setTailTypes(List<String> tailTypes) { this.tailTypes = tailTypes; }
    
    public List<String> getComponentRoles() { return componentRoles; }
    public void setComponentRoles(List<String> componentRoles) { this.componentRoles = componentRoles; }
    
    public List<String> getComponentIDs() { return componentIDs; }
    public void setComponentIDs(List<String> componentIDs) { this.componentIDs = componentIDs; }
    
    public List<Double> getWeight() { return weight; }
    public void setWeight(List<Double> weight) { this.weight = weight; }
    
    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }
    
    public String getHeadID() { return headID; }
    public void setHeadID(String headID) { this.headID = headID; }
    
    public List<String> getHeadTypes() { return headTypes; }
    public void setHeadTypes(List<String> headTypes) { this.headTypes = headTypes; }
}
