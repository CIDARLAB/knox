package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import knox.spring.data.neo4j.sample.DesignSampler.EnumerateType;
import knox.spring.data.neo4j.services.DesignSpaceService;

import java.util.*;

public class DesignTools {
    
    final DesignSpaceService designSpaceService;

    public DesignTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool(description = "Delete Design Space", returnDirect = true)
    String deleteDesignSpace() {
        System.out.println("\nAI: deleteDesignSpace\n");
        return "Design Space deletion not enabled for safety. User can delete via User Interface, Search by Design Space ID and delete desired space.";
    }

    @Tool(description = "Design Space Information and Details via spaceid", returnDirect = true)
    String getDesignSpaceDetails(@ToolParam(description = "Design Space ID") String spaceID) {
        System.out.println("\nAI: getDesignSpaceDetails\n");

        String context = "Design Space ID: " + spaceID + "<br><br>";

        Map<String, Object> d3 = designSpaceService.d3GraphDesignSpace(spaceID);

        context += "In a design space, edges represent parts with componentIDs (part ids) and componentRoles (roles).<br>"; 
        context += "To get a design, you start at a start node and traverse the graph to an accept node, collecting parts from edges along the way.<br>";
        context += "componentIDs are identifiers or names of parts<br>";
        context += "componentRoles are the roles or types of parts<br>";
        context += "Weights on edges represent the value assigned to a part<br>";
        context += "Orientation describes the direction the part is facing in the design<br>";

        return context + "<br><br>Graph Representation: " + d3.toString();
    }

    @Tool(description = "Some Design Spaces and Number of designs", returnDirect = true)
    String listDesignSpaces() {
        System.out.println("\nAI: listDesignSpaces\n");

        List<String> spaceIDs = designSpaceService.listDesignSpaces();

        Random random = new Random();
        Collections.shuffle(spaceIDs, random);
        List<String> sampleSpaceIDs = spaceIDs.subList(0, 10);

        String context = "Number of Design Spaces: " + spaceIDs.size() + " <br><br> ";
        context += "10 Design Spaces in Neo4j: <br> ";
        
        return context + String.join(", ", sampleSpaceIDs);
    }

    @Tool(description = "Get Best Path", returnDirect = true)
    String getBestPath(@ToolParam(description = "Design Space ID") String spaceID) {
        System.out.println("\nAI: getBestPath\n");

        List<List<Map<String, Object>>> bestPaths = designSpaceService.getBestPath(spaceID);

        List<Map<String, Object>> bestPath = bestPaths.get(0);

        String context = "Best Path in Design Space ID: " + spaceID;

        ArrayList<String> parts = new ArrayList<>();
        String pathScore = new String();

        for (Map<String, Object> part : bestPath) {
            parts.add((String) part.get("id"));
            pathScore = part.get("pathScore").toString();
        }
        context += " Parts: " + parts.toString();
        context += " with a Path Score: " + pathScore;

        return context;
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
