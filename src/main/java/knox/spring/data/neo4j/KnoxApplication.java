package knox.spring.data.neo4j;

import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.NodeNotFoundException;
import knox.spring.data.neo4j.services.DesignSpaceService;

import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@Configuration
@Import(MyNeo4jConfiguration.class)
@RestController("/")
public class KnoxApplication extends WebMvcConfigurerAdapter {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(KnoxApplication.class, args);
    }

    @Autowired
    DesignSpaceService designSpaceService;
    
    @RequestMapping(value = "/merge/sbol", method = RequestMethod.POST)
    public ResponseEntity<String> mergeSBOL(@RequestParam("inputSBOLFiles[]") List<MultipartFile> inputSBOLFiles,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
    	List<InputStream> inputSBOLStreams = new ArrayList<InputStream>();
    	for (MultipartFile inputSBOLFile : inputSBOLFiles) {
    		if (!inputSBOLFile.isEmpty()) {
    			try {
    				inputSBOLStreams.add(inputSBOLFile.getInputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	
    	try {
			designSpaceService.mergeSBOL(inputSBOLStreams, outputSpaceID);
		} catch (SBOLValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SBOLConversionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    		
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "targetBranchID", required = true) String targetBranchID) {
    	designSpaceService.deleteBranch(targetSpaceID, targetBranchID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch", method = RequestMethod.POST)
    public ResponseEntity<String> createBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "outputBranchID", required = true) String outputBranchID) {
    	designSpaceService.copyHeadBranch(targetSpaceID, outputBranchID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/and", method = RequestMethod.POST)
    public ResponseEntity<String> andBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchID1", required = true) String inputBranchID1,
    		@RequestParam(value = "inputBranchID2", required = true) String inputBranchID2,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID) {
    	designSpaceService.mergeBranches(targetSpaceID, inputBranchID1, inputBranchID2, outputBranchID,
    			true, true);
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully intersected.\"}", 
    				HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/checkout", method = RequestMethod.PUT)
    public ResponseEntity<String> checkoutBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "targetBranchID", required = true) String targetBranchID) {
    	designSpaceService.checkoutBranch(targetSpaceID, targetBranchID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/resetHead", method = RequestMethod.POST)
    public ResponseEntity<String> resetHeadBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "targetCommitID", required = true) String targetCommitID) {
    	try {
    		designSpaceService.resetHeadBranch(targetSpaceID, targetCommitID);
    		return new ResponseEntity<String>("{\"message\": \"Head branch was successfully reset.\"}", 
    				HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/branch/graph/d3", method = RequestMethod.GET)
    public Map<String, Object> d3GraphBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
        return designSpaceService.d3GraphBranches(targetSpaceID);
    }
    
    @RequestMapping(value = "/branch/commitToHead", method = RequestMethod.POST)
    public ResponseEntity<String> commitToHeadBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
    	designSpaceService.commitToHeadBranch(targetSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/join", method = RequestMethod.POST)
    public ResponseEntity<String> joinBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchID1", required = true) String inputBranchID1,
    		@RequestParam(value = "inputBranchID2", required = true) String inputBranchID2,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID) {
    	designSpaceService.joinBranches(targetSpaceID, inputBranchID1, inputBranchID2, outputBranchID);
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully joined.\"}", 
    				HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/merge", method = RequestMethod.POST)
    public ResponseEntity<String> mergeBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchID1", required = true) String inputBranchID1,
    		@RequestParam(value = "inputBranchID2", required = true) String inputBranchID2,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID,
    		@RequestParam(value = "isStrong", required = false) String isStrong) {
    	if (outputBranchID == null) {
    		outputBranchID = inputBranchID1;
		}
    	
    	boolean parsedIsStrong;
    	if (isStrong == null) {
    		parsedIsStrong = true;
    	} else {
    		parsedIsStrong = Boolean.parseBoolean(isStrong);
    	}
    	
    	designSpaceService.mergeBranches(targetSpaceID, inputBranchID1, inputBranchID2, outputBranchID, 
    			false, parsedIsStrong);
    	
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully merged.\"}", 
    				HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/or", method = RequestMethod.POST)
    public ResponseEntity<String> orBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchID1", required = true) String inputBranchID1,
    		@RequestParam(value = "inputBranchID2", required = true) String inputBranchID2,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID) {
    	designSpaceService.orBranches(targetSpaceID, inputBranchID1, inputBranchID2, outputBranchID);
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully disjoined.\"}", 
    				HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
    	try {
    		designSpaceService.deleteDesignSpace(targetSpaceID);
    		return new ResponseEntity<String>("Design space was deleted successfully.", HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/designSpace", method = RequestMethod.POST)
    public ResponseEntity<String> createDesignSpace(@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
    	try {
    		designSpaceService.createDesignSpace(outputSpaceID);
    		return new ResponseEntity<String>("Design space was created successfully.", HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/designSpace/and", method = RequestMethod.POST)
    public ResponseEntity<String> andDesignSpaces(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1, 
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
        if (outputSpaceID == null) {
			outputSpaceID = inputSpaceID1;
		}
    	try {
    		designSpaceService.mergeDesignSpaces(inputSpaceID1, inputSpaceID2, outputSpaceID, true, true);
    		return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully intersected.\"}", 
    				HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException|DesignSpaceConflictException|DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }

    @RequestMapping(value = "/designSpace/graph/d3", method = RequestMethod.GET)
    public Map<String, Object> d3GraphDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
        return designSpaceService.d3GraphDesignSpace(targetSpaceID);
    }
    
    @RequestMapping(value = "/designSpace/insert", method = RequestMethod.POST)
    public ResponseEntity<String> insertDesignSpace(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1,
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "targetNodeID", required = true) String targetNodeID,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID) {
    	if (outputSpaceID == null) {
			outputSpaceID = inputSpaceID1;
		}
    	try {
    		designSpaceService.insertDesignSpace(inputSpaceID1, inputSpaceID2, targetNodeID, outputSpaceID);
    		return new ResponseEntity<String>("{\"message\": \"Design space was successfully inserted.\"}", 
    				HttpStatus.NO_CONTENT);
    	} catch (NodeNotFoundException|DesignSpaceNotFoundException|DesignSpaceConflictException|DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/designSpace/join", method = RequestMethod.POST)
    public ResponseEntity<String> joinDesignSpaces(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1, 
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID) {
    	if (outputSpaceID == null) {
			outputSpaceID = inputSpaceID1;
		}
    	try {
    		designSpaceService.joinDesignSpaces(inputSpaceID1, inputSpaceID2, outputSpaceID);
    		return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully joined.\"}", 
    				HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException|DesignSpaceConflictException|DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/designSpace/merge", method = RequestMethod.POST)
    public ResponseEntity<String> mergeDesignSpaces(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1, 
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
    		@RequestParam(value = "isStrong", required = false) String isStrong) {
    	if (outputSpaceID == null) {
			outputSpaceID = inputSpaceID1;
		}
    	
    	boolean parsedIsStrong;
    	if (isStrong == null) {
    		parsedIsStrong = true;
    	} else {
    		parsedIsStrong = Boolean.parseBoolean(isStrong);
    	}
    	
    	try {
    		designSpaceService.mergeDesignSpaces(inputSpaceID1, inputSpaceID2, outputSpaceID, false, parsedIsStrong);
    		return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully merged.\"}", 
    				HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException|DesignSpaceConflictException|DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/designSpace/minimize", method = RequestMethod.POST)
    public ResponseEntity<String> minimizeDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
    	try {
    		designSpaceService.minimizeDesignSpace(targetSpaceID);
    		return new ResponseEntity<String>("{\"message\": \"Design space was successfully minimized.\"}", 
    				HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
    
    @RequestMapping(value = "/designSpace/or", method = RequestMethod.POST)
    public ResponseEntity<String> orDesignSpaces(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1, 
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID) {
    	if (outputSpaceID == null) {
			outputSpaceID = inputSpaceID1;
		}
    	try {
    		designSpaceService.orDesignSpaces(inputSpaceID1, inputSpaceID2, outputSpaceID);
    		return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully disjoined.\"}", 
    				HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException|DesignSpaceConflictException|DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    	
    	
    }
    
    @RequestMapping(value = "/edge", method = RequestMethod.POST)
    public ResponseEntity<String> createEdge(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "targetTailID", required = true) String targetTailID,
    		@RequestParam(value = "targetHeadID", required = true) String targetHeadID) {
    	try {
    		designSpaceService.createEdge(targetSpaceID, targetTailID, targetHeadID);
    		return new ResponseEntity<String>("Edge was created successfully.", HttpStatus.NO_CONTENT);
    	} catch (DesignSpaceNotFoundException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
}
