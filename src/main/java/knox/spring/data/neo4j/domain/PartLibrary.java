package knox.spring.data.neo4j.domain;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Relationship;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class PartLibrary {

    @Id
    @GeneratedValue
    private Long id;

    @Property
    private String partLibraryName;

    @Property
    private List<String> partFeaturesLabels;

    @Property
    private List<String> interactionFeaturesLabels;

    @Property
    private int partIndex;

    @Relationship(type = "HAS_PART")
    private Set<Part> parts;

    @Transient
    private Map<String, Map<String, Object>> compIDMap;


    public PartLibrary() {}

    public PartLibrary(String partLibraryName, List<String> partFeaturesLabels, List<String> interactionFeaturesLabels) {
        this.partLibraryName = partLibraryName;
        this.partFeaturesLabels = partFeaturesLabels;
        this.interactionFeaturesLabels = interactionFeaturesLabels;
        this.partIndex = 0;
    }

    public PartLibrary(
            String partLibraryName, 
            List<String> componentIDs,
            List<String> componentRoles,
            List<String> sequences,
            List<String> descriptions,
            List<String> partFeaturesLabels, 
            List<String> interactionFeaturesLabels
    ) {
        this.partLibraryName = partLibraryName;
        this.partFeaturesLabels = partFeaturesLabels;
        this.interactionFeaturesLabels = interactionFeaturesLabels;
        this.partIndex = 0;

        if (componentIDs != null) {
            for (int i = 0; i < componentIDs.size(); i++) {
                String componentID = componentIDs.get(i);
                String componentRole = componentRoles.get(i);
                String sequence = sequences.get(i);
                String description = descriptions.get(i);
                createPart(componentID, componentRole, sequence, description, new ArrayList<>());
            }
        }
    }

    public void createPart(String componentID, String componentRole, String sequence, String description, List<Double> partData) {
        this.partIndex++;
        addPart(new Part(componentID, componentRole, sequence, description, this.partIndex, partData));
    }

    public void addPart(Part part) {
        if (this.parts == null) {
            this.parts = new HashSet<>();
        }
        this.parts.add(part);
    }

    public void removePart(String componentID) {
        if (this.parts != null) {
            this.parts.removeIf(part -> part.getComponentID().equals(componentID));
        }
    }

    public List<String> getUniqueComponentRoles() {
        if (this.parts == null) {
            return Collections.emptyList();
        }
        return this.parts.stream()
                .map(Part::getComponentRole)
                .distinct()
                .collect(Collectors.toList());
    }

    public Map<String, Map<String, Object>> getCompIDMap() {
        if (this.compIDMap == null) {
            buildCompIDMap();
        }
        return this.compIDMap;
    }

    public void buildCompIDMap() {
        this.compIDMap = new HashMap<>();
        if (this.parts != null) {
            for (Part part : this.parts) {
                String compID = part.getComponentID();
                
                Map<String, Object> compData = new HashMap<>();
                compData.put("componentRole", part.getComponentRole());
                compData.put("componentSequence", part.getSequence());
                compData.put("tokenID", part.getIndex());
                
                this.compIDMap.put(compID, compData);
            }
        }
    }

    public Map<String, Map<String, List<String>>> partLibraryToCategories() {
        Map<String, Map<String, List<String>>> categories = new HashMap<>();

        // Individual components
        for (Part part : this.parts) {
            String compID = part.getComponentID();
            String compRole = part.getComponentRole();

            Map<String, List<String>> compData = new HashMap<>();
            compData.put(compRole, Arrays.asList(compID));

            categories.put(compID, compData);
        }

        // components belonging to the same role can be grouped together
        List<String> uniqueRoles = getUniqueComponentRoles();
        Map<String, List<String>> roleToCompIDsMap = new HashMap<>();
        for (String role : uniqueRoles) {
            List<String> compIDsForRole = new ArrayList<>();

            for (Part part : this.parts) {
                if (part.getComponentRole().equals(role)) {
                    compIDsForRole.add(part.getComponentID());
                }
            }

            if (!compIDsForRole.isEmpty()) {
                roleToCompIDsMap.put(role, compIDsForRole);

                Map<String, List<String>> compData = new HashMap<>();
                compData.put(role, compIDsForRole);

                categories.put(role, compData);
            }
        }

        // Any Part Concrete
        if (!roleToCompIDsMap.isEmpty()) {
            categories.put("any_part_concrete", roleToCompIDsMap);
        }

        return categories;
    }

    public Map<String, Object> getPartLibraryInfo() {
        Map<String, Object> partLibraryInfo = new HashMap<>();
        partLibraryInfo.put("partLibraryName", partLibraryName);

        List<String> componentIDs = new ArrayList<>();
        List<String> componentRoles = new ArrayList<>();
        List<String> sequences = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        partInformation(componentIDs, componentRoles, sequences, descriptions);

        partLibraryInfo.put("componentIDs", componentIDs);
        partLibraryInfo.put("componentRoles", componentRoles);
        partLibraryInfo.put("componentSequences", sequences);
        partLibraryInfo.put("componentDescriptions", descriptions);
        return partLibraryInfo;
    }

    public void partInformation(List<String> componentIDs, List<String> componentRoles, List<String> sequences, List<String> descriptions) {
        if (this.parts == null) {
            return;
        }
        
        for (Part part : this.parts) {
            componentIDs.add(part.getComponentID());
            componentRoles.add(part.getComponentRole());
            sequences.add(part.getSequence());
            descriptions.add(part.getDescription());
        }
    }

    public Map<String, Object> toD3() {
        Map<String, Object> d3Graph = new HashMap<>();

        Map<String, Integer> partToIndex = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        int i = 0;
        for (Part part : this.parts) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", "[" + part.getComponentID() + ", " + part.getComponentRole() + "]");
            node.put("nodeTypes", new ArrayList<>());
            nodes.add(node);
            partToIndex.put(part.getComponentID(), i);
            i++;
        }

        for (Part part : this.parts) {
            if (part.hasInteractions()) {
                for (Interaction interaction : part.getInteractions()) {
                    Map<String, Object> link = new HashMap<>();
                    link.put("source", partToIndex.get(part.getComponentID()));
                    link.put("target", partToIndex.get(interaction.getTargetPart().getComponentID()));
                    link.put("componentRoles", new ArrayList<>());
                    link.put("componentIDs", new ArrayList<>());
                    link.put("weight", new ArrayList<>());
                    links.add(link);
                }
            }
        }

        d3Graph.put("nodes", nodes);
        d3Graph.put("links", links);
        return d3Graph;
    }

    // Getters and setters
    public String getPartLibraryName() {
        return partLibraryName;
    }

    public void setPartLibraryName(String partLibraryName) {
        this.partLibraryName = partLibraryName;
    }

    public Set<Part> getParts() {
        if (this.parts == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(this.parts);
    }

    public void setParts(Set<Part> parts) {
        this.parts = parts;
    }

    public List<String> getPartFeaturesLabels() {
        return partFeaturesLabels;
    }

    public void setPartFeaturesLabels(List<String> partFeaturesLabels) {
        this.partFeaturesLabels = partFeaturesLabels;
    }

    public List<String> getInteractionFeaturesLabels() {
        return interactionFeaturesLabels;
    }

    public void setInteractionFeaturesLabels(List<String> interactionFeaturesLabels) {
        this.interactionFeaturesLabels = interactionFeaturesLabels;
    }

    public int getPartIndex() {
        if (this.parts == null) {
            return 0;
        }
        return this.parts.size();
    }

}
