package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import knox.spring.data.neo4j.services.DesignSpaceService;

import java.util.*;

public class GroupTools {

    final DesignSpaceService designSpaceService;

    public GroupTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool(description = "Delete Group by ID", returnDirect = true)
    String deleteByGroupID() {
        System.out.println("\nAI: deleteByGroupID\n");
        return "Group ID deletion not enabled for safety. User can delete via User Interface, Search by Group ID and delete desired group.";
    }

    @Tool(description = "Get Unique Group IDs", returnDirect = true)
    String getUniqueGroupIDs() {
        System.out.println("\nAI: getUniqueGroupIDs\n");

        List<String> groupIDs = designSpaceService.getUniqueGroupIDs();

        System.out.println("Unique Group IDs: " + groupIDs);
        return "Design Space Groups in Neo4j:<br><br>" + String.join("<br>", groupIDs);
    }

    @Tool(description = "Get All Design Spaces in a group by their Group ID", returnDirect = true)
    String getGroupSpaceIDs(@ToolParam(description = "Group ID") String groupID) {
        System.out.println("\nAI: getGroupSpaceIDs\n");

        List<String> designSpaces = designSpaceService.getGroupSpaceIDs(groupID);

        System.out.println("Number of Design Spaces for Group ID " + groupID + ": " + designSpaces.size());
        return designSpaces.subList(0, 50).toString() + " Number of Design Spaces: " + designSpaces.size() + (designSpaces.size() > 50 ? " ... (truncated - First 50 shown)" : "");
    }

}
