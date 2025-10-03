package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;

import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.ParameterEmptyException;
import knox.spring.data.neo4j.services.DesignSpaceService;

import java.util.*;

public class OperatorTools {
    
    final DesignSpaceService designSpaceService;

    public OperatorTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool
    public String availableOperators() {
        System.out.println("\nAI: AVAILABLE OPERATORS\n");
        return "Available Operators: OR, AND, MERGE, REVERSE, JOIN, REPEAT";
    }

    @Tool(
        description = "OR operator: Finds complete intersection between design spaces\n"
        + "required input is List of design spaceIDs (never pass null)\n"
        + "optional input is the outputSpaceID (pass null if not specified)\n"
        + "output is the union design space"
    )
    public String orDesignSpaces(List<String> spaceIds, String outputSpaceID) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: OR OPERATOR\n");
        String context = "ORing Design Spaces: " + spaceIds.toString();

        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_or" : outputSpaceID;
        designSpaceService.orDesignSpaces(spaceIds, outputSpaceID, "AI_Created");

        context += "Design Successfully ORed, output space id: " + outputSpaceID;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")";
    }

    @Tool(
        description = "AND operator: Finds complete intersection between design spaces\n"
        + "required input is List of design spaceIDs (never pass null)\n"
        + "optional input is the outputSpaceID (pass null if not specified)\n"
        + "Default tolerance is 1\n"
        + "output is the intersected design space"
    )
    public String andDesignSpaces(List<String> spaceIds, String outputSpaceID) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: AND OPERATOR\n");
        String context = "ANDing Design Spaces: " + spaceIds.toString();

        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_and1" : outputSpaceID;
        designSpaceService.andDesignSpaces(spaceIds, outputSpaceID, "AI_Created", 1, true, new HashSet<>());

        context += "Design Successfully ANDed, output space id: " + outputSpaceID;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")";
    }

    @Tool(
        description = "MERGE operator: merges design spaces\n"
        + "required input is List of design spaceIDs (never pass null)\n"
        + "optional input is the outputSpaceID (pass null if not specified)\n"
        + "Default tolerance is 1\n"
        + "Default weight tolerance is 0\n"
        + "output is the merged design space"
    )
    public String mergeDesignSpaces(List<String> spaceIds, String outputSpaceID) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: MERGE OPERATOR\n");
        String context = "Merging Design Spaces: " + spaceIds.toString();

        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_merged" : outputSpaceID;
        designSpaceService.mergeDesignSpaces(spaceIds, outputSpaceID, "AI_Created", 0, 0, new HashSet<>(), new ArrayList<>());

        context += "Design Successfully Merged, output space id: " + outputSpaceID;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")";
    }

    @Tool(
        description = "REVERSE operator: Reverses a design space\n"
        + "required input is the design's spaceid (never pass null)\n"
        + "optional input is the outputSpaceID (pass null if not specified)\n"
        + "output is the reversed design space"
    )
    public String reverseDesignSpace(String spaceId, String outputSpaceID) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: REVERSE OPERATOR\n");
        String context = "Reversing Design Space with ID: " + spaceId;

        outputSpaceID = outputSpaceID == null ? spaceId + "_reversed" : outputSpaceID;
        designSpaceService.reverseDesignSpace(spaceId, outputSpaceID, "AI_Created", true);

        context += "Design Successfully Reversed, output space id: " + outputSpaceID;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")";
    }

    @Tool(
        description = "JOIN operator: concatenates design spaces\n"
        + "required input is List of design spaceIDs (never pass null)\n"
        + "optional input is the outputSpaceID (pass null if not specified)\n"
        + "output is the joined design space"
    )
    public String joinDesignSpaces(List<String> spaceIds, String outputSpaceID) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: JOIN OPERATOR\n");
        String context = "Joining Design Spaces: " + spaceIds.toString();

        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_joined" : outputSpaceID;
        designSpaceService.joinDesignSpaces(spaceIds, outputSpaceID, "AI_Created");

        context += "Design Successfully Joined, output space id: " + outputSpaceID;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")";
    }

    @Tool(
        description = "REPEAT operator: repeats design spaces\n"
        + "required input is List of design spaceIDs (never pass null)\n"
        + "optional input is the outputSpaceID (pass null if not specified)\n"
        + "optional input is the isOptional (Default to true if zero-or-more or not specified, else false if one-or-more)\n"
        + "output is the repeated design space"
    )
    public String repeatDesignSpaces(List<String> spaceIds, String outputSpaceID, Boolean isOptional) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: REPEAT OPERATOR\n");
        String context = "Repeating Design Spaces: " + spaceIds.toString();
        
        isOptional = isOptional == null ? true : isOptional;
        String cardinality = isOptional ? "zero-or-more" : "one-or-more";
        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_repeat" + cardinality : outputSpaceID;
        designSpaceService.repeatDesignSpaces(spaceIds, outputSpaceID, "AI_Created", isOptional);

        context += "Design Successfully Repeated, output space id: " + outputSpaceID;
        context += " with cardinality: " + cardinality;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")";
    }

}
