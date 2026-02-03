package knox.spring.data.neo4j.domain;

import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.j2objc.annotations.Property;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class Component {
    @Id
    @GeneratedValue
    private Long id;

    @Property
    private String componentName;

    @Property
    private ArrayList<String> componentIDs;

    @Property
    private ArrayList<String> componentRoles;

    public Component() {}

    public Component(String componentName, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
        this.componentName = componentName;
        this.componentIDs = componentIDs;
        this.componentRoles = componentRoles;
    }

    public JSONObject toJSON() {
        JSONObject rolesMap = new JSONObject();

        for (int i = 0; i < componentRoles.size(); i++) {
            String role = componentRoles.get(i);

            if (!rolesMap.has(role)) {
                rolesMap.put(role, new JSONArray());
            }

            if (i >= componentIDs.size()) {
                continue;
            }

            String id = componentIDs.get(i);
            rolesMap.getJSONArray(role).put(id);
        }

        return rolesMap;
    }

    public String getComponentName() {
        return componentName;
    }

    public ArrayList<String> getComponentIDs() {
        return componentIDs;
    }

    public ArrayList<String> getComponentRoles() {
        return componentRoles;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setComponentIDs(ArrayList<String> componentIDs) {
        this.componentIDs = componentIDs;
    }

    public void setComponentRoles(ArrayList<String> componentRoles) {
        this.componentRoles = componentRoles;
    }
}