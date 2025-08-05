package knox.spring.data.neo4j.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

import javax.sql.rowset.serial.SerialBlob;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.sql.Blob;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.exception.*;
import knox.spring.data.neo4j.sample.DesignSampler.EnumerateType;
import knox.spring.data.neo4j.sbol.SBOLConversion;
import knox.spring.data.neo4j.sbol.SBOLGeneration;
import knox.spring.data.neo4j.services.DesignSpaceService;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;

import org.jdom.Document;
import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SBOLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.neo4j.ogm.json.JSONException;
import org.neo4j.ogm.json.JSONObject;

import javassist.bytecode.ByteArray;

/**
 * @author Nicholas Roehner
 */
@RestController("/")
public class KnoxController {
	final DesignSpaceService designSpaceService;

	@Autowired DesignSpaceRepository designSpaceRepository;
	
	private static final Logger LOG = LoggerFactory.getLogger(KnoxController.class);

	@Autowired
	public KnoxController(DesignSpaceService designSpaceService) {
		this.designSpaceService = designSpaceService;
	}
	
	/**
	 * @api {post} /branch/and AND
	 * @apiName andBranches
	 * @apiGroup Branch
	 * 
	 * @apiParam {String[]} targetSpaceID ID for the target design space containing the input branches to be AND-ed.
	 * @apiParam {String[]} inputSpaceIDs IDs for the input branches to be AND-ed.
	 * @apiParam {String} [outputSpaceID] ID for the output branch resulting from AND. If omitted, then the result is stored in 
	 * the first input branch.
	 * @apiParam {Integer=0,1,2,3,4} tolerance=1 This parameter determines the criteria by which edges are matched. If tolerance
	 * = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges 
	 * must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component 
	 * roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be 
	 * labeled with the same orientation. If tolerance <= 1, then labels on matching edges are intersected; otherwise, they are 
	 * unioned.
	 * @apiParam {Boolean} isComplete=true If true, then only edges belonging to paths for designs common to all input design 
	 * spaces are retained.
	 * @apiParam {String[]} [roles] If specified, then only edges labeled with at least one of these roles will be AND-ed.
	 * 
	 * @apiDescription Intersects designs from input branches. Based on tensor product of graphs.
	 */
	
