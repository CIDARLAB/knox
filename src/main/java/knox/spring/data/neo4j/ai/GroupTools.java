package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;

import knox.spring.data.neo4j.services.DesignSpaceService;

import java.util.*;

public class GroupTools {

    final DesignSpaceService designSpaceService;

    public GroupTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool(description = "Delete All Design Spaces by their Group ID")
    String deleteByGroupID(String groupID) {
        System.out.println("\nAI: deleteByGroupID\n");
        return "Group ID deletion not enabled for safety. User can delete via User Interface, Search by Group ID and delete desired group.";
    }

    @Tool(description = "Get All Design Spaces by their Group ID")
    String getUniqueGroupIDs() {
        System.out.println("\nAI: getUniqueGroupIDs\n");

        List<String> groupIDs = designSpaceService.getUniqueGroupIDs();

        System.out.println("Unique Group IDs: " + groupIDs);
        return "Design Space Groups in Neo4j: " + groupIDs.toString();
    }

    @Tool(description = "Get All Design Spaces in a group by their Group ID")
    String getGroupSpaceIDs(String groupID) {
        System.out.println("\nAI: getGroupSpaceIDs\n");

        List<String> designSpaces = designSpaceService.getGroupSpaceIDs(groupID);

        System.out.println("Number of Design Spaces for Group ID " + groupID + ": " + designSpaces.size());
        return designSpaces.subList(0, 50).toString() + " Number of Design Spaces: " + designSpaces.size() + (designSpaces.size() > 50 ? " ... (truncated - First 50 shown)" : "");
    }

}
