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
	 * @apiParam {Boolean} isComplete=false If true, then only edges belonging to paths for designs common to all input design 
	 * spaces are retained.
	 * @apiParam {String[]} [roles] If specified, then only edges labeled with at least one of these roles will be merged.
	 * 
	 * @apiDescription Merges designs from input design spaces. Based on strong product of graphs.
	 */
	
	@RequestMapping(value = "/branch/merge", method = RequestMethod.POST)
    public ResponseEntity<String> mergeBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "2") int tolerance,
    		@RequestParam(value = "isComplete", required = false, defaultValue = "false") boolean isComplete,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) { 	
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
    		designSpaceService.mergeBranches(targetSpaceID, inputBranchIDs, tolerance, isComplete,
    				uniqueRoles); 
    	} else {
    		designSpaceService.mergeBranches(targetSpaceID, inputBranchIDs, outputBranchID, 
    				tolerance, isComplete, uniqueRoles);
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
			@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
			@RequestParam(value = "isComplete", required = false, defaultValue = "true") boolean isComplete,
			@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		try {
			long startTime = System.nanoTime();
			
			if (outputSpaceID == null) {
				designSpaceService.andDesignSpaces(inputSpaceIDs, tolerance, isComplete, uniqueRoles);
			} else {
				designSpaceService.andDesignSpaces(inputSpaceIDs, outputSpaceID, tolerance, 
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
	 * in the first input design space.
	 * 
	 * @apiDescription Concatenates designs from input design spaces.
	 */
	
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

	/**
	 * @api {post} /designSpace/merge Merge
	 * @apiName mergeDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} inputSpaceIDs IDs for the input design spaces to be merged.
	 * @apiParam {String} [outputSpaceID] ID for the output design space resulting from Merge.  If omitted, then the result is
	 *  stored in the first input design space.
	 * @apiParam {Integer=0,1,2,3,4} tolerance=1 This parameter determines the criteria by which edges are matched. If tolerance
	 * = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges 
	 * must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component 
	 * roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be 
	 * labeled with the same orientation. If tolerance <= 1, then labels on matching edges are intersected; otherwise, they are 
	 * unioned.
	 * @apiParam {Boolean} isComplete=false If true, then only the matching edges that belong to paths for designs common to all 
	 * input design spaces are retained prior to adding the non-matching edges from these spaces.
	 * @apiParam {String[]} [roles] If specified, then only edges labeled with at least one of these roles will be AND-ed.
	 * 
	 * @apiDescription Merges designs from input design spaces. Based on strong product of graphs.
	 */
	
	@RequestMapping(value = "/designSpace/merge", method = RequestMethod.POST)
	public ResponseEntity<String> mergeDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
			@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
			@RequestParam(value = "tolerance", required = false, defaultValue = "2") int tolerance,
			@RequestParam(value = "isComplete", required = false, defaultValue = "false") boolean isComplete,
			@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		try {
			long startTime = System.nanoTime();
			
			if (outputSpaceID == null) {
				designSpaceService.mergeDesignSpaces(inputSpaceIDs, tolerance, isComplete, uniqueRoles);
			} else {
				designSpaceService.mergeDesignSpaces(inputSpaceIDs, outputSpaceID, tolerance, 
						isComplete, uniqueRoles);
			}
			
			return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully merged after " +
					(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
		} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
				DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
			return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * @api {post} /designSpace/or OR
	 * @apiName orDesignSpaces
	 * @apiGroup DesignSpace
	 * 
	 * @apiParam {String[]} inputSpaceIDs IDs for the input design spaces to be OR-ed.
	 * @apiParam {String} [outputSpaceID] ID for the output design space resulting from OR.  If omitted, then the result is stored 
	 * in the first input design space.
	 * 
	 * @apiDescription Unions designs from input design spaces.
	 */
	
	@RequestMapping(value = "/designSpace/or", method = RequestMethod.POST)
	public ResponseEntity<String> orDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
	        @RequestParam(value = "outputSpaceID", required = false) String outputSpaceID) {
	    try {
	    	long startTime = System.nanoTime();
	    	
	        if (outputSpaceID == null) {
	            designSpaceService.orDesignSpaces(inputSpaceIDs);
	        } else {
	            designSpaceService.orDesignSpaces(inputSpaceIDs, outputSpaceID);
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
	 * @apiParam {Boolean} isOptional=false If true, then designs from the input spaces are repeated zero-or-more times; otherwise, 
	 * they are repeated one-or-more times.
	 * 
	 * @apiDescription Concatenates and then repeats designs from input design spaces either zero-or-more or one-or-more times.
	 */
	
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

	@RequestMapping(value = "/designSpace/constrain", method = RequestMethod.POST)
    public ResponseEntity<String> constrainDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID,
    		@RequestBody List<Rule> rules) {
        try {
        	long startTime = System.nanoTime();
        	
            designSpaceService.constrainDesignSpace(targetSpaceID, outputSpaceID, rules);

            return new ResponseEntity<String>("{\"message\": \"Design spaces was successfully constrained after " + 
            		(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
        } catch (ParameterEmptyException | DesignSpaceNotFoundException |
                DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
            return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}",
                    HttpStatus.BAD_REQUEST);
        }
    }
	
	@RequestMapping(value = "/designSpace/diff", method = RequestMethod.POST)
    public ResponseEntity<String> diffDesignSpaces(@RequestParam(value = "inputSpaceIDs", required = true) List<String> inputSpaceIDs,
    		@RequestParam(value = "outputSpaceID", required = false) String outputSpaceID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
    		@RequestParam(value = "isRow", required = false, defaultValue = "true") boolean isRow,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
    	Set<String> uniqueRoles = new HashSet<String>(roles);
    	
		try {
    		long startTime = System.nanoTime();
    		
    		if (outputSpaceID == null) {
    			designSpaceService.diffDesignSpaces(inputSpaceIDs, tolerance, isRow, 
    					uniqueRoles);
    		} else {
    			designSpaceService.diffDesignSpaces(inputSpaceIDs, outputSpaceID, tolerance, isRow, 
    					uniqueRoles);
    		}

    		return new ResponseEntity<String>("{\"message\": \"Design spaces were successfully diff-ed after " +
    				(System.nanoTime() - startTime) + " ns.\"}", HttpStatus.NO_CONTENT);
    	} catch (ParameterEmptyException | DesignSpaceNotFoundException | 
    			DesignSpaceConflictException | DesignSpaceBranchesConflictException ex) {
    		return new ResponseEntity<String>("{\"message\": \"" + ex.getMessage() + "\"}", 
    				HttpStatus.BAD_REQUEST);
    	}
    }
	
	@RequestMapping(value = "/branch/diff", method = RequestMethod.POST)
    public ResponseEntity<String> diffBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID, 
    		@RequestParam(value = "inputBranchIDs", required = true) List<String> inputBranchIDs,
    		@RequestParam(value = "outputBranchID", required = false) String outputBranchID,
    		@RequestParam(value = "tolerance", required = false, defaultValue = "1") int tolerance,
    		@RequestParam(value = "isRow", required = false, defaultValue = "true") boolean isRow,
    		@RequestParam(value = "roles", required = false, defaultValue = "") List<String> roles) {
		Set<String> uniqueRoles = new HashSet<String>(roles);
		
		long startTime = System.nanoTime();
		
		if (outputBranchID == null) {
    		designSpaceService.diffBranches(targetSpaceID, inputBranchIDs, tolerance, isRow, 
    				uniqueRoles);
		} else {
			designSpaceService.diffBranches(targetSpaceID, inputBranchIDs, outputBranchID, tolerance,
					isRow, uniqueRoles);
		}
    	
    	return new ResponseEntity<String>("{\"message\": \"Branches were successfully diff-ed after " + 
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
    
    @RequestMapping(value = "/branch/graph/d3", method = RequestMethod.GET)
    public Map<String, Object> d3GraphBranches(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
        return designSpaceService.d3GraphBranches(targetSpaceID);
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
            @RequestParam(value = "minLength", required = false, defaultValue = "0") int minLength,
            @RequestParam(value = "maxLength", required = false, defaultValue = "0") int maxLength,
            @RequestParam(value = "bfs", required = true, defaultValue = "true") boolean bfs) {
        EnumerateType enumerateType = bfs ? EnumerateType.BFS : EnumerateType.DFS;  // BFS is default
        
        return designSpaceService.enumerateDesignSpace(targetSpaceID, numDesigns, minLength, maxLength, 
        		enumerateType);
    }

//    @RequestMapping(value = "/partition", method = RequestMethod.GET)
//    public Set<List<String>> partition(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
//        return designSpaceService.partitionDesignSpace(targetSpaceID);
//    }
}
