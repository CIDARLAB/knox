package knox.spring.data.neo4j.domain;

import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class ContextSpace {
    @Id
	@GeneratedValue
    Long id;
    
    @Property 
	private String spaceID;

    @Relationship(type = "INCLUDES")
    private Set<Component> components;

    public ContextSpace() {}

    public ContextSpace(String spaceID) {
        this.spaceID = spaceID;
    }

    public ContextSpace(String spaceID, Set<Component> components) {
        this.spaceID = spaceID;
        this.components = components;
    }

    public ContextSpace(String spaceID, JSONObject categories) {
        this.spaceID = spaceID;

        // Convert JSONObject to Set<Component>
        this.components = new HashSet<>();
        for (String componentName : categories.keySet()) {
            JSONObject rolesMap = categories.getJSONObject(componentName);
            ArrayList<String> componentIDs = new ArrayList<>();
            ArrayList<String> componentRoles = new ArrayList<>();

            // Concrete component IDs 
            for (String role : rolesMap.keySet()) {
                JSONArray idsArray = rolesMap.getJSONArray(role);
                for (int i = 0; i < idsArray.length(); i++) {
                    componentIDs.add(idsArray.getString(i));
                    componentRoles.add(role);
                }
            }

            // Abstract component IDs
            for (String role : rolesMap.keySet()) {
                if (rolesMap.getJSONArray(role).length() == 0) {
                    componentRoles.add(role);
                }
            }

            Component component = new Component(componentName, componentIDs, componentRoles);
            addComponent(component);
        }
    }

    public ContextSpace (String spaceID, ArrayList<ContextSpace> contextSpaces) {
        this.spaceID = spaceID;
        this.components = new HashSet<>();

        ArrayList<String> componentNames = new ArrayList<>();
        for (ContextSpace cs : contextSpaces) {
            for (Component component : cs.getComponents()) {
                if (!componentNames.contains(component.getComponentName())) {
                    addComponent(component);
                    componentNames.add(component.getComponentName());
                }
            }
        }
    }
    
    public void addComponent(Component component) {
        Set<String> componentNames = getComponentNames();
        if (!componentNames.contains(component.getComponentName())) {
            components.add(component);
        }
    }

    private Set<String> getComponentNames() {
        Set<String> names = new HashSet<>();
        for (Component component : components) {
            names.add(component.getComponentName());
        }
        return names;
    }

    public JSONObject toJSON() {
        JSONObject categories = new JSONObject();
        
        for (Component component : components) {
            JSONObject rolesMap = component.toJSON();
            categories.put(component.getComponentName(), rolesMap);
        }

        return categories;
    }

    public String getSpaceID() {
        return spaceID;
    }

    public void setSpaceID(String spaceID) {
        this.spaceID = spaceID;
    }

    public Set<Component> getComponents() {
        return components;
    }
}
