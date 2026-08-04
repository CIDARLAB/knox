package knox.spring.data.neo4j.domain;
import java.util.*;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.annotation.Transient;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class PartLibrary {

    @Id
    @GeneratedValue
    private Long id;

    @Property
    private String partLibraryName;

    @Property 
    private List<String> componentIDs;

    @Property
    private List<String> componentRoles;

    @Property
    private List<String> componentSequences;

    @Property
    private List<String> componentDescriptions;

    @Property
    private List<String> componentData;

    @Property
    private List<List<Object>> componentInteractionsMatrix;

    @Transient
    private Map<String, Map<String, Object>> compIDMap;


    public PartLibrary() {}

    public PartLibrary(String partLibraryName, List<String> componentIDs, List<String> componentRoles, List<String> componentSequences, List<String> componentDescriptions) {
        this.partLibraryName = partLibraryName;

        // compIDs, compRoles, compSequences, compDescriptions should all have the same length
        this.componentIDs = componentIDs;
        this.componentRoles = componentRoles;
        this.componentSequences = componentSequences;
        this.componentDescriptions = componentDescriptions;
    }

    public String getPartLibraryName() {
        return partLibraryName;
    }

    public List<String> getComponentIDs() {
        return componentIDs;
    }

    public List<String> getComponentRoles() {
        return componentRoles;
    }

    public List<String> getComponentSequences() {
        return componentSequences;
    }

    public List<String> getComponentDescriptions() {
        return componentDescriptions;
    }

    public void setPartLibraryName(String partLibraryName) {
        this.partLibraryName = partLibraryName;
    }

    public void setComponentIDs(List<String> componentIDs) {
        this.componentIDs = componentIDs;
    }

    public void setComponentRoles(List<String> componentRoles) {
        this.componentRoles = componentRoles;
    }

    public void setComponentSequences(List<String> componentSequences) {
        this.componentSequences = componentSequences;
    }

    public void setComponentDescriptions(List<String> componentDescriptions) {
        this.componentDescriptions = componentDescriptions;
    }

    public void addPart(String componentID, String componentRole, String componentSequence, String componentDescription) {
        if (this.componentIDs == null) {
            this.componentIDs = new ArrayList<>();
        }
        if (this.componentRoles == null) {
            this.componentRoles = new ArrayList<>();
        }
        if (this.componentSequences == null) {
            this.componentSequences = new ArrayList<>();
        }
        if (this.componentDescriptions == null) {
            this.componentDescriptions = new ArrayList<>();
        }
        this.componentIDs.add(componentID);
        this.componentRoles.add(componentRole);
        this.componentSequences.add(componentSequence);
        this.componentDescriptions.add(componentDescription);
    }

    public void removePart(String componentID) {
        if (this.componentIDs != null) {
            int index = this.componentIDs.indexOf(componentID);
            if (index != -1) {
                this.componentIDs.remove(index);
                if (this.componentRoles != null && this.componentRoles.size() > index) {
                    this.componentRoles.remove(index);
                }
                if (this.componentSequences != null && this.componentSequences.size() > index) {
                    this.componentSequences.remove(index);
                }
                if (this.componentDescriptions != null && this.componentDescriptions.size() > index) {
                    this.componentDescriptions.remove(index);
                }
            }
        }
    }

    public List<String> getUniqueComponentRoles() {
        if (this.componentRoles == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new HashSet<>(this.componentRoles));
    }

    public void buildCompIDMap() {
        this.compIDMap = new HashMap<>();
        if (this.componentIDs != null) {
            for (int i = 0; i < this.componentIDs.size(); i++) {
                String compID = this.componentIDs.get(i);
                
                Map<String, Object> compData = new HashMap<>();
                compData.put("componentRole", this.componentRoles != null && this.componentRoles.size() > i ? this.componentRoles.get(i) : null);
                compData.put("componentSequence", this.componentSequences != null && this.componentSequences.size() > i ? this.componentSequences.get(i) : null);
                compData.put("tokenID", i+1);
                
                this.compIDMap.put(compID, compData);
            }
        }
    }

    public Map<String, Map<String, Object>> getCompIDMap() {
        return this.compIDMap;
    }

    public Map<String, Object> getPartLibraryInfo() {
        Map<String, Object> partLibraryInfo = new HashMap<>();
        partLibraryInfo.put("partLibraryName", partLibraryName);
        partLibraryInfo.put("componentIDs", componentIDs);
        partLibraryInfo.put("componentRoles", componentRoles);
        partLibraryInfo.put("componentSequences", componentSequences);
        partLibraryInfo.put("componentDescriptions", componentDescriptions);
        return partLibraryInfo;
    }
}