	@RequestMapping(value = "/branch/and", method = RequestMethod.POST)
    public ResponseEntity<String> andBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
    		@RequestParam(value = "isComplete", required = false, defaultValue = "true") boolean isComplete,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
    		designSpaceService.andBranches(targetSpaceID, inputBranchIDs, tolerance, isComplete,
    				uniqueRoles);
		} else {
			designSpaceService.andBranches(targetSpaceID, inputBranchIDs, outputBranchID, tolerance,
					isComplete, uniqueRoles);
		}
    	
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully AND-ed after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }
	
	/**
	 * @api {post} /branch Checkout
	 * @apiName checkoutBranch
	 * @apiGroup Branch
	 * 
	 * @apiParam {String} targetSpaceID ID for the target design space.
	 * @apiParam {String} targetBranchID ID for the target branch. 
	 * 
	 * @apiDescription Checks out the target branch as the new head, copying its latest committed snapshot to the contents of the
	 * target design space.
	 */
	
	@RequestMapping(value = "/branch/checkout", method = RequestMethod.POST)
	public ResponseEntity<String> checkoutBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
	        @RequestParam(value = "targetBranchID", required = true) String targetBranchID) {
		long startTime = System.nanoTime();
		
	    designSpaceService.checkoutBranch(targetSpaceID, targetBranchID);
	    
	    return new ResponseEntity<String>("{\"message\": \"Branch was successfully checked out after " + 
        		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	}
	
	/**
	 * @api {post} /branch CommitTo
	 * @apiName commitToBranch
	 * @apiGroup Branch
	 * 
	 * @apiParam {String} targetSpaceID ID for the target design space.
	 * @apiParam {String} targetBranchID ID for the target branch. 
	 * 
	 * @apiDescription Commits a snapshot of the target design space to the target branch.
	 */

	@RequestMapping(value = "/branch/commitTo", method = RequestMethod.POST)
	public ResponseEntity<String> commitToBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
	        @RequestParam(value = "targetBranchID", required = false) String targetBranchID) {
		long startTime = System.nanoTime();
		
	    if (targetBranchID == null) {
	        designSpaceService.commitToHeadBranch(targetSpaceID);
	    } else {
	        designSpaceService.commitToBranch(targetSpaceID, targetBranchID);
	    }
	    return new ResponseEntity<String>("{\"message\": \"Changes to design space were successfully committed after " + 
        		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	}

	/**
	 * @api {post} /branch Create
	 * @apiName createBranch
	 * @apiGroup Branch
	 * 
	 * @apiParam {String} targetSpaceID ID for the target design space.
	 * @apiParam {String} outputBranchID ID for the output branch. 
	 * 
	 * @apiDescription Creates a new branch that is a copy of the current head branch.
	 */
	
	@RequestMapping(value = "/branch", method = RequestMethod.POST)
	public ResponseEntity<String> createBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
	        @RequestParam(value = "outputBranchID", required = true) String outputBranchID) {
		long startTime = System.nanoTime();
		
	    designSpaceService.copyHeadBranch(targetSpaceID, outputBranchID);
	    
	    return new ResponseEntity<String>("{\"message\": \"Branch was successfully created after " + 
	    		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	}

	/**
	 * @api {delete} /branch Delete
	 * @apiName deleteBranch
	 * @apiGroup Branch
	 * 
	 * @apiParam {String} targetSpaceID ID for the target design space.
	 * @apiParam {String} targetBranchID ID for the target branch. 
	 * 
	 * @apiDescription Deletes the target branch.
	 */
	
	@RequestMapping(value = "/branch", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
	        @RequestParam(value = "targetBranchID", required = true) String targetBranchID) {
		long startTime = System.nanoTime();
		
	    designSpaceService.deleteBranch(targetSpaceID, targetBranchID);
	
	    return new ResponseEntity<String>("{\"message\": \"Branch was successfully deleted after " + 
	    		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	}

	/**
	 * @api {post} /branch/join Join
	 * @apiName joinBranches
	 * @apiGroup Branch
	 * 
	 * @apiParam {String[]} targetSpaceID ID for the target design space containing the input branches to be joined.
	 * @apiParam {String[]} inputBranchIDs IDs for the input branches to be joined.
	 * @apiParam {String} [outputBranchID] ID for the output branch resulting from Join. If omitted, then the result is stored in 
	 * the first input branch.
	 * 
	 * @apiDescription Concatenates designs from input branches.
	 */
	
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
	
	/**
	 * @api {post} /branch/merge Merge
	 * @apiName mergeBranches
	 * @apiGroup Branch
	 * 
	 * @apiParam {String[]} targetSpaceID ID for the target design space containing the branches to be merged.
	 * @apiParam {String[]} inputSpaceIDs IDs for the input branches to be merged.
	 * @apiParam {String} [outputSpaceID] ID for the output branch resulting from Merge. If omitted, then the result is stored in 
	 * the first input branch.
	 * @apiParam {Integer=0,1,2,3,4} tolerance=1 This parameter determines the criteria by which edges are matched. If tolerance
	 * = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges 
	 * must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component 
	 * roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be 
	 * labeled with the same orientation. If tolerance <= 1, then labels on matching edges are intersected; otherwise, they are 
	 * unioned.
	 * @apiParam {Integer=0,1,2} weightTolerance=0 This parameter determines the criteria by which how to combine the weights of matched edges. 
	 * If weightTolerance = 0, the weights are summed. 
	 * If weightTolerance = 1, the weights are summed if their edges are the same distance to Start or Accept Node, otherwise they are averaged. 
	 * If weightTolerance = 2, the weights are summed if their edges are next to the same part, otherwise they are averaged.
	 * @apiParam {String[]} [roles] If specified, then only edges labeled with at least one of these roles will be merged.
	 * 
	 * @apiDescription Merges designs from input design spaces. Based on strong product of graphs.
	 */
	
	@RequestMapping(value = "/branch/merge", method = RequestMethod.POST)
    public ResponseEntity<String> mergeBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "2") int tolerance,
			@RequestParam(value = "weightTolerance", required = false, defaultValue = "0") int weightTolerance,
    		@RequestParam(value = "isComplete", required = false, defaultValue = "false") boolean isComplete,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) { 	
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
    		designSpaceService.mergeBranches(targetSpaceID, inputBranchIDs, tolerance, weightTolerance, uniqueRoles); 
    	} else {
    		designSpaceService.mergeBranches(targetSpaceID, inputBranchIDs, outputBranchID, weightTolerance, tolerance, 
    				uniqueRoles);
    	}
    	
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully merged after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }
	
	/**
	 * @api {post} /branch/or OR
	 * @apiName orBranches
	 * @apiGroup Branch
	 * 
	 * @apiParam {String[]} targetSpaceID ID for the target design space containing the input branches to be OR-ed.
	 * @apiParam {String[]} inputBranchIDs IDs for the input branches to be OR-ed.
	 * @apiParam {String} [outputBranchID] ID for the output branch resulting from OR. If omitted, then the result is stored in the 
	 * first input branch.
	 * 
	 * @apiDescription Unions designs from input branches.
	 */
	
	@RequestMapping(value = "/branch/or", method = RequestMethod.POST)
    public ResponseEntity<String> orBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
            @RequestParam(value = "outputBranchID", required = false) String outputBranchID) {
        long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
            designSpaceService.orBranches(targetSpaceID, inputBranchIDs);
        } else {
            designSpaceService.orBranches(targetSpaceID, inputBranchIDs, outputBranchID);
        }

        return new ResponseEntity<String>("{\"message\": \"Branches were successfully OR-ed after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    }
	
	/**
	 * @api {post} /branch/repeat Repeat
	 * @apiName repeatBranches
	 * @apiGroup Branch
	 * 
	 * @apiParam {String[]} targetSpaceID ID for the target design space containing the input branches to be repeated.
	 * @apiParam {String[]} inputBranchIDs IDs for the input branches to be repeated.
	 * @apiParam {String} [outputBranchID] ID for the output branch resulting from Repeat. If omitted, then the result is stored 
	 * in the first input branch.
	 * @apiParam {Boolean} isOptional=false If true, then designs from the input branches are repeated zero-or-more times;
	 * otherwise, they are repeated one-or-more times.
	 * 
	 * @apiDescription Concatenates and then repeats designs from input branches either zero-or-more or one-or-more times.
	 */
	
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
	
	/**
	 * @api {post} /branch Reset
	 * @apiName resetBranch
	 * @apiGroup Branch
	 * 
	 * @apiParam {String} targetSpaceID ID for the target design space.
	 * @apiParam {String} [targetBranchID] ID for the target branch. If omitted, the head branch is reset.
	 * @apiParam {String[]} commitPath List of commit IDs beginning with latest commit and ending with the commit to
	 * which the target branch is to be reset. 
	 * 
	 * @apiDescription Resets the target branch to a previously committed snapshot. No record of the reset is
	 * preserved.
	 */
	
	@RequestMapping(value = "/branch/reset", method = RequestMethod.POST)
	public ResponseEntity<String> resetBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
	        @RequestParam(value = "targetBranchID", required = false) String targetBranchID,
	        @RequestParam(value = "commitPath", required = true) List<String> commitPath) {
		long startTime = System.nanoTime();
		
	    if (targetBranchID == null) {
	        designSpaceService.resetHeadBranch(targetSpaceID, commitPath);
	    } else {
	        designSpaceService.resetBranch(targetSpaceID, targetBranchID,
	                commitPath);
	    }
	
	    return new ResponseEntity<String>("{\"message\": \"Branch was successfully reset after " + 
	            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	}
	
	/**
	 * @api {post} /branch Revert
	 * @apiName revertBranch
	 * @apiGroup Branch
	 * 
	 * @apiParam {String} targetSpaceID ID for the target design space.
	 * @apiParam {String} [targetBranchID] ID for the target branch. If omitted, the head branch is reverted.
	 * @apiParam {String[]} commitPath List of commit IDs beginning with latest commit and ending with the commit to
	 * which the target branch is to be reverted.
	 * 
	 * @apiDescription Reverts the target branch to a previously committed snapshot by copying the contents of the
	 * latter to a new latest commit on the target branch. This preserves a record of the reversion.
	 */

	@RequestMapping(value = "/branch/revert", method = RequestMethod.POST)
	public ResponseEntity<String> revertBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
	        @RequestParam(value = "targetBranchID", required = false) String targetBranchID,
	        @RequestParam(value = "commitPath", required = true) List<String> commitPath) {
		long startTime = System.nanoTime();
		
	    if (targetBranchID == null) {
	        designSpaceService.revertHeadBranch(targetSpaceID, commitPath);
	    } else {
	        designSpaceService.revertBranch(targetSpaceID, targetBranchID,
	                commitPath);
	    }
	
	    return new ResponseEntity<String>("{\"message\": \"Branch was successfully reverted after " + 
        		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	}

	/**
	 * @api {post} /designSpace/and AND
	 * @apiName andDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} inputSpaceIDs IDs for the input design spaces to be AND-ed.
	 * @apiParam {String} [outputSpaceID] ID for the output design space resulting from AND. If omitted, then the result is stored 
	 * in the first input design space.
	 * @apiParam {String} [groupID] ID for the output design space's group. If omitted, groupID is "none".
	 * @apiParam {Integer=0,1,2,3,4} tolerance=1 This parameter determines the criteria by which edges are matched. If tolerance
	 * = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges 
	 * must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component 
	 * roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be 
	 * labeled with the same orientation. If tolerance <= 1, then labels on matching edges are intersected; otherwise, they are 
	 * unioned.
	 * @apiParam {Boolean} isComplete=true If true, then only the matching edges that belong to paths for designs common to all 
	 * input design spaces are retained.
	 * @apiParam {String[]} [roles] If specified, then only edges labeled with at least one of these roles will be AND-ed.
	 * 
	 * @apiDescription Intersects designs from input design spaces. Based on tensor product of graphs.
	 */
	
	@RequestMapping(value = "/designSpace/and", method = RequestMethod.POST)
	public ResponseEntity<String> andDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
			@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
			@RequestParam(value = "isComplete", required = false, defaultValue = "true") boolean isComplete,
			@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		try {
			long startTime = System.nanoTime();
			
			if (outputSpaceID == null) {
				designSpaceService.andDesignSpaces(inputSpaceIDs, groupID, tolerance, isComplete, uniqueRoles);
			} else {
				designSpaceService.andDesignSpaces(inputSpaceIDs, outputSpaceID, groupID, tolerance, 
						isComplete, uniqueRoles);
			}
	
			return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully AND-ed after " +
					(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
		} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
				DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
			return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
					HttpStatus.BAD_REQUEST);
		}
	}
	
	/**
	 * @api {post} /designSpace/delete Delete
	 * @apiName deletDesignSpace
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String} targetSpaceID ID for the target design space to be deleted.
	 * 
	 * @apiDescription Deletes the target design space.
	 */

	@RequestMapping(value = "/designSpace", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
	    try {
	    	long startTime = System.nanoTime();
	    	
	        designSpaceService.deleteDesignSpace(targetSpaceID);
	        
	        return new ResponseEntity<String>("{\"message\": \"Design space was deleted successfully after " +
					(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	    } catch (DesignSpaceNotFoundException ex) {
	        return new ResponseEntity<String>(
	                "{\"message\": \"" + ex.getMessage() + "\"}",
	                HttpStatus.BAD_REQUEST);
	    }
	}

	/**
	 * @api {post} /designSpace/join Join
	 * @apiName joinDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} inputSpaceIDs IDs for the input design spaces to be joined.
	 * @apiParam {String} [outputSpaceID] ID for the output design space resulting from Join. If omitted, then the result is stored 
	 * @apiParam {String} [groupID] ID for the output design space's group. If omitted, groupID is "none".
	 * in the first input design space.
	 * 
	 * @apiDescription Concatenates designs from input design spaces.
	 */
	
	@RequestMapping(value = "/designSpace/join", method = RequestMethod.POST)
	public ResponseEntity<String> joinDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
	        @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID) {
	    try {
	    	long startTime = System.nanoTime();
	    	
	        if (outputSpaceID == null) {
	            designSpaceService.joinDesignSpaces(inputSpaceIDs, groupID);
	        } else {
	            designSpaceService.joinDesignSpaces(inputSpaceIDs, outputSpaceID, groupID);
	        }
	
	        return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully joined after " + 
	        		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	    } catch (ParameterEmptyException | DesignSpaceNotFoundException |
	            DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
	        return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
	                HttpStatus.BAD_REQUEST);
	    }
	}

	/**
	 * @api {post} /designSpace/merge Merge
	 * @apiName mergeDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} inputSpaceIDs IDs for the input design spaces to be merged.
	 * @apiParam {String} [outputSpaceID] ID for the output design space resulting from Merge.  If omitted, then the result is
	 *  stored in the first input design space.
	 * @apiParam {String} [groupID] ID for the output design space's group. If omitted, groupID is "none".
	 * @apiParam {Integer=0,1,2,3,4} tolerance=1 This parameter determines the criteria by which edges are matched. If tolerance
	 * = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges 
	 * must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component 
	 * roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be 
	 * labeled with the same orientation. If tolerance <= 1, then labels on matching edges are intersected; otherwise, they are 
	 * unioned.
	 * @apiParam {Integer=0,1,2} weightTolerance=0 This parameter determines the criteria by which how to combine the weights of matched edges. 
	 * If weightTolerance = 0, the weights are summed. 
	 * If weightTolerance = 1, the weights are summed if their edges are the same distance to Start or Accept Node, otherwise they are averaged. 
	 * If weightTolerance = 2, the weights are summed if their edges are next to the same part, otherwise they are averaged.
	 * @apiParam {String[]} [roles] If specified, then only edges labeled with at least one of these roles will be AND-ed.
	 * 
	 * @apiDescription Merges designs from input design spaces. Based on strong product of graphs.
	 */
	
	@RequestMapping(value = "/designSpace/merge", method = RequestMethod.POST)
	public ResponseEntity<String> mergeDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
			@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
			@RequestParam(value = "weightTolerance", required = false, defaultValue = "0") int weightTolerance,
			@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles,
			@RequestParam(value = "irrelevantParts", required = false, defaultValue = "") ArrayList<String> irrelevantParts) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		try {
			long startTime = System.nanoTime();
			
			if (outputSpaceID == null) {
				designSpaceService.mergeDesignSpaces(inputSpaceIDs, groupID, tolerance, weightTolerance, uniqueRoles, irrelevantParts);
			} else {
				designSpaceService.mergeDesignSpaces(inputSpaceIDs, outputSpaceID, groupID, tolerance, weightTolerance, uniqueRoles, irrelevantParts);
			}
			
			return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully merged after " +
					(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
		} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
				DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
			return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
					HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/designSpace/weight", method = RequestMethod.POST)
	public ResponseEntity<String> weightDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
			@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "tolerance", required = false, defaultValue = "0") int tolerance,
			@RequestParam(value = "weightTolerance", required = false, defaultValue = "0") int weightTolerance) {
		
		try {
			long startTime = System.nanoTime();
			
			
			designSpaceService.weightDesignSpaces(inputSpaceIDs, outputSpaceID, groupID, tolerance, weightTolerance);
			
			
			return new ResponseEntity<String>("{\"message\": \"Design space was successfully weighted after " +
					(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
		} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
				DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
			return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
					HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/designSpace/reverse", method = RequestMethod.POST)
	public ResponseEntity<String> reverseDesignSpaces(@RequestParam(value = "inputSpaceID", required = true) String inputSpaceID,
			@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "reverseOrientation", required = false, defaultValue = "true") Boolean reverseOrientation) {
		
		try {
			long startTime = System.nanoTime();
			
			
			designSpaceService.reverseDesignSpace(inputSpaceID, outputSpaceID, groupID, reverseOrientation);
			
			
			return new ResponseEntity<String>("{\"message\": \"Design space was successfully reversed after " +
					(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
		} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
				DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
			return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * @api {post} /designSpace/merge Merge
	 * @apiName mergeDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} mergeSpaceIDs IDs for the input design spaces to be merged.
	 * @apiParam {String[]} andSpaceIDs IDs for the input design spaces to be anded with output of merge.
	 * @apiParam {String} [mergeOutputSpaceID] ID for the output design space resulting from Merge. 
	 * @apiParam {String} [andOutputSpaceID] ID for the output design space resulting from And.
	 * @apiParam {Integer=0,1,2,3,4} mergeTolerance=1 This parameter determines the criteria by which edges are matched. If mergeTolerance
	 * = 0, then matching edges must be labeled with the same component IDs and roles. If mergeTolerance = 1 or 2, then matching edges 
	 * must share at least one component ID and role. If mergeTolerance = 3, then matching edges must be labeled with the same component 
	 * roles. If mergeTolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be 
	 * labeled with the same orientation. If mergeTolerance <= 1, then labels on matching edges are intersected; otherwise, they are 
	 * unioned.
	 * @apiParam {Integer=0,1,2,3,4} andTolerance=1 This parameter determines the criteria by which edges are matched. If AndTolerance
	 * = 0, then matching edges must be labeled with the same component IDs and roles. If andTolerance = 1 or 2, then matching edges 
	 * must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component 
	 * roles. If andTolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be 
	 * labeled with the same orientation. If andTolerance <= 1, then labels on matching edges are intersected; otherwise, they are 
	 * unioned.
	 * @apiParam {Integer=0,1,2} weightTolerance=0 This parameter determines the criteria by which how to combine the weights of matched edges. 
	 * If weightTolerance = 0, the weights are summed. 
	 * If weightTolerance = 1, the weights are summed if their edges are the same distance to Start or Accept Node, otherwise they are averaged. 
	 * If weightTolerance = 2, the weights are summed if their edges are next to the same part, otherwise they are averaged.
	 * @apiParam {Boolean} isComplete=true If true, then only the matching edges that belong to paths for designs common to all 
	 * input design spaces are retained.
	 * @apiParam {String[]} [roles] If specified, then only edges labeled with at least one of these roles will be AND-ed.
	 * 
	 * @apiDescription Merges designs from merge design spaces then ands output of merge with and design spaces then Enumerates Set.
	 */
	
	 @RequestMapping(value = "/designSpace/mergeThenAndThenEnumerate", method = RequestMethod.GET)
	 public HashSet<List<Map<String, Object>>> mergeThenAndThenEnumerateDesignSpaces(@RequestParam(value = "mergeSpaceIDs", required = true) List<String> mergeSpaceIDs,
	 		@RequestParam(value = "andSpaceIDs", required = true) List<String> andSpaceIDs,
			@RequestParam(value = "mergeOutputSpaceID", required = true) String mergeOutputSpaceID,
			@RequestParam(value = "andOutputSpaceID", required = true) String andOutputSpaceID,
			@RequestParam(value = "mergeTolerance", required = false, defaultValue = "0") int mergeTolerance,
			@RequestParam(value = "andTolerance", required = false, defaultValue = "1") int andTolerance,
			@RequestParam(value = "weightTolerance", required = false, defaultValue = "0") int weightTolerance,
			@RequestParam(value = "isComplete", required = false, defaultValue = "true") boolean isComplete,
			@RequestParam(value = "roles", required = false, defaultValue = "") Set<String> roles) {
		
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		try {
			return designSpaceService.mergeThenAndThenEnumerateDesignSpaces(mergeSpaceIDs, andSpaceIDs, 
					mergeOutputSpaceID, andOutputSpaceID, mergeTolerance, andTolerance, weightTolerance, isComplete, roles);
		} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
				DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
			return null;
		}
	 }

	/**
	 * @api {post} /designSpace/or OR
	 * @apiName orDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} inputSpaceIDs IDs for the input design spaces to be OR-ed.
	 * @apiParam {String} [outputSpaceID] ID for the output design space resulting from OR.  If omitted, then the result is stored 
	 * @apiParam {String} [groupID] ID for the output design space's group. If omitted, groupID is "none".
	 * in the first input design space.
	 * 
	 * @apiDescription Unions designs from input design spaces.
	 */
	
	@RequestMapping(value = "/designSpace/or", method = RequestMethod.POST)
	public ResponseEntity<String> orDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
	        @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID) {
	    try {
	    	long startTime = System.nanoTime();
	    	
	        if (outputSpaceID == null) {
	            designSpaceService.orDesignSpaces(inputSpaceIDs, groupID);
	        } else {
	            designSpaceService.orDesignSpaces(inputSpaceIDs, outputSpaceID, groupID);
	        }
	
	        return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully OR-ed after " + 
	        		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	    } catch (ParameterEmptyException | DesignSpaceNotFoundException |
	            DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
	        return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
	                HttpStatus.BAD_REQUEST);
	    }
	}

	/**
	 * @api {post} /designSpace/repeat Repeat
	 * @apiName repeatDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} inputSpaceIDs IDs for the input design spaces to be repeated.
	 * @apiParam {String} [outputSpaceID] ID for the output design space resulting from Repeat. If omitted, then the result is 
	 * stored in the first input design space.
	 * @apiParam {String} [groupID] ID for the output design space's group. If omitted, groupID is "none".
	 * @apiParam {Boolean} isOptional=false If true, then designs from the input spaces are repeated zero-or-more times; otherwise, 
	 * they are repeated one-or-more times.
	 * 
	 * @apiDescription Concatenates and then repeats designs from input design spaces either zero-or-more or one-or-more times.
	 */
	
	@RequestMapping(value = "/designSpace/repeat", method = RequestMethod.POST)
	public ResponseEntity<String> repeatDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
	        @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
	        @RequestParam(value = "isOptional", required = false, defaultValue = "false") boolean isOptional) {
	    try {
	    	long startTime = System.nanoTime();
	    	
	        if (outputSpaceID == null) {
	            designSpaceService.repeatDesignSpaces(inputSpaceIDs, groupID, isOptional);
	        } else {
	            designSpaceService.repeatDesignSpaces(inputSpaceIDs, outputSpaceID, groupID, isOptional);
	        }
	
	        return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully repeated after " + 
	        		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
	    } catch (ParameterEmptyException | DesignSpaceNotFoundException |
	            DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
	        return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
	                HttpStatus.BAD_REQUEST);
	    }
	}


