package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;

import knox.spring.data.neo4j.sample.DesignSampler.EnumerateType;
import knox.spring.data.neo4j.services.DesignSpaceService;

import java.util.*;

public class DesignTools {
    
    final DesignSpaceService designSpaceService;

    public DesignTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool(description = "Delete Design Space")
    String deleteDesignSpace(String spaceID) {
        System.out.println("\nAI: deleteDesignSpace\n");
        return "Design Space deletion not enabled for safety. User can delete via User Interface, Search by Design Space ID and delete desired space.";
    }

    @Tool(description = "Design Space Information Details via spaceid")
    String getDesignSpaceDetails(String spaceID) {
        System.out.println("\nAI: getDesignSpaceDetails\n");

        String context = "Design Space ID: " + spaceID + "\n";

        Map<String, Object> d3 = designSpaceService.d3GraphDesignSpace(spaceID);

        context += "In a design space, edges (links) represent parts with componentIDs (part ids) and componentRoles (roles).\n"; 
        context += "To get a design, you start at a start node and traverse the graph to an accept node, collecting parts from edges along the way.\n";
        context += "componentIDs are identifiers or names of parts";
        context += "componentRoles are the roles or types of parts";
        context += "Weights on edges represent the value assigned to a part";
        context += "Orientation describes the direction the part is facing in the design";

        return context + "\nGraph Representation: " + d3.toString();
    }

    @Tool(description = "Some Design Spaces")
    String listDesignSpaces() {
        System.out.println("\nAI: listDesignSpaces\n");

        List<String> spaceIDs = designSpaceService.listDesignSpaces();

        Random random = new Random();
        Collections.shuffle(spaceIDs, random);
        List<String> sampleSpaceIDs = spaceIDs.subList(0, 5);

        String context = "5 Design Spaces in Neo4j:\n";
        context += "Number of Design Spaces: " + spaceIDs.size() + "\n";
        
        return context + String.join(", ", sampleSpaceIDs);
    }

    /*
    @Tool(description = "Sample or Enumerate Designs from spaceid")
    String sampleDesigns(String spaceID){
        
        System.out.println("\nAI: sampleDesigns\n");

        Collection<List<Map<String, Object>>> designs = designSpaceService.enumerateDesignSpace(
            spaceID, 2, 1, 0, 1, EnumerateType.DFS,
            true, true, false, false);

        String context = "Sampled Designs from Design Space ID: " + spaceID + "\n";

        System.out.println(designs);

        for (List<Map<String, Object>> design : designs) {
            context += "\n\nDesign:\n";
            ArrayList<String> parts = new ArrayList<>();
            ArrayList<String> roles = new ArrayList<>();
            ArrayList<Integer> weights = new ArrayList<>();
            ArrayList<String> orientations = new ArrayList<>();
            for (Map<String, Object> part : design) {
                if (!part.containsKey("isBlank") || (Boolean) part.get("isBlank").equals("true")) {
                    continue;
                }
                parts.add((String) part.get("id"));
                roles.add((String) part.get("roles"));
                weights.add((Integer) part.get("weight"));
                orientations.add((String) part.get("orientation"));
            }
            context += "Parts: " + parts.toString() + "\n";
            context += "Roles: " + roles.toString() + "\n";
            context += "Weights: " + weights.toString() + "\n";
            context += "Orientations: " + orientations.toString() + "\n";
        }

        System.out.println(designs.size() + " designs sampled.");

        System.out.println(context);

        context += "\n\nShow the user the Parts";

        return context;
    }
    */
}
