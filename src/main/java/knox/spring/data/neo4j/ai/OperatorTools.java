package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;

import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.ParameterEmptyException;
import knox.spring.data.neo4j.services.DesignSpaceService;

public class OperatorTools {
    
    final DesignSpaceService designSpaceService;

    public OperatorTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool(description = "REVERSE operator: Reverses a design space, input is the design spaceid, output is the reversed design space")
    public String reverseDesignSpace(String spaceId) throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: reverseDesignSpace\n");
        String context = "Reversing Design Space with ID: " + spaceId;

        String outputSpaceID = spaceId + "_reversed";
        designSpaceService.reverseDesignSpace(spaceId, outputSpaceID, "AI_Created", true);

        context += "Design Successfully Reversed, output space id: " + outputSpaceID;

        return context;
    }
    
}