//	@RequestMapping(value = "/designSpace/export", method = RequestMethod.POST)
//	public ResponseEntity<String> exportDesignSpaces(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
//													 @RequestParam(value = "targetBranchID", required = false) String targetBranchID) {
//		try {
//			long startTime = System.nanoTime();
//
//			DesignSpace target = designSpaceRepository.findBySpaceID(targetSpaceID);
//			SBOLGeneration sbolGen = new SBOLGeneration(target, "http://knox.org");
//
//			SBOLDocument document = sbolGen.createSBOLDocument();
//
//
//			return new ResponseEntity<String>("{\"message\": \"Design space successfully exported as SBOL" +
//					(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
//		} catch (ParameterEmptyException | DesignSpaceNotFoundException |
//				DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
//			return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
//					HttpStatus.BAD_REQUEST);
//		}
//	}



	@RequestMapping(value = "/import/csv", method = RequestMethod.POST)
    public ResponseEntity<String> importCSV(@RequestParam("inputCSVFiles[]") List<MultipartFile> inputCSVFiles,
    		@RequestParam(value = "outputSpacePrefix", required = true) String outputSpacePrefix, 
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "weight", defaultValue = "0.0", required = false) String weight) {
    	List<InputStream> inputCSVStreams = new ArrayList<InputStream>();

		int weightTolerance = 0;
    	
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
    	
		designSpaceService.importCSV(inputCSVStreams, outputSpacePrefix, groupID, false, weight, weightTolerance);
		
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/merge/csv", method = RequestMethod.POST)
    public ResponseEntity<String> mergeCSV(@RequestParam("inputCSVFiles[]") List<MultipartFile> inputCSVFiles,
            @RequestParam(value = "outputSpacePrefix", required = true) String outputSpacePrefix,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "weight", defaultValue = "0.0", required = false) String weight,
			@RequestParam(value = "weightTolerance", defaultValue = "0", required = false) int weightTolerance) {
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

        designSpaceService.importCSV(inputCSVStreams, outputSpacePrefix, groupID, true, weight, weightTolerance);

        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/sbol/import", method = RequestMethod.POST)
    public ResponseEntity<String> importSBOL(@RequestParam("inputSBOLFiles[]") List<MultipartFile> inputSBOLFiles,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "weight", required = false, defaultValue = "0.0") Double weight) {
    	List<SBOLDocument> sbolDocs = new ArrayList<SBOLDocument>();
    	
    	for (MultipartFile inputSBOLFile : inputSBOLFiles) {
    		if (!inputSBOLFile.isEmpty()) {
    			try {
    				sbolDocs.add(SBOLReader.read(inputSBOLFile.getInputStream()));
				} catch (IOException | SBOLValidationException | SBOLConversionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	
    	try {
			designSpaceService.importSBOL(sbolDocs, outputSpaceID, groupID, weight);
		} catch (IOException | SBOLValidationException | SBOLConversionException | SBOLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

	@RequestMapping(value = "/goldbarSBOL/import", method = RequestMethod.POST)
	public ResponseEntity<String> importGoldbarSBOL(@RequestParam(value = "sbolDoc", required = true) String sbolDoc,
			@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "weight", required = false) Double weight) throws IOException {
		List<SBOLDocument> sbolDocs = new ArrayList<SBOLDocument>();
		SBOLDocument sbolDocument = new SBOLDocument();

		InputStream sbolStream = org.apache.commons.io.IOUtils.toInputStream(sbolDoc, "UTF-8");
		
		try {
			sbolDocument = SBOLReader.read(sbolStream);
			sbolDocs.add(sbolDocument);

		} catch (IOException | SBOLValidationException | SBOLConversionException e) {
			e.printStackTrace();
		}


		try {
			designSpaceService.importSBOL(sbolDocs, outputSpaceID, groupID, weight);
		} catch (IOException | SBOLValidationException | SBOLConversionException | SBOLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/goldbar/import", method = RequestMethod.POST)
	public ResponseEntity<String> importGoldbar(@RequestParam(value = "goldbar", required = true) String goldbarString,
			@RequestParam(value = "categories", required = true) String categoriesString,
			@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID,
			@RequestParam(value = "weight", required = false) Double weight) throws IOException {
		
		System.out.println();		
		System.out.println("GOLDBAR:");
		System.out.println(goldbarString);
		System.out.println();
		System.out.println("Categories:");
		System.out.println(categoriesString);
		
		try{
			JSONObject goldbar = new JSONObject(goldbarString);
			JSONObject categories = new JSONObject(categoriesString);
			designSpaceService.importGoldbar(goldbar, categories, outputSpaceID, groupID, weight);

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/goldbarGen/generator", method = RequestMethod.POST)
	public Map<String, Object> goldbarGenerator(@RequestParam(value = "inputCSVFiles[]", required = true) List<MultipartFile> inputCSVFiles, 
			@RequestParam(value = "rules", required = false) String rules, 
			@RequestParam(value = "lengths", required = false) String lengths,
			@RequestParam(value = "outputSpacePrefix", required = false) String OutputSpacePrefix,
			@RequestParam(value = "verify", required = false, defaultValue="true") Boolean verify,
			@RequestParam(value = "direction", required = false, defaultValue="true") String direction) {
		
		System.out.println("Starting GOLDBAR Generator");
		try {
			InputStream inputCSVStream = inputCSVFiles.get(0).getInputStream();

			String[] rulesArray = rules.split(",");
			ArrayList<String> rulesList = new ArrayList<>(Arrays.asList(rulesArray));
			System.out.println("Rules" + rulesList);

			String[] lengthsArray = lengths.split(",");
			ArrayList<String> lengthsList = new ArrayList<>(Arrays.asList(lengthsArray));
			System.out.println("Lengths" + lengthsList);
			
			return designSpaceService.goldbarGeneration(rulesList, inputCSVStream, lengthsList, OutputSpacePrefix, verify, direction);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new HashMap<>();
	}

	// LIST VER
	@RequestMapping(value = "/sbol/exportCombinatorial", method = RequestMethod.GET)
	public ResponseEntity<List<String>> exportCombinatorial(@RequestParam(value = "targetSpaceID") String targetSpaceID,
											 @RequestParam(value = "namespace", required = false) String namespace) {

		List<String> res;
		try {
			res = designSpaceService.exportCombinatorial(targetSpaceID, namespace);
//			return new ResponseEntity<String>(goldbar, HttpStatus.NO_CONTENT);
			System.out.println("end of try");

		} catch (IOException | SBOLValidationException | SBOLConversionException | SBOLException | URISyntaxException e) {
			e.printStackTrace();
			res = Arrays.asList("{\"message\": \"" + e.getMessage() + "\"}");
			System.out.println("before returning bad request");
			return new ResponseEntity<List<String>>(res, HttpStatus.BAD_REQUEST);
		}

		//TODO should download SBOL document or throw error on the UI
		System.out.println("before returning data");
		return new ResponseEntity<List<String>>(res, HttpStatus.OK);
	}
    
    @RequestMapping(value = "/branch/graph/d3", method = RequestMethod.GET)
    public Map<String, Object> d3GraphBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
        return designSpaceService.d3GraphBranches(targetSpaceID);
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

    @RequestMapping(value = "/designSpace/sample", method = RequestMethod.GET)
    public Set<List<String>> sample(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "numDesigns", required = false, defaultValue = "0") int numDesigns,
			@RequestParam(value = "minLength", required = false, defaultValue = "0") int minLength,
			@RequestParam(value = "maxLength", required = false, defaultValue = "0") int maxLength,
			@RequestParam(value = "isWeighted", required = false, defaultValue = "false") boolean isWeighted,
			@RequestParam(value = "positiveOnly", required = false, defaultValue = "false") boolean positiveOnly,
			@RequestParam(value = "isSampleSpace", required = false, defaultValue = "false") boolean isSampleSpace) {
        return designSpaceService.sampleDesignSpace(targetSpaceID, numDesigns, minLength, maxLength, isWeighted, positiveOnly, isSampleSpace);
    }

    @RequestMapping(value = "/designSpace/list", method = RequestMethod.GET)
    public List<String> listDesignSpaces() {
        return designSpaceService.listDesignSpaces();
    }

    @RequestMapping(value = "/designSpace/enumerateSet", method = RequestMethod.GET)
    public HashSet<List<Map<String, Object>>> enumerateSet(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "numDesigns", required = false, defaultValue = "0") int numDesigns,
			@RequestParam(value = "isWeighted", required = false, defaultValue = "true") boolean isWeighted,
			@RequestParam(value = "isSampleSpace", required = false, defaultValue = "false") boolean isSampleSpace,
			@RequestParam(value = "printDesigns", required = false, defaultValue = "false") boolean printDesigns,
            @RequestParam(value = "minLength", required = false, defaultValue = "0") int minLength,
            @RequestParam(value = "maxLength", required = false, defaultValue = "0") int maxLength,
            @RequestParam(value = "bfs", required = true, defaultValue = "true") boolean bfs) {
        EnumerateType enumerateType = bfs ? EnumerateType.BFS : EnumerateType.DFS;  // BFS is default
        
        return designSpaceService.enumerateDesignSpaceSet(targetSpaceID, numDesigns, minLength, maxLength, 
        		EnumerateType.BFS, isWeighted, isSampleSpace, printDesigns);
    }

	@RequestMapping(value = "/designSpace/enumerateList", method = RequestMethod.GET)
    public List<List<Map<String, Object>>> enumerateList(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
            @RequestParam(value = "numDesigns", required = false, defaultValue = "0") int numDesigns,
			@RequestParam(value = "isWeighted", required = false, defaultValue = "true") boolean isWeighted,
			@RequestParam(value = "isSampleSpace", required = false, defaultValue = "false") boolean isSampleSpace,
			@RequestParam(value = "printDesigns", required = false, defaultValue = "false") boolean printDesigns,
            @RequestParam(value = "minLength", required = false, defaultValue = "0") int minLength,
            @RequestParam(value = "maxLength", required = false, defaultValue = "0") int maxLength,
            @RequestParam(value = "bfs", required = true, defaultValue = "true") boolean bfs) {
        EnumerateType enumerateType = bfs ? EnumerateType.BFS : EnumerateType.DFS;  // BFS is default
        
        return designSpaceService.enumerateDesignSpaceList(targetSpaceID, numDesigns, minLength, maxLength, 
        		EnumerateType.BFS, isWeighted, isSampleSpace, printDesigns);
    }

	@RequestMapping(value = "/designSpace/score", method = RequestMethod.GET)
    public List<String> graphScore(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID){
        
        return designSpaceService.getGraphScore(targetSpaceID);
    }

	@RequestMapping(value = "/designSpace/bestPath", method = RequestMethod.GET)
    public List<List<Map<String, Object>>> getBestPath(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID){
        
        return designSpaceService.getBestPath(targetSpaceID);
    }

	@RequestMapping(value = "/designSpace/partAnalytics", method = RequestMethod.GET)
    public Map<String, Map<String, Object>> partAnalytics(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID){
        
        return designSpaceService.partAnalytics(targetSpaceID);
    }

	@RequestMapping(value = "/designSpace/createSampleSpace", method = RequestMethod.GET)
	public Boolean createSampleSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
			@RequestParam(value = "groupID", required = false) String groupID){
		return designSpaceService.createSampleSpace(targetSpaceID, groupID);
	}
}
