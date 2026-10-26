package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import org.neo4j.driver.Value;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class Categories {
    @Id @GeneratedValue Long id;
    @Relationship(type = "HAS_SUBCATEGORY") private List<SubCategory> subCategories;

    public Categories() {}

    public Categories(Map<String, Map<String, List<String>>> categoriesMap) {
        this.subCategories = categoriesMap.entrySet().stream()
            .map(entry -> new SubCategory(entry.getKey(), entry.getValue()))
            .toList();
    }

    @SuppressWarnings("unchecked")
    public Categories(JSONObject categoriesJSON) throws JSONException {
        try{
            this.subCategories = categoriesJSON.toMap().entrySet().stream()
                .map(entry -> new SubCategory(entry.getKey(), (Map<String, List<String>>) entry.getValue()))
                .toList();
        } catch (ClassCastException e) {
            throw new JSONException("Invalid JSON structure for categories");
        }
    }

    public void rebuildSubCategories(Map<String, Map<String, List<String>>> categoriesMap) {
        this.subCategories = categoriesMap.entrySet().stream()
            .map(entry -> new SubCategory(entry.getKey(), entry.getValue()))
            .toList();
    }

    public void mergeCategories(List<Categories> otherCategories) {
        if (otherCategories == null || otherCategories.isEmpty()) {
            return;
        }

        List<Map<String, Map<String, List<String>>>> otherSubCategoriesMaps = otherCategories.stream()
            .map(Categories::toMap)
            .toList();

        Map<String, Map<String, List<String>>> currentSubCategoriesMap = this.toMap();

        // Merge Maps, If matching subCategory name exists with different rolesToComponents, rename the new subCategory by appending a suffix
        for (Map<String, Map<String, List<String>>> otherSubCategoriesMap : otherSubCategoriesMaps) {
            for (Map.Entry<String, Map<String, List<String>>> entry : otherSubCategoriesMap.entrySet()) {
                String subCategoryName = entry.getKey();
                Map<String, List<String>> rolesToComponents = entry.getValue();

                if (currentSubCategoriesMap.containsKey(subCategoryName)) {
                    if (!currentSubCategoriesMap.get(subCategoryName).equals(rolesToComponents)) {
                        int suffix = 1;
                        String newSubCategoryName;
                        do {
                            newSubCategoryName = subCategoryName + "_" + suffix++;
                        } while (currentSubCategoriesMap.containsKey(newSubCategoryName));
                        currentSubCategoriesMap.put(newSubCategoryName, rolesToComponents);
                    }
                } else {
                    currentSubCategoriesMap.put(subCategoryName, rolesToComponents);
                }
            }
        }

        rebuildSubCategories(currentSubCategoriesMap);
    }

    public Map<String, Map<String, List<String>>> toMap() {
        if (subCategories == null || subCategories.isEmpty()) {
            return Map.of();
        }

        return subCategories.stream().collect(Collectors.toMap(
                subCategory -> subCategory.getName(),
                subCategory -> subCategory.getRolesToComponents()
            ));
    }

    public List<SubCategory> getSubCategories() {
        return subCategories;
    }
    
    @org.springframework.data.neo4j.core.schema.Node("SubCategory")
    public static class SubCategory {
        @Id @GeneratedValue private Long id;
        @Property public String name;
        @CompositeProperty(prefix = "role") public Map<String, List<String>> rolesToComponents;

        public SubCategory() {}

        public SubCategory(String name, Map<String, List<String>> rolesToComponents) {
            this.name = name;
            this.rolesToComponents = rolesToComponents;
        }

        public String getName() {
            return name;
        }

        public Map<String, List<String>> getRolesToComponents() {
            Map<String, List<String>> copy = new HashMap<>();

            // Convert each value to a string, handling both Neo4j Value and String types
            for (Map.Entry<String, List<String>> entry : rolesToComponents.entrySet()) {
                List<String> values = new ArrayList<>();

                for (Object value : entry.getValue()) {
                    if (value instanceof Value) {
                        values.add(((Value) value).asString());
                    } else if (value instanceof String) {
                        values.add((String) value);
                    } else {
                        throw new IllegalStateException(
                            "Unexpected category value type: " + value.getClass().getName()
                        );
                    }
                }

                copy.put(entry.getKey(), values);
            }
            return copy;
        }
    }
}
