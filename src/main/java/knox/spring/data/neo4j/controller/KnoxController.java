package knox.spring.data.neo4j.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import knox.spring.data.neo4j.sample.DesignSampler.EnumerateType;
import knox.spring.data.neo4j.eugene.Rule;
import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.NodeNotFoundException;
import knox.spring.data.neo4j.exception.ParameterEmptyException;
import knox.spring.data.neo4j.services.DesignSpaceService;

import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Nicholas Roehner
 */
@RestController("/")
public class KnoxController {
	final DesignSpaceService designSpaceService;
	
	private static final Logger LOG = LoggerFactory.getLogger(KnoxController.class);

	@Autowired
	public KnoxController(DesignSpaceService designSpaceService) {
		this.designSpaceService = designSpaceService;
	}
	
	@RequestMapping(value = "/designSpace/constrain", method = RequestMethod.POST)
    public ResponseEntity<String> constrainDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID,
    		@RequestParam(value = "isClosed", required = false, defaultValue = "false") boolean isClosed,
    		@RequestBody List<Rule> rules) {
        try {
        	long startTime = System.nanoTime();
        	
            designSpaceService.constrainDesignSpace(targetSpaceID, outputSpaceID, isClosed, rules);

            return new ResponseEntity<String>("{\"message\": \"Design spaces was successfully constrained after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
        } catch (ParameterEmptyException | DesignSpaceNotFoundException |
                DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
            return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }
	
	@RequestMapping(value = "/designSpace/join", method = RequestMethod.POST)
    public ResponseEntity<String> joinDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
            @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID) {
        try {
        	long startTime = System.nanoTime();
        	
            if (outputSpaceID == null) {
                designSpaceService.joinDesignSpaces(inputSpaceIDs);
            } else {
                designSpaceService.joinDesignSpaces(inputSpaceIDs, outputSpaceID);
            }

            return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully joined after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
        } catch (ParameterEmptyException | DesignSpaceNotFoundException |
                DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
            return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }
	
	@RequestMapping(value = "/branch/join", method = RequestMethod.POST)
    public ResponseEntity<String> joinBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
            @RequestParam(value = "outputBranchID", required = false) String outputBranchID) {
		long startTime = System.nanoTime();
		
        if (outputBranchID == null) {
            designSpaceService.joinBranches(targetSpaceID, inputBranchIDs);
        } else {
            designSpaceService.joinBranches(targetSpaceID, inputBranchIDs, outputBranchID);
        }

        return new ResponseEntity<String>("{\"message\": \"Branches were successfully joined after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }
	
	@RequestMapping(value = "/designSpace/or", method = RequestMethod.POST)
    public ResponseEntity<String> orDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
            @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
            @RequestParam(value = "isClosed", required = false, defaultValue = "true") boolean isClosed) {
        try {
        	long startTime = System.nanoTime();
        	
            if (outputSpaceID == null) {
                designSpaceService.orDesignSpaces(inputSpaceIDs, isClosed);
            } else {
                designSpaceService.orDesignSpaces(inputSpaceIDs, outputSpaceID, isClosed);
            }

            return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully OR-ed after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
        } catch (ParameterEmptyException | DesignSpaceNotFoundException |
                DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
            return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }
	
	@RequestMapping(value = "/branch/or", method = RequestMethod.POST)
    public ResponseEntity<String> orBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
            @RequestParam(value = "outputBranchID", required = false) String outputBranchID,
            @RequestParam(value = "isClosed", required = false, defaultValue = "true") boolean isClosed) {
        long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
            designSpaceService.orBranches(targetSpaceID, inputBranchIDs, isClosed);
        } else {
            designSpaceService.orBranches(targetSpaceID, inputBranchIDs, outputBranchID, 
            		isClosed);
        }

        return new ResponseEntity<String>("{\"message\": \"Branches were successfully OR-ed after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }
	
	@RequestMapping(value = "/designSpace/repeat", method = RequestMethod.POST)
    public ResponseEntity<String> repeatDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
            @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
            @RequestParam(value = "isOptional", required = false, defaultValue = "false") boolean isOptional) {
        try {
        	long startTime = System.nanoTime();
        	
            if (outputSpaceID == null) {
                designSpaceService.repeatDesignSpaces(inputSpaceIDs, isOptional);
            } else {
                designSpaceService.repeatDesignSpaces(inputSpaceIDs, outputSpaceID, isOptional);
            }

            return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully repeated after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
        } catch (ParameterEmptyException | DesignSpaceNotFoundException |
                DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
            return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }
	
	@RequestMapping(value = "/branch/repeat", method = RequestMethod.POST)
    public ResponseEntity<String> repeatBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
            @RequestParam(value = "outputBranchID", required = false) String outputBranchID,
            @RequestParam(value = "isOptional", required = false, defaultValue = "false") boolean isOptional) {
        long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
            designSpaceService.repeatBranches(targetSpaceID, inputBranchIDs, isOptional);
        } else {
            designSpaceService.repeatBranches(targetSpaceID, inputBranchIDs, outputBranchID, 
            		isOptional);
        }

        return new ResponseEntity<String>("{\"message\": \"Branches were successfully repeated after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }
	
	@RequestMapping(value = "/designSpace/and", method = RequestMethod.POST)
    public ResponseEntity<String> andDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
    		@RequestParam(value = "isComplete", required = false, defaultValue = "true") boolean isComplete,
    		@RequestParam(value = "isClosed", required = false, defaultValue = "false") boolean isClosed,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
    	Set<String> uniqueRoles = new HashSet<String>(roles);
    	
		try {
    		long startTime = System.nanoTime();
    		
    		if (outputSpaceID == null) {
    			designSpaceService.andDesignSpaces(inputSpaceIDs, tolerance, isComplete, 
    					isClosed, uniqueRoles);
    		} else {
    			designSpaceService.andDesignSpaces(inputSpaceIDs, outputSpaceID, tolerance, 
    					isComplete, isClosed, uniqueRoles);
    		}

    		return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully AND-ed after " +
    				(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    	} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
    			DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
	
	@RequestMapping(value = "/branch/and", method = RequestMethod.POST)
    public ResponseEntity<String> andBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
    		@RequestParam(value = "isComplete", required = false, defaultValue = "true") boolean isComplete,
    		@RequestParam(value = "isClosed", required = false, defaultValue = "false") boolean isClosed,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
    		designSpaceService.andBranches(targetSpaceID, inputBranchIDs, tolerance, isComplete,
    				isClosed, uniqueRoles);
		} else {
			designSpaceService.andBranches(targetSpaceID, inputBranchIDs, outputBranchID, tolerance,
					isComplete, isClosed, uniqueRoles);
		}
    	
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully AND-ed after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }
	
	@RequestMapping(value = "/designSpace/merge", method = RequestMethod.POST)
    public ResponseEntity<String> mergeDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "2") int tolerance,
    		@RequestParam(value = "isComplete", required = false, defaultValue = "false") boolean isComplete,
    		@RequestParam(value = "isClosed", required = false, defaultValue = "false") boolean isClosed,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		try {
    		long startTime = System.nanoTime();
    		
    		if (outputSpaceID == null) {
    			designSpaceService.mergeDesignSpaces(inputSpaceIDs, tolerance, isComplete,
    					isClosed, uniqueRoles);
    		} else {
    			designSpaceService.mergeDesignSpaces(inputSpaceIDs, outputSpaceID, tolerance, 
    					isComplete, isClosed, uniqueRoles);
    		}
    		
    		return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully merged after " +
    				(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    	} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
    			DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
	
	@RequestMapping(value = "/branch/merge", method = RequestMethod.POST)
    public ResponseEntity<String> mergeBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "2") int tolerance,
    		@RequestParam(value = "isComplete", required = false, defaultValue = "false") boolean isComplete,
    		@RequestParam(value = "isClosed", required = false, defaultValue = "false") boolean isClosed,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) { 	
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
    		designSpaceService.mergeBranches(targetSpaceID, inputBranchIDs, tolerance, isComplete,
    				isClosed, uniqueRoles); 
    	} else {
    		designSpaceService.mergeBranches(targetSpaceID, inputBranchIDs, outputBranchID, 
    				tolerance, isComplete, isClosed, uniqueRoles);
    	}
    	
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully merged after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }

	@RequestMapping(value = "/import/csv", method = RequestMethod.POST)
    public ResponseEntity<String> importCSV(@RequestParam("inputCSVFiles[]") List<MultipartFile> inputCSVFiles,
    		@RequestParam(value = "outputSpacePrefix", required = true) String outputSpacePrefix) {
    	List<InputStream> inputCSVStreams = new ArrayList<InputStream>();
    	
    	for (MultipartFile inputCSVFile : inputCSVFiles) {
    		if (!inputCSVFile.isEmpty()) {
    			try {
    				inputCSVStreams.add(inputCSVFile.getInputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	
		designSpaceService.importCSV(inputCSVStreams, outputSpacePrefix, true);
		
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/merge/csv", method = RequestMethod.POST)
    public ResponseEntity<String> mergeCSV(@RequestParam("inputCSVFiles[]") List<MultipartFile> inputCSVFiles,
            @RequestParam(value = "outputSpacePrefix", required = true) String outputSpacePrefix) {
        List<InputStream> inputCSVStreams = new ArrayList<InputStream>();

        for (MultipartFile inputCSVFile : inputCSVFiles) {
            if (!inputCSVFile.isEmpty()) {
                try {
                    inputCSVStreams.add(inputCSVFile.getInputStream());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        designSpaceService.importCSV(inputCSVStreams, outputSpacePrefix, true);

        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/import/eugene", method = RequestMethod.POST)
    public ResponseEntity<String> importEugene(@RequestParam("inputCSVFiles[]") List<MultipartFile> inputEugeneFiles) {
        designSpaceService.importEugene();

        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/merge/sbol", method = RequestMethod.POST)
    public ResponseEntity<String> mergeSBOL(@RequestParam("inputSBOLFiles[]") List<MultipartFile> inputSBOLFiles,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID,
    		@RequestParam(value = "authority", required = false) String authority) {
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
			designSpaceService.importSBOL(inputSBOLStreams, outputSpaceID, authority);
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
    
    @RequestMapping(value = "/branch/checkout", method = RequestMethod.PUT)
    public ResponseEntity<String> checkoutBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "targetBranchID", required = true) String targetBranchID) {
        designSpaceService.checkoutBranch(targetSpaceID, targetBranchID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/branch/reset", method = RequestMethod.POST)
    public ResponseEntity<String> resetBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "targetBranchID", required = false) String targetBranchID,
            @RequestParam(value = "commitPath", required = true) List<String> commitPath) {
        if (targetBranchID == null) {
            designSpaceService.resetHeadBranch(targetSpaceID, commitPath);
        } else {
            designSpaceService.resetBranch(targetSpaceID, targetBranchID,
                    commitPath);
        }

        return new ResponseEntity<String>(
                "{\"message\": \"Branch was successfully reset.\"}",
                HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/branch/revert", method = RequestMethod.POST)
    public ResponseEntity<String> revertBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "targetBranchID", required = false) String targetBranchID,
            @RequestParam(value = "commitPath", required = true) List<String> commitPath) {
        if (targetBranchID == null) {
            designSpaceService.revertHeadBranch(targetSpaceID, commitPath);
        } else {
            designSpaceService.revertBranch(targetSpaceID, targetBranchID,
                    commitPath);
        }

        return new ResponseEntity<String>(
                "{\"message\": \"Branch was successfully reverted.\"}",
                HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/branch/graph/d3", method = RequestMethod.GET)
    public Map<String, Object> d3GraphBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
        return designSpaceService.d3GraphBranches(targetSpaceID);
    }

    @RequestMapping(value = "/branch/commitTo", method = RequestMethod.POST)
    public ResponseEntity<String> commitToBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "targetBranchID", required = false) String targetBranchID) {
        if (targetBranchID == null) {
            designSpaceService.commitToHeadBranch(targetSpaceID);
        } else {
            designSpaceService.commitToBranch(targetSpaceID, targetBranchID);
        }
        return new ResponseEntity<String>(
                "Changes to design space were successfully committed.",
                HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/branch/insert", method = RequestMethod.POST)
    public ResponseEntity<String> insertBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "inputBranchID1", required = true) String inputBranchID1,
            @RequestParam(value = "inputBranchID2", required = true) String inputBranchID2,
            @RequestParam(value = "targetNodeID", required = true) String targetNodeID,
            @RequestParam(value = "outputBranchID", required = false) String outputBranchID) {
        designSpaceService.insertBranch(targetSpaceID, inputBranchID1,
                inputBranchID2, targetNodeID,
                outputBranchID);
        return new ResponseEntity<String>(
                "{\"message\": \"Branch was successfully inserted.\"}",
                HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/designSpace", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
        try {
            designSpaceService.deleteDesignSpace(targetSpaceID);
            return new ResponseEntity<String>(
                    "Design space was deleted successfully.",
                    HttpStatus.NO_CONTENT);
        } catch (DesignSpaceNotFoundException ex) {
            return new ResponseEntity<String>(
                    "{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/designSpace", method = RequestMethod.POST)
    public ResponseEntity<String> createDesignSpace(@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID,
            @RequestParam(value = "componentIDs", required = false) List<String> compIDs,
            @RequestParam(value = "componentRoles", required = false) List<String> compRoles) {
        if (compIDs != null && compRoles != null) {
            try {
                designSpaceService.createDesignSpace(outputSpaceID, compIDs,
                        compRoles);
                return new ResponseEntity<String>(
                        "Design space was created successfully.",
                        HttpStatus.NO_CONTENT);
            } catch (DesignSpaceConflictException ex) {
                return new ResponseEntity<String>(
                        "{\"message\": \"" + ex.getMessage() + "\"}",
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            try {
                designSpaceService.createDesignSpace(outputSpaceID);
                return new ResponseEntity<String>(
                        "Design space was created successfully.",
                        HttpStatus.NO_CONTENT);
            } catch (DesignSpaceConflictException ex) {
                return new ResponseEntity<String>(
                        "{\"message\": \"" + ex.getMessage() + "\"}",
                        HttpStatus.BAD_REQUEST);
            }
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
            designSpaceService.insertDesignSpace(inputSpaceID1, inputSpaceID2,
                    targetNodeID, outputSpaceID);
            return new ResponseEntity<String>(
                    "{\"message\": \"Design space was successfully inserted.\"}",
                    HttpStatus.NO_CONTENT);
        } catch (NodeNotFoundException | DesignSpaceNotFoundException |
                DesignSpaceConflictException |
                DesignSpaceBranchesConflictException ex) {
            return new ResponseEntity<String>(
                    "{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }

//    @RequestMapping(value = "/designSpace/match", method = RequestMethod.GET)
//    public Map<String, Object> matchDesignSpaces(@RequestParam(value = "querySpaceIDs", required = true) List<String> querySpaceIDs,
//            @RequestParam(value = "queriedSpaceIDs", required = true) List<String> queriedSpaceIDs) {
//        return designSpaceService.matchDesignSpaces(querySpaceIDs,
//                queriedSpaceIDs);
//    }
    
    @RequestMapping(value = "/designSpace/union", method = RequestMethod.POST)
    public ResponseEntity<String> unionDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
            @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID) {
        try {
            if (outputSpaceID == null) {
                designSpaceService.unionDesignSpaces(inputSpaceIDs);
            } else {
                designSpaceService.unionDesignSpaces(inputSpaceIDs,
                        outputSpaceID);
            }

            return new ResponseEntity<String>(
                    "{\"message\": \"Design spaces were successfully unioned.\"}",
                    HttpStatus.NO_CONTENT);
        } catch (ParameterEmptyException | DesignSpaceNotFoundException |
                DesignSpaceConflictException |
                DesignSpaceBranchesConflictException ex) {
            return new ResponseEntity<String>(
                    "{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }

//    @RequestMapping(value = "/designSpace/minimize", method = RequestMethod.POST)
//    public ResponseEntity<String>
//    minimizeDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
//        try {
//            designSpaceService.minimizeDesignSpace(targetSpaceID);
//
//            return new ResponseEntity<String>(
//                    "{\"message\": \"Design space was successfully minimized.\"}",
//                    HttpStatus.NO_CONTENT);
//        } catch (DesignSpaceNotFoundException ex) {
//            return new ResponseEntity<String>(
//                    "{\"message\": \"" + ex.getMessage() + "\"}",
//                    HttpStatus.BAD_REQUEST);
//        }
//    }

    

//    @RequestMapping(value = "/designSpace/partition",
//            method = RequestMethod.POST)
//    public ResponseEntity<String>
//    partitionDesignSpace(@RequestParam(value = "inputSpaceID",
//            required = true) String inputSpaceID,
//                         @RequestParam(value = "outputSpacePrefix",
//                                 required = true)
//                                 String outputSpacePrefix) {
//        //    	try {
//        designSpaceService.partitionDesignSpace(inputSpaceID,
//                outputSpacePrefix);
//
//        return new ResponseEntity<String>(
//                "{\"message\": \"Design space was successfully partitioned.\"}",
//                HttpStatus.NO_CONTENT);
//        //    	} catch
//        //    (ParameterEmptyException|DesignSpaceNotFoundException|DesignSpaceConflictException|DesignSpaceBranchesConflictException
//        //    ex) {
//        //    		return new ResponseEntity<String>("{\"message\": \"" +
//        //    ex.getMessage() + "\"}",
//        //    				HttpStatus.BAD_REQUEST);
//        //    	}
//    }

    @RequestMapping(value = "/edge", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteEdge(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "targetTailID", required = true) String targetTailID,
            @RequestParam(value = "targetHeadID", required = true) String targetHeadID) {
        designSpaceService.deleteEdge(targetSpaceID, targetTailID,
                targetHeadID);

        return new ResponseEntity<String>("Edge was deleted successfully.",
                HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/edge", method = RequestMethod.POST)
    public ResponseEntity<String> createEdge(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "targetTailID", required = true) String targetTailID,
            @RequestParam(value = "targetHeadID", required = false) String targetHeadID,
            @RequestParam(value = "componentIDs", required = false) List<String> compIDs,
            @RequestParam(value = "componentRoles", required = false) List<String> compRoles) {
        if (targetHeadID == null) {
            targetHeadID = designSpaceService.createNode(targetSpaceID);
        }

        if (compIDs != null && compRoles != null) {
            try {
                designSpaceService.createComponentEdge(
                        targetSpaceID, targetTailID, targetHeadID,
                        new ArrayList<String>(compIDs),
                        new ArrayList<String>(compRoles));
                return new ResponseEntity<String>(
                        "Edge was created successfully.", HttpStatus.NO_CONTENT);
            } catch (DesignSpaceNotFoundException ex) {
                return new ResponseEntity<String>(
                        "{\"message\": \"" + ex.getMessage() + "\"}",
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            try {
                designSpaceService.createEdge(targetSpaceID, targetTailID,
                        targetHeadID);
                return new ResponseEntity<String>(
                        "Edge was created successfully.", HttpStatus.NO_CONTENT);
            } catch (DesignSpaceNotFoundException ex) {
                return new ResponseEntity<String>(
                        "{\"message\": \"" + ex.getMessage() + "\"}",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    @RequestMapping(value = "/node", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteNode(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "targetNodeID", required = true) String targetNodeID) {
        designSpaceService.deleteNode(targetSpaceID, targetNodeID);
        return new ResponseEntity<String>("Node was deleted successfully.",
                HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/designSpace/sample", method = RequestMethod.GET)
    public Set<List<String>> sample(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "numDesigns", required = false, defaultValue = "1") int numDesigns) {
        return designSpaceService.sampleDesignSpace(targetSpaceID, numDesigns);
    }

    @RequestMapping(value = "/designSpace/list", method = RequestMethod.GET)
    public List<String> listDesignSpaces() {
        return designSpaceService.listDesignSpaces();
    }

    @RequestMapping(value = "/designSpace/enumerate", method = RequestMethod.GET)
    public List<List<Map<String, Object>>> enumerate(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "numDesigns", required = false, defaultValue = "0") int numDesigns,
            @RequestParam(value = "maxLength", required = false, defaultValue = "0") int maxLength,
            @RequestParam(value = "bfs", required = true, defaultValue = "true") boolean bfs) {
        EnumerateType enumerateType = bfs ? EnumerateType.BFS : EnumerateType.DFS;  // BFS is default
        
        return designSpaceService.enumerateDesignSpace(targetSpaceID, numDesigns, maxLength, 
        		enumerateType);
    }

//    @RequestMapping(value = "/partition", method = RequestMethod.GET)
//    public Set<List<String>> partition(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
//        return designSpaceService.partitionDesignSpace(targetSpaceID);
//    }
}
