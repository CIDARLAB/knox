package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import knox.spring.data.neo4j.services.DesignSpaceService;

import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.ParameterEmptyException;

import java.util.*;

public class OperatorTools {
    
    final DesignSpaceService designSpaceService;

    public OperatorTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool(returnDirect = true)
    public String availableOperators() {
        System.out.println("\nAI: AVAILABLE OPERATORS\n");
        return "Available Operators: OR, AND, MERGE, REVERSE, JOIN, REPEAT";
    }

    @Tool(description = "OR operator: Finds union of design spaces.")
    public String orOperator(@ToolParam(description = "List of design space IDs", required = true) List<String> spaceIds, 
            @ToolParam(description = "Output space ID", required = false) String outputSpaceID) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: OR OPERATOR\n");
        String context = "ORing Design Spaces: " + spaceIds.toString();

        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_or" : outputSpaceID;
        designSpaceService.orDesignSpaces(spaceIds, outputSpaceID, "AI_Created");

        context += "Design Successfully ORed, output space id: " + outputSpaceID;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")" + " or use " + outputSpaceID + " in subsequent operators.";
    }

    @Tool(description = "AND operator: Finds complete intersection between design spaces.")
    public String andOperator(@ToolParam(description = "List of design space IDs", required = true) List<String> spaceIds, 
            @ToolParam(description = "Output space ID", required = false) String outputSpaceID,
            @ToolParam(description = "Tolerance (Default to 1)", required = false) Integer tolerance) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: AND OPERATOR\n");
        String context = "ANDing Design Spaces: " + spaceIds.toString();

        tolerance = (tolerance == null || tolerance < 0 || tolerance > 4) ? 1 : tolerance;
        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_and" + tolerance : outputSpaceID;
        designSpaceService.andDesignSpaces(spaceIds, outputSpaceID, "AI_Created", tolerance, true, new HashSet<>());

        context += "Design Successfully ANDed, output space id: " + outputSpaceID;
        context += " with tolerance: " + tolerance;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")" + " or use " + outputSpaceID + " in subsequent operators.";
    }

    @Tool(description = "MERGE operator: finds merge between design spaces.")
    public String mergeOperator(@ToolParam(description = "List of design space IDs", required = true) List<String> spaceIds, 
            @ToolParam(description = "Output space ID", required = false) String outputSpaceID,
            @ToolParam(description = "Tolerance (Default to 0)", required = false) Integer tolerance,
            @ToolParam(description = "Weight Tolerance (Default to 0)", required = false) Integer weightTolerance) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: MERGE OPERATOR\n");
        String context = "Merging Design Spaces: " + spaceIds.toString();

        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_merged" : outputSpaceID;
        tolerance = (tolerance == null || tolerance < 0 || tolerance > 4) ? 0 : tolerance;
        weightTolerance = (weightTolerance == null || weightTolerance < 0 || weightTolerance > 7) ? 0 : weightTolerance;
        designSpaceService.mergeDesignSpaces(spaceIds, outputSpaceID, "AI_Created", tolerance, weightTolerance, new HashSet<>(), new ArrayList<>());

        context += "Design Successfully Merged, output space id: " + outputSpaceID;
        context += " with tolerance: " + tolerance + " and weight tolerance: " + weightTolerance;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")" + " or use " + outputSpaceID + " in subsequent operators.";
    }

    @Tool(description = "REVERSE operator: Reverses a design space.")
    public String reverseOperator(@ToolParam(description = "Design space ID", required = true) String spaceId, 
            @ToolParam(description = "Output space ID", required = false) String outputSpaceID, 
            @ToolParam(description = "keep orientation is false, reverse is true (Default to true)", required = true) Boolean reverseOrientation) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: REVERSE OPERATOR\n");
        String context = "Reversing Design Space with ID: " + spaceId;

        outputSpaceID = outputSpaceID == null ? spaceId + "_reversed" : outputSpaceID;
        designSpaceService.reverseDesignSpace(spaceId, outputSpaceID, "AI_Created", reverseOrientation);

        context += "Design Successfully Reversed, output space id: " + outputSpaceID;
        context += reverseOrientation ? " with orientation reversed." : " with orientation unchanged.";

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")" + " or use " + outputSpaceID + " in subsequent operators.";
    }

    @Tool(description = "JOIN operator: concatenates design spaces.")
    public String joinOperator(@ToolParam(description = "List of design space IDs", required = true) List<String> spaceIds, 
            @ToolParam(description = "Output space ID", required = false) String outputSpaceID) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: JOIN OPERATOR\n");
        String context = "Joining Design Spaces: " + spaceIds.toString();

        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_joined" : outputSpaceID;
        designSpaceService.joinDesignSpaces(spaceIds, outputSpaceID, "AI_Created");

        context += "Design Successfully Joined, output space id: " + outputSpaceID;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")" + " or use " + outputSpaceID + " in subsequent operators.";
    }

    @Tool(description = "REPEAT operator: repeats concatenated design spaces.")
    public String repeatOperator(@ToolParam(description = "List of design space IDs", required = true) List<String> spaceIds, 
            @ToolParam(description = "Output space ID", required = false) String outputSpaceID, 
            @ToolParam(description = "zero-or-more = true, one-or-more = false. (Default to true)", required = false) Boolean isOptional) 
            throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
        
        System.out.println("\nAI: REPEAT OPERATOR\n");
        String context = "Repeating Design Spaces: " + spaceIds.toString();
        
        isOptional = isOptional == null ? true : isOptional;
        String cardinality = isOptional ? "zero-or-more" : "one-or-more";
        outputSpaceID = outputSpaceID == null ? spaceIds.get(0) + "_repeat_" + cardinality : outputSpaceID;
        designSpaceService.repeatDesignSpaces(spaceIds, outputSpaceID, "AI_Created", isOptional);

        context += "Design Successfully Repeated, output space id: " + outputSpaceID;
        context += " with cardinality: " + cardinality;

        return context + "\n\nVISUALIZE_DESIGN_SPACE(" + outputSpaceID + ")" + " or use " + outputSpaceID + " in subsequent operators.";
    }

}
