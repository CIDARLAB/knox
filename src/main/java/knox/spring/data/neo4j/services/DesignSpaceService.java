package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.analysis.DesignAnalysis;
import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.Commit;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Snapshot;
import knox.spring.data.neo4j.domain.dto.DesignSpaceEdgeDTO;
import knox.spring.data.neo4j.exception.*;
import knox.spring.data.neo4j.operations.ANDOperator;
import knox.spring.data.neo4j.operations.Concatenation;
import knox.spring.data.neo4j.operations.JoinOperator;
import knox.spring.data.neo4j.operations.MergeOperator;
import knox.spring.data.neo4j.operations.OROperator;
import knox.spring.data.neo4j.operations.Product;
import knox.spring.data.neo4j.operations.RepeatOperator;
import knox.spring.data.neo4j.operations.Star;
import knox.spring.data.neo4j.operations.Union;
import knox.spring.data.neo4j.operations.WeightOperator;
import knox.spring.data.neo4j.operations.ReverseOperator;
import knox.spring.data.neo4j.repositories.BranchRepository;
import knox.spring.data.neo4j.repositories.CommitRepository;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;
import knox.spring.data.neo4j.repositories.EdgeRepository;
import knox.spring.data.neo4j.repositories.NodeRepository;
import knox.spring.data.neo4j.repositories.SnapshotRepository;
import knox.spring.data.neo4j.sample.DesignSampler;
import knox.spring.data.neo4j.sample.DesignSampler.EnumerateType;
import knox.spring.data.neo4j.sbol.SBOLConversion;
import knox.spring.data.neo4j.goldbar.GoldbarConversion;
import knox.spring.data.neo4j.goldbar.GoldbarGeneration;

import knox.spring.data.neo4j.sbol.SBOLGeneration;

import org.apache.http.client.HttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceOntology;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
//@Transactional
public class DesignSpaceService {
	@Autowired BranchRepository branchRepository;
	
    @Autowired CommitRepository commitRepository;
    
    @Autowired DesignSpaceRepository designSpaceRepository;
    
    @Autowired EdgeRepository edgeRepository;
    
    @Autowired NodeRepository nodeRepository;
    
    @Autowired SnapshotRepository snapshotRepository;
    
    private static final Logger LOG = LoggerFactory.getLogger(DesignSpaceService.class);

    public static final String RESERVED_ID = "knox";
    
    public void joinDesignSpaces(List<String> inputSpaceIDs, String groupID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	joinDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), groupID);
    }
    
    public void joinDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, String groupID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
		outputSpace.setGroupID(groupID);

    	JoinOperator.apply(inputSpaces, outputSpace);
    	
//    	List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputSpaces.size());
//    	
//    	NodeSpace outputSnap = mergeVersionHistories(castNodeSpacesToDesignSpaces(inputSpaces), 
//    			outputSpace, inputSnaps);
//
//    	JoinOperator.apply(inputSnaps, outputSnap);

    	saveDesignSpace(outputSpace);
    }
    
    public void joinBranches(String targetSpaceID, List<String> inputBranchIDs) {
        joinBranches(targetSpaceID, inputBranchIDs, inputBranchIDs.get(0));
    }

    public void joinBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		String outputBranchID) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
        List<Branch> inputBranches = new ArrayList<Branch>(inputBranchIDs.size());
        
        Branch outputBranch = loadIOBranches(targetSpace, inputBranchIDs, outputBranchID,
        		inputBranches);
        
        List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputBranches.size());
        
        NodeSpace outputSnap = mergeVersions(targetSpace, inputBranches, outputBranch, inputSnaps);

        JoinOperator.apply(inputSnaps, outputSnap);
        
        saveDesignSpace(targetSpace);
    }
    
    public void orDesignSpaces(List<String> inputSpaceIDs, String groupID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	orDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), groupID);
    }
    
    public void orDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, String groupID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	List<NodeSpace> inputSpaces = new LinkedList<NodeSpace>();
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
		outputSpace.setGroupID(groupID);
    	
    	OROperator.apply(inputSpaces, outputSpace);
    	
//    	List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputSpaces.size());
//    	
//    	NodeSpace outputSnap = mergeVersionHistories(castNodeSpacesToDesignSpaces(inputSpaces), 
//    			outputSpace, inputSnaps);
//    	
//    	OROperator.apply(inputSnaps, outputSnap);

    	saveDesignSpace(outputSpace);
    }
    
    public void orBranches(String targetSpaceID, List<String> inputBranchIDs) {
        orBranches(targetSpaceID, inputBranchIDs, inputBranchIDs.get(0));
    }

    public void orBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		String outputBranchID) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
        List<Branch> inputBranches = new ArrayList<Branch>(inputBranchIDs.size());
        
        Branch outputBranch = loadIOBranches(targetSpace, inputBranchIDs, outputBranchID,
        		inputBranches);
        
        List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputBranches.size());
        
        NodeSpace outputSnap = mergeVersions(targetSpace, inputBranches, outputBranch, inputSnaps);

        OROperator.apply(inputSnaps, outputSnap);
        
        saveDesignSpace(targetSpace);
    }
	
	public void repeatDesignSpaces(List<String> inputSpaceIDs, String groupID, boolean isOptional) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	repeatDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), groupID, isOptional);
    }
    
    public void repeatDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, String groupID, boolean isOptional) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	List<NodeSpace> inputSpaces = new LinkedList<NodeSpace>();
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
		outputSpace.setGroupID(groupID);
    	
    	RepeatOperator.apply(inputSpaces, outputSpace, isOptional);
    	
//    	List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputSpaces.size());
//    	
//    	NodeSpace outputSnap = mergeVersionHistories(castNodeSpacesToDesignSpaces(inputSpaces), 
//    			outputSpace, inputSnaps);
//    	
//    	RepeatOperator.apply(inputSnaps, outputSnap, isOptional);

    	saveDesignSpace(outputSpace);
    }
    
    public void repeatBranches(String targetSpaceID, List<String> inputBranchIDs, boolean isOptional) {
        repeatBranches(targetSpaceID, inputBranchIDs, inputBranchIDs.get(0), isOptional);
    }

    public void repeatBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		String outputBranchID, boolean isOptional) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
        List<Branch> inputBranches = new ArrayList<Branch>(inputBranchIDs.size());
        
        Branch outputBranch = loadIOBranches(targetSpace, inputBranchIDs, outputBranchID,
        		inputBranches);
        
        List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputBranches.size());
        
        NodeSpace outputSnap = mergeVersions(targetSpace, inputBranches, outputBranch, inputSnaps);

        RepeatOperator.apply(inputSnaps, outputSnap, isOptional);
        
        saveDesignSpace(targetSpace);
    }
    
    public void andDesignSpaces(List<String> inputSpaceIDs, String groupID, int tolerance, boolean isComplete,
    		Set<String> roles) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    	    DesignSpaceConflictException, DesignSpaceBranchesConflictException{
    	andDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), groupID, tolerance, isComplete, roles);
    }
    
    public void andDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, String groupID, int tolerance,
    		boolean isComplete, Set<String> roles)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
//    	long startTime = System.nanoTime();
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);

    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
		outputSpace.setGroupID(groupID);
    	
    	ANDOperator.apply(inputSpaces, outputSpace, tolerance, isComplete, roles, new ArrayList<String>());
//    	long endTime = System.nanoTime();
//    	long duration = (endTime - startTime);
//    	LOG.info("AND TIME: " + duration);
//    	LOG.info("AND NODES: " + outputSpace.getNodes().size());
//    	LOG.info("AND EDGES: " + outputSpace.getEdges().size());
    	
//      No version history
//    	List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputSpaces.size());
//    	
//    	NodeSpace outputSnap = mergeVersionHistories(castNodeSpacesToDesignSpaces(inputSpaces), 
//    			outputSpace, inputSnaps);
//    	
//    	ANDOperator.apply(inputSnaps, outputSnap, tolerance, isComplete, roles);
    	
    	saveDesignSpace(outputSpace);
    }
    
    public void andBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		int tolerance, boolean isComplete, Set<String> roles) {
        andBranches(targetSpaceID, inputBranchIDs, inputBranchIDs.get(0), tolerance, 
        		isComplete, roles);
    }

    public void andBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		String outputBranchID, int tolerance, boolean isComplete, Set<String> roles) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
        List<Branch> inputBranches = new ArrayList<Branch>(inputBranchIDs.size());
        
        Branch outputBranch = loadIOBranches(targetSpace, inputBranchIDs, outputBranchID,
        		inputBranches);
        
        List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputBranches.size());
        
        NodeSpace outputSnap = mergeVersions(targetSpace, inputBranches, outputBranch, 
        		inputSnaps);

        ANDOperator.apply(inputSnaps, outputSnap, tolerance, isComplete, roles, new ArrayList<String>());
        
        saveDesignSpace(targetSpace);
    }
	
	public void mergeDesignSpaces(List<String> inputSpaceIDs, String groupID, int tolerance, int weightTolerance, Set<String> roles, ArrayList<String> irrelevantParts) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    	    DesignSpaceConflictException, DesignSpaceBranchesConflictException{
		mergeDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), groupID, tolerance, weightTolerance, roles, irrelevantParts);
    }
    
    public void mergeDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, String groupID, int tolerance, int weightTolerance,
    		Set<String> roles, ArrayList<String> irrelevantParts)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
//    	long startTime = System.nanoTime();
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);

    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
		outputSpace.setGroupID(groupID);
    	
    	MergeOperator.apply(inputSpaces, outputSpace, tolerance, weightTolerance, roles, irrelevantParts);
//    	long endTime = System.nanoTime();
//    	long duration = (endTime - startTime);
//    	LOG.info("MERGE TIME: " + duration);
//    	LOG.info("MERGE NODES: " + outputSpace.getNodes().size());
//    	LOG.info("MERGE EDGES: " + outputSpace.getEdges().size());
    	
//      No version history
//    	List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputSpaces.size());
//    	
//    	NodeSpace outputSnap = mergeVersionHistories(castNodeSpacesToDesignSpaces(inputSpaces), 
//    			outputSpace, inputSnaps);
//    	
//    	MergeOperator.apply(inputSnaps, outputSnap, tolerance, roles);

    	saveDesignSpace(outputSpace);
    }

	public HashSet<List<Map<String, Object>>> mergeThenAndThenEnumerateDesignSpaces(List<String> mergeSpaceIDs, List<String> andSpaceIDs, String mergeOutputSpaceID, String andOutputSpaceID,
			int mergeTolerance, int andTolerance, int weightTolerance, boolean isComplete, Set<String> roles)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {

		// Merge
    	validateCombinationalDesignSpaceOperator(mergeSpaceIDs, mergeOutputSpaceID);

    	List<NodeSpace> mergeInputSpaces = new ArrayList<NodeSpace>(mergeSpaceIDs.size());
    	
    	DesignSpace mergeOutputSpace = loadIOSpaces(mergeSpaceIDs, mergeOutputSpaceID, mergeInputSpaces);
    	
    	MergeOperator.apply(mergeInputSpaces, mergeOutputSpace, mergeTolerance, weightTolerance, roles, new ArrayList<String>());

    	saveDesignSpace(mergeOutputSpace);

		// AND
		validateCombinationalDesignSpaceOperator(andSpaceIDs, andOutputSpaceID);

    	List<NodeSpace> andInputSpaces = new ArrayList<NodeSpace>(andSpaceIDs.size());
    	
    	DesignSpace andOutputSpace = loadIOSpaces(andSpaceIDs, andOutputSpaceID, andInputSpaces);

		andInputSpaces.add((NodeSpace) mergeOutputSpace);

		System.out.println("length of And Input Spaces");
		System.out.println(andInputSpaces.size());
    	
    	ANDOperator.apply(andInputSpaces, andOutputSpace, andTolerance, isComplete, roles, new ArrayList<String>());

		saveDesignSpace(andOutputSpace);

		// Enumerate
		DesignSampler designSampler = new DesignSampler(andOutputSpace);
        
		System.out.println("\nBegin Enumeration\n");
        HashSet<List<Map<String, Object>>> samplerOutput = designSampler.enumerateSet(0, 0, 0, false, EnumerateType.BFS);

		return designSampler.processEnumerateSet(samplerOutput, true, false, false);
		
    }
    
    public void mergeBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		int tolerance, int weightTolerance, Set<String> roles) {
    	mergeBranches(targetSpaceID, inputBranchIDs, inputBranchIDs.get(0), tolerance, weightTolerance, roles);
    }

    public void mergeBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		String outputBranchID, int tolerance, int weightTolerance, Set<String> roles) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
        List<Branch> inputBranches = new ArrayList<Branch>(inputBranchIDs.size());
        
        Branch outputBranch = loadIOBranches(targetSpace, inputBranchIDs, outputBranchID,
        		inputBranches);
        
        List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputBranches.size());
        
        NodeSpace outputSnap = mergeVersions(targetSpace, inputBranches, outputBranch, 
        		inputSnaps);

        MergeOperator.apply(inputSnaps, outputSnap, tolerance, weightTolerance, roles, new ArrayList<String>());
        
        saveDesignSpace(targetSpace);
    }
	
	private DesignSpace loadIOSpaces(List<String> inputSpaceIDs, String outputSpaceID,
    		List<NodeSpace> inputSpaces) {
    	for (String inputSpaceID : inputSpaceIDs) {
    		inputSpaces.add(loadDesignSpace(inputSpaceID));
    	}
    	
    	int outputIndex = inputSpaceIDs.indexOf(outputSpaceID);
    	
    	if (outputIndex < 0) {
    		return new DesignSpace(outputSpaceID);
    	} else {
    		return (DesignSpace) inputSpaces.get(outputIndex);
    	}
	}
	
	private Branch loadIOBranches(DesignSpace targetSpace, List<String> inputBranchIDs, 
			String outputBranchID, List<Branch> inputBranches) {
    	for (Branch branch : targetSpace.getBranches()) {
    		if (inputBranchIDs.contains(branch.getBranchID())) {
    			inputBranches.add(branch);
    		}
    	}
    	
    	int outputIndex = inputBranchIDs.indexOf(outputBranchID);
    	
    	if (outputIndex < 0) {
    		return targetSpace.createBranch(outputBranchID);
    	} else {
    		return inputBranches.get(outputIndex);
    	}
	}
	
	private List<DesignSpace> castNodeSpacesToDesignSpaces(List<NodeSpace> nodeSpaces) {
		List<DesignSpace> designSpaces = new ArrayList<DesignSpace>(nodeSpaces.size());
    	
    	for (NodeSpace nodeSpace : nodeSpaces) {
    		designSpaces.add((DesignSpace) nodeSpace);
    	}
    	
    	return designSpaces;
	}
    
    private NodeSpace mergeVersionHistories(List<DesignSpace> inputSpaces, DesignSpace outputSpace,
    		List<NodeSpace> outputSnaps) {
    	List<Branch> outputBranches = new ArrayList<Branch>(inputSpaces.size());
    	
    	for (DesignSpace inputSpace : inputSpaces) {
    		if (inputSpace.equals(outputSpace)) {
    			outputBranches.add(outputSpace.getHeadBranch());
    		} else {
    			outputBranches.add(outputSpace.copyVersionHistory(inputSpace));
    		}
    	}
    	
    	int outputIndex = inputSpaces.indexOf(outputSpace);
    	
    	Branch headOutputBranch;
    	
    	if (outputIndex < 0) {
    		headOutputBranch = outputBranches.get(0);
    	} else {
    		headOutputBranch = outputBranches.get(outputIndex);
    	}
    	
    	outputSpace.updateCommitIDs();
    	
    	return mergeVersions(outputSpace, outputBranches, headOutputBranch, outputSnaps);
    }
    
    private NodeSpace mergeVersions(DesignSpace targetSpace, List<Branch> inputBranches, 
    		Branch outputBranch, List<NodeSpace> inputSnaps) {
    	Set<Commit> inputCommits = new HashSet<Commit>();
    	
    	for (Branch inputBranch : inputBranches) {
    		inputCommits.add(inputBranch.getLatestCommit());
    		
    		inputSnaps.add(inputBranch.getLatestCommit().getSnapshot());
    	}
    	
    	Commit outputCommit = targetSpace.createCommit(outputBranch);
    	
    	outputBranch.setLatestCommit(outputCommit);
    	
    	outputCommit.createSnapshot();
    	
    	for (Branch inputBranch : inputBranches) {
    		if (!inputBranch.equals(outputBranch)) {
    			for (Commit inputCommit : inputBranch.getCommits()) {
    				outputBranch.addCommit(inputCommit);
    			}
    		}
    	}
    	
    	for (Commit inputCommit : inputCommits) {
    		outputCommit.addPredecessor(inputCommit);
    	}
    	
    	targetSpace.setHeadBranch(outputBranch);
    	
    	return outputBranch.getLatestCommit().getSnapshot();
    }

    public void importCSV(List<InputStream> inputCSVStreams, String outputSpacePrefix, String groupID,
    		boolean isMerge, String weight, int weightTolerance) {
    	List<BufferedReader> designReaders = new LinkedList<BufferedReader>();
    	
    	List<BufferedReader> compReaders = new LinkedList<BufferedReader>();

		List<BufferedReader> weightReaders = new LinkedList<BufferedReader>();

		Boolean weightCSV = false;

		Boolean multipleWeightsCSV = false;
    	
    	for (InputStream inputCSVStream : inputCSVStreams) {
    		try {
    			String csvLine;
    			
    			BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputCSVStream));
    			
    			if ((csvLine = csvReader.readLine()) != null) {
    				ArrayList<String> csvArray = csvToArrayList(csvLine);
    				
    				if (csvArray.size() > 0) {
    					if (csvArray.get(0).equals("id") && csvArray.get(1).equals("role")
    							&& csvArray.get(2).equals("sequence")) {
    						compReaders.add(csvReader);
    					} else if (csvArray.get(0).equals("design")) {
    						designReaders.add(csvReader);
    					} else if (csvArray.get(0).equals("weight")) {
							weightReaders.add(csvReader);
							weightCSV = true;
						} else if (csvArray.get(0).equals("multipleWeights")) {
							weightReaders.add(csvReader);
							multipleWeightsCSV = true;
						}
    				}
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	HashMap<String, String> compIDToRole = new HashMap<String, String>();
    	
    	for (BufferedReader compReader : compReaders) {
    		try {
    			compIDToRole.putAll(processCSVComponents(compReader));
    		} catch (IOException e) {
    			e.printStackTrace();
    		} finally {
    			try {
    				if (compReader != null) {
    					compReader.close();
    				}
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    	}

		// CSV to List (weights)
		List<String> weights = new ArrayList<String>();
		weights.add(weight);
		if (weightCSV) {
			try {
				weights = processCSVWeights(weightReaders.get(0));
				
			} catch (IOException e) {
    			e.printStackTrace();
    		} finally {
    			try {
    				if (weightReaders.get(0) != null) {
    					weightReaders.get(0).close();
    				}
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    	}

		// CSV to List (Multiple Weights)
		List<List<String>> multipleWeights = new ArrayList<>();
		if (multipleWeightsCSV) {
			try {
				multipleWeights = processCSVMultipleWeights(weightReaders.get(0));
				
			} catch (IOException e) {
    			e.printStackTrace();
    		} finally {
    			try {
    				if (weightReaders.get(0) != null) {
    					weightReaders.get(0).close();
    				}
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    	}
		
    	List<NodeSpace> csvSpaces = new LinkedList<NodeSpace>();
    	
    	for (BufferedReader designReader : designReaders) {
    		try {
    			csvSpaces.addAll(processCSVDesigns(designReader, outputSpacePrefix, compIDToRole, weights, multipleWeights, weightCSV, multipleWeightsCSV));
    		} catch (IOException e) {
    			e.printStackTrace();
    		} finally {
    			try {
    				if (designReader != null) {
    					designReader.close();
    				}
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    	}
    	
    	if (!csvSpaces.isEmpty()) {
    		DesignSpace outputSpace = new DesignSpace(outputSpacePrefix, groupID);
    		
    		if (isMerge) {
    			MergeOperator.apply(csvSpaces, outputSpace, 1, weightTolerance, new HashSet<String>(), new ArrayList<String>());

    			saveDesignSpace(outputSpace);
    		} else {
    			OROperator.apply(csvSpaces, outputSpace);
    			
    			saveDesignSpace(outputSpace);
    		}
    	}
    }
    
    public List<DesignSpace> processCSVDesigns(BufferedReader csvReader, String outputSpacePrefix, 
    		HashMap<String, String> compIDToRole, List<String> defaultWeight, List<List<String>> multipleWeights, Boolean weightCSV, Boolean multipleWeightsCSV) throws IOException {
    	List<DesignSpace> csvSpaces = new LinkedList<DesignSpace>();
    	
    	SequenceOntology so = new SequenceOntology();
    	
    	String csvLine;
    	
		int j = -1;

		int designNumber = 0;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);

			// Use Input weight or CSV weights
			Double default_Weight = 0.0;
			if (weightCSV) {
				default_Weight = Double.parseDouble(defaultWeight.get(designNumber));
			} else {
				default_Weight = Double.parseDouble(defaultWeight.get(0));
			}

			ArrayList<Double> weight = new ArrayList<Double>(Arrays.asList(default_Weight));

			if (multipleWeightsCSV) {
				if (csvArray.size() > 0 && csvArray.get(0).length() > 0) {
					j++;
					designNumber++;
	
					DesignSpace outputSpace = new DesignSpace(outputSpacePrefix + j);
	
					Node outputStart = outputSpace.createStartNode();
	
					Node outputPredecessor = outputStart;
	
					for (int i = 0; i < csvArray.size(); i++) {
						if (csvArray.get(i).length() > 0) {
							ArrayList<String> compIDs = new ArrayList<String>(1);
	
							compIDs.add(csvArray.get(i));
	
							ArrayList<String> compRoles = new ArrayList<String>(1);
	
							if (compIDToRole.containsKey(csvArray.get(i))) {
								compRoles.add(compIDToRole.get(csvArray.get(i)));
							} else {
								compRoles.add(so.getName(SequenceOntology.SEQUENCE_FEATURE));
							}
							
							Node outputNode;
	
							if (i < csvArray.size() - 1) {
								outputNode = outputSpace.createNode();
							} else {
								outputNode = outputSpace.createAcceptNode();
							}

							ArrayList<Double> edge_weight = new ArrayList<Double>(Arrays.asList(Double.parseDouble(multipleWeights.get(j).get(i))));
	
							outputPredecessor.createEdge(outputNode, compIDs, compRoles, Edge.Orientation.INLINE, edge_weight);
	
							outputPredecessor = outputNode;
						}
					}
	
					csvSpaces.add(outputSpace);
				}

			} else {
				if (csvArray.size() > 0 && csvArray.get(0).length() > 0) {
					j++;
					designNumber++;
	
					DesignSpace outputSpace = new DesignSpace(outputSpacePrefix + j);
	
					Node outputStart = outputSpace.createStartNode();
	
					Node outputPredecessor = outputStart;
	
					for (int i = 0; i < csvArray.size(); i++) {
						if (csvArray.get(i).length() > 0) {
							ArrayList<String> compIDs = new ArrayList<String>(1);
	
							compIDs.add(csvArray.get(i));
	
							ArrayList<String> compRoles = new ArrayList<String>(1);
	
							if (compIDToRole.containsKey(csvArray.get(i))) {
								compRoles.add(compIDToRole.get(csvArray.get(i)));
							} else {
								compRoles.add(so.getName(SequenceOntology.SEQUENCE_FEATURE));
							}
							
							Node outputNode;
	
							if (i < csvArray.size() - 1) {
								outputNode = outputSpace.createNode();
							} else {
								outputNode = outputSpace.createAcceptNode();
							}
	
							outputPredecessor.createEdge(outputNode, compIDs, compRoles, Edge.Orientation.INLINE, weight);
	
							outputPredecessor = outputNode;
						}
					}
	
					csvSpaces.add(outputSpace);
				}

			}
			
		}
		
		return csvSpaces;
    }
    
    public HashMap<String, String> processCSVComponents(BufferedReader csvReader) throws IOException {
    	HashMap<String, String> compIDToRole = new HashMap<String, String>();
    	
    	String csvLine;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);
			
			if (csvArray.size() >= 2) {
				compIDToRole.put(csvArray.get(0), convertCSVRole(csvArray.get(1)));
			}
		}
		
		return compIDToRole;
    }


	public List<String> processCSVWeights(BufferedReader csvReader) throws IOException {
		List<String> weights = new ArrayList<String>();

		String csvLine;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);

			weights.add(csvArray.get(0));
		}

		return weights;
	}

	public List<List<String>> processCSVMultipleWeights(BufferedReader csvReader) throws IOException {
		List<List<String>> MultipleWeights = new ArrayList<>();

		String csvLine;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);

			List<String> weights = new ArrayList<String>();
			for (String weight : csvArray) {
				weights.add(weight);
			}

			MultipleWeights.add(weights);
		}

		return MultipleWeights;
	}

	public void importSBOL(List<SBOLDocument> sbolDocs, String outputSpaceID, String groupID, Double weight)
			throws SBOLValidationException, IOException, SBOLConversionException, SBOLException {
		SBOLConversion sbolConv = new SBOLConversion();

		sbolConv.setSbolDoc(sbolDocs, weight);

		List<DesignSpace> outputSpaces = sbolConv.convertSBOLsToSpaces();

		for (DesignSpace outputSpace: outputSpaces){
			correctComponentIds(outputSpace);
			//outputSpace.splitEdges();
			outputSpace.setGroupID(groupID);
			saveDesignSpace(outputSpace);
		}

	}

	public DesignSpace importSBOL(String sbolString, String groupID, Boolean save)
			throws SBOLValidationException, IOException, SBOLConversionException, SBOLException {
		
		List<SBOLDocument> sbolDocs = new ArrayList<SBOLDocument>();
		SBOLDocument sbolDocument = new SBOLDocument();
		Double weight = 0.0;

		InputStream sbolStream = org.apache.commons.io.IOUtils.toInputStream(sbolString, "UTF-8");
		
		try {
			sbolDocument = SBOLReader.read(sbolStream);
			sbolDocs.add(sbolDocument);

		} catch (IOException | SBOLValidationException | SBOLConversionException e) {
			e.printStackTrace();
		}

		SBOLConversion sbolConv = new SBOLConversion();

		sbolConv.setSbolDoc(sbolDocs, weight);

		List<DesignSpace> outputSpaces = sbolConv.convertSBOLsToSpaces();

		DesignSpace output = new DesignSpace();
		for (DesignSpace outputSpace: outputSpaces){
			correctComponentIds(outputSpace);
			//outputSpace.splitEdges();

			if (save) {
				outputSpace.setGroupID(groupID);
				saveDesignSpace(outputSpace);
			}

			output = outputSpace;
		}
		
		return output;

	}
  
	public List<String> exportCombinatorial(String targetSpaceID, String namespace)
			throws SBOLValidationException, IOException, SBOLConversionException, SBOLException, URISyntaxException {

		DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
		SBOLGeneration sbolGenerator = new SBOLGeneration(targetSpace, namespace);
		return sbolGenerator.createSBOLDocument();
  }

	private void correctComponentIds(DesignSpace designSpace) {
		// Keeps only the part name in the Components Ids
		
		Set<Edge> edges = designSpace.getEdges();

		for (Edge edge : edges) {

			ArrayList<String> IDs = edge.getComponentIDs();

			for (int i = 0; i < IDs.size(); i++) {
				if (IDs.get(i).contains("constellationcad.org/")){
					String newID = IDs.get(i).substring(28, IDs.get(i).lastIndexOf('/'));
					IDs.set(i, newID);
				}
			}

			edge.setComponentIDs(IDs);			
		}

		//designSpace.printAllEdges();
	}

	public void importGoldbar(JSONObject goldbar, JSONObject categories, String outputSpacePrefix, 
			String groupID, Double weight, Boolean verbose) throws JSONException {

		GoldbarConversion goldbarConversion = new GoldbarConversion(goldbar, categories, weight, verbose);

		goldbarConversion.convert();

		NodeSpace outSpace = goldbarConversion.getSpace();

		DesignSpace outputSpace = new DesignSpace(outputSpacePrefix, groupID);

		outputSpace.shallowCopyNodeSpace(outSpace);

		saveDesignSpace(outputSpace);
	}

	public Map<String, Object> goldbarGeneration(ArrayList<String> rules, InputStream inputCSVStream, 
			ArrayList<String> lengths, String outputSpacePrefix, Boolean verify, String direction) {
		
		Map<String, Object> goldbarAndCategories = new HashMap<>();
		String spacePrefix;
		Map<String, String> g = new HashMap<>();
		String s = "";

		GoldbarGeneration goldbarGeneration = new GoldbarGeneration(rules, inputCSVStream, direction);

		if (rules.contains("N")) {
			g = goldbarGeneration.createRuleN(lengths);
		}

		if (rules.contains("B")) {
			g = goldbarGeneration.createRuleB();
		}

		if (rules.contains("R")) {
			g = goldbarGeneration.createRuleR();
		}

		if (rules.contains("I")) {
			g = goldbarGeneration.createRuleI();
		}
		
		if (rules.contains("M")) {
			g = goldbarGeneration.createRuleM();
		}

		if (rules.contains("O")) {
			g = goldbarGeneration.createRuleO();
		}

		if (rules.contains("L")) {
			s = goldbarGeneration.createRuleL();
		}

		if (rules.contains("P")) {
			s = goldbarGeneration.createRuleP();
		}

		if (rules.contains("T")) {
			g = goldbarGeneration.createRuleT();
		}

		if (rules.contains("E")) {
			g = goldbarGeneration.createRuleE();
		}

		if (rules.contains("goldbar")) {
			g = goldbarGeneration.createRuleGoldbar();
		}

		Map<String, String> goldbar = goldbarGeneration.getGoldbar();
		for (String key : goldbar.keySet()) {
			System.out.println("\nGOLDBAR: ");
			System.out.println("\n" + key + ": ");
			System.out.println(goldbar.get(key));
		}

		System.out.println("\nCategories: ");
		System.out.println(goldbarGeneration.getCategoriesString());

		goldbarAndCategories.put("goldbar", goldbar);
		goldbarAndCategories.put("categories", goldbarGeneration.getCategoriesString());

		return goldbarAndCategories;
	}

	private String constellationGoldbarRequest(String OutputSpacePrefix, String goldbar, String categories) {
		
		String responseBody = "Error";

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			// Create the POST request
            HttpPost postRequest = new HttpPost("http://localhost:8082/postSpecs");
            postRequest.setHeader("Content-Type", "application/json");

			String parameters = String.format(
				"{" +
				"\"designName\":" + "\"%1$s\"," +
				"\"specification\":" + "\"%2$s\"," +
				"\"categories\":" + "%3$s," +
				"\"numDesigns\":" + "\"1\"," +
				"\"maxCycles\":" + "\"1\"," +
				"\"number\":" + "\"2.0\"," +
				"\"name\":" + "\"specificationname\"," +
				"\"clientid\":" + "\"userid\"," +
				"\"representation\":" + "\"EDGE\"" +
			    "}",
				 OutputSpacePrefix, goldbar, categories);

			System.out.println(parameters);

			postRequest.setEntity(new StringEntity(parameters));

			try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                // Handle the response
                responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Status code: " + response.getStatusLine().getStatusCode());
                System.out.println("Response body: " + responseBody);

				// Parse the JSON response and extract the "sbol" portion
                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.has("sbol")) {
                    return jsonResponse.getString("sbol");
                } else {
                    return "SBOL not found in response";
                }
            }
		}  catch (Exception e) {
			e.printStackTrace();
		}

		return responseBody;
	}
    
    public void deleteBranch(String targetSpaceID, String targetBranchID) {
        branchRepository.deleteBranch(targetSpaceID, targetBranchID);
    }

    public void copyHeadBranch(String targetSpaceID, String outputBranchID) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);

    	Branch outputBranch = targetSpace.getHeadBranch().copy();
		outputBranch.setBranchID(outputBranchID);

		targetSpace.addBranch(outputBranch);
    	
    	saveDesignSpace(targetSpace);
    }

    public void checkoutBranch(String targetSpaceID, String targetBranchID) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
    	targetSpace.clearNodes();

    	targetSpace.setHeadBranch(targetSpace.getBranch(targetBranchID));

    	targetSpace.copyNodeSpace(targetSpace.getHeadSnapshot());
    	
    	saveDesignSpace(targetSpace);
    }

    public void commitToBranch(String targetSpaceID, String targetBranchID) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
    	Branch targetBranch = targetSpace.getBranch(targetBranchID);
    	
    	commitToBranch(targetSpace, targetBranch);
    }

    public void commitToHeadBranch(String targetSpaceID) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
    	commitToBranch(targetSpace, targetSpace.getHeadBranch());
    }
    
    private void commitToBranch(DesignSpace targetSpace, Branch targetBranch) {
    	Commit commit = targetSpace.createCommit(targetBranch);

		commit.createSnapshot().copyNodeSpace(targetSpace);

		targetBranch.setLatestCommit(commit);
    	
    	saveDesignSpace(targetSpace);
    }
    
    public void resetBranch(String targetSpaceID, String targetBranchID,
    		List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);

    	Branch targetBranch = targetSpace.getBranch(targetBranchID);

    	resetBranch(targetSpace, targetBranch, commitPath);
    }

    public void resetHeadBranch(String targetSpaceID, List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);

    	resetBranch(targetSpace, targetSpace.getHeadBranch(), commitPath);
    }

    private void resetBranch(DesignSpace targetSpace, Branch targetBranch,
                            List<String> commitPath) {
        if (targetBranch != null && targetBranch.getNumCommits() > 1) {
            Commit targetCommit = targetBranch.getLatestCommit();

            int i = 0;

            if (targetCommit.getCommitID().equals(commitPath.get(i))) {
                while (targetCommit != null && i + 1 < commitPath.size()) {
                    targetCommit =
                        targetCommit.findPredecessor(commitPath.get(i + 1));
                    i++;
                }

                if (targetCommit != null) {
                    targetBranch.setLatestCommit(targetCommit);

                    Set<Commit> diffCommits =
                        targetBranch.retainCommits(targetCommit.getHistory());

                    Set<Commit> deletedCommits = new HashSet<Commit>();

                    for (Commit diffCommit : diffCommits) {
                        if (!targetSpace.containsCommit(diffCommit)) {
                            deletedCommits.add(diffCommit);
                        }
                    }

                    if (deletedCommits.size() > 0) {
                        deleteCommits(deletedCommits);
                    }
                }
            }

            saveDesignSpace(targetSpace);
        }
    }
    
    public void revertBranch(String targetSpaceID, String targetBranchID,
    		List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);

    	Branch targetBranch = targetSpace.getBranch(targetBranchID);

    	revertBranch(targetSpace, targetBranch, commitPath);
    }

    public void revertHeadBranch(String targetSpaceID, List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);

    	revertBranch(targetSpace, targetSpace.getHeadBranch(), commitPath);
    }

    private void revertBranch(DesignSpace targetSpace, Branch targetBranch,
                             List<String> commitPath) {
        if (targetBranch != null && targetBranch.getNumCommits() > 1) {
            Commit targetCommit = targetBranch.getLatestCommit();

            int i = 0;

            while (targetCommit != null && i + 1 < commitPath.size()) {
                targetCommit =
                    targetCommit.findPredecessor(commitPath.get(i + 1));

                i++;
            }

            if (targetCommit != null) {
                Commit commitCopy = targetSpace.copyCommit(targetBranch, targetCommit);

                commitCopy.addPredecessor(targetBranch.getLatestCommit());

                targetBranch.setLatestCommit(commitCopy);
            }

            saveDesignSpace(targetSpace);
        }
    }

    public Map<String, Object> d3GraphBranches(String targetSpaceID) {
    	return mapBranchesToD3Format(branchRepository.mapBranches(targetSpaceID));
    }
    
    public List<String> listDesignSpaces() {
        return designSpaceRepository.listDesignSpaces();
    }

    public void deleteDesignSpace(String targetSpaceID) {
		System.out.println("\nDeleting Design Space: " + targetSpaceID + "\n");
        validateDesignSpaceOperator(targetSpaceID);

        designSpaceRepository.deleteDesignSpace(targetSpaceID);
    }

	public void deleteDesignSpaceGroup(String groupID) {
		System.out.println("\nDeleting Group: " + groupID + "\n");

		List<String> spaceIDs = designSpaceRepository.listDesignSpaces(groupID);

		for (String spaceID : spaceIDs) {
			System.out.println("\nDeleting Design Space: " + spaceID + "\n");

			validateDesignSpaceOperator(spaceID);

        	designSpaceRepository.deleteDesignSpace(spaceID);
		}
    }

	public void setGroupID(String targetSpaceID, String groupID) {
		validateDesignSpaceOperator(targetSpaceID);
		designSpaceRepository.setGroupID(targetSpaceID, groupID);
	}

	public String getGroupID(String targetSpaceID) {
		validateDesignSpaceOperator(targetSpaceID);
		return designSpaceRepository.getGroupID(targetSpaceID);
	}

	public Integer getGroupSize(String groupID) {
		return designSpaceRepository.getGroupIDSize(groupID);
	}

	public List<String> getGroupSpaceIDs(String groupID) {
		return designSpaceRepository.listDesignSpaces(groupID);
	}

	public List<String> getUniqueGroupIDs() {
		return designSpaceRepository.getUniqueGroupIDs();
	}

    public void createDesignSpace(String outputSpaceID) {
        validateGenerativeDesignSpaceOperator(outputSpaceID);

        designSpaceRepository.createDesignSpace(outputSpaceID);
    }

    public void createDesignSpace(String outputSpaceID, List<String> compIDs,
                                  List<String> compRoles) {
        validateGenerativeDesignSpaceOperator(outputSpaceID);

        designSpaceRepository.createDesignSpace(
            outputSpaceID, new ArrayList<String>(compIDs),
            new ArrayList<String>(compRoles));
    }

    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
		List<DesignSpaceEdgeDTO> designSpaceData = designSpaceRepository.mapDesignSpace(targetSpaceID);
		Map<String, Object> d3 = mapDesignSpaceToD3Format(designSpaceData);
        return reformatD3Graph(targetSpaceID, d3);
    }

	public Map<String, Object> reformatD3Graph(String targetSpaceID, Map<String, Object> d3) {
		List<Map<String, Object>> nodes = (List<Map<String, Object>>) d3.get("nodes");
		if (nodes.size() > 100) {
			Map<String, Object> d3_reformatted = new HashMap<String, Object>();
			List<Map<String, Object>> newNodes = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> newLinks = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> links = (List<Map<String, Object>>) d3.get("links");

			ArrayList<String> startNodeTypes = new ArrayList<String>();
			startNodeTypes.add("start");

			ArrayList<String> acceptNodeTypes = new ArrayList<String>();
			acceptNodeTypes.add("accept");

			newNodes.add(makeD3("id", targetSpaceID, "nodeTypes", startNodeTypes));
			newNodes.add(makeD3("id", "Total Nodes: " + nodes.size(), "nodeTypes", acceptNodeTypes));
			newNodes.add(makeD3("id", "Total Edges: " + links.size(), "nodeTypes", acceptNodeTypes));

			Map<String, Object> link1 = makeD3("source", 0, "target", 1);
			link1.put("componentRoles", new ArrayList<String>());
			link1.put("componentIDs", new ArrayList<String>());
			link1.put("weight", new ArrayList<Double>());

			Map<String, Object> link2 = makeD3("source", 0, "target", 2);
			link2.put("componentRoles", new ArrayList<String>());
			link2.put("componentIDs", new ArrayList<String>());
			link2.put("weight", new ArrayList<Double>());

			newLinks.add(link1);
			newLinks.add(link2);

			d3_reformatted.put("nodes", newNodes);
			d3_reformatted.put("links", newLinks);

			return d3_reformatted;
		}
		
		return d3;
	}
    
    public HashSet<List<Map<String, Object>>> enumerateDesignSpaceSet(String targetSpaceID, 
    		int numDesigns, int minLength, int maxLength, EnumerateType enumerateType, boolean isWeighted, boolean isSampleSpace, boolean printDesigns) {

    	DesignSpace designSpace = loadDesignSpace(targetSpaceID);
    	
        DesignSampler designSampler = new DesignSampler(designSpace);
        
		System.out.println("\nBegin Enumeration\n");
        HashSet<List<Map<String, Object>>> samplerOutput = designSampler.enumerateSet(numDesigns, minLength, maxLength, isSampleSpace, enumerateType);

		return designSampler.processEnumerateSet(samplerOutput, isWeighted, isSampleSpace, printDesigns);
    }

	public List<List<Map<String, Object>>> enumerateDesignSpaceList(String targetSpaceID, 
    		int numDesigns, int minLength, int maxLength, EnumerateType enumerateType, boolean isWeighted, boolean isSampleSpace, boolean printDesigns) {

    	DesignSpace designSpace = loadDesignSpace(targetSpaceID);
    	
        DesignSampler designSampler = new DesignSampler(designSpace);
        
		System.out.println("\nBegin Enumeration\n");
        List<List<Map<String, Object>>> samplerOutput = designSampler.enumerateList(numDesigns, minLength, maxLength, isSampleSpace, enumerateType);

		return designSampler.processEnumerateList(samplerOutput, isWeighted, isSampleSpace, printDesigns);
    }
    
    public List<String> getGraphScore(String targetSpaceID) {
    	
		long startTime = System.nanoTime();
    	DesignSpace designSpace = loadDesignSpace(targetSpaceID);

		designSpace.weightBlankEdges();
    	
		// Total Score of All Non-Blank Edges
        String graphScoreOfNonBlankEdges = designSpace.getTotalScoreOfNonBlankEdges();

		// Total Score of All Non-Blank Edges
        String graphScoreOfEdges = designSpace.getTotalScoreOfEdges();

		// Average Score of All Non-Blank Edges
		String avgScoreOfAllNonBlankEdges = designSpace.getAvgScoreofAllNonBlankEdges();

		// Average Score of All Non-Blank Edges
		String avgScoreOfAllEdges = designSpace.getAvgScoreofAllEdges();

		// Make List of Scores
		List<String> scores = new ArrayList<String>();
		scores.add(graphScoreOfNonBlankEdges);
		scores.add(graphScoreOfEdges);
		scores.add(avgScoreOfAllNonBlankEdges);
		scores.add(avgScoreOfAllEdges);
        
        long endTime = System.nanoTime();
    	long duration = (endTime - startTime);

        return scores;
    }

	public List<List<Map<String, Object>>> getBestPath(String targetSpaceID) {

		List<List<Map<String, Object>>> bestPath = new ArrayList<List<Map<String,Object>>>();
    	
		long startTime = System.nanoTime();
    	DesignSpace designSpace = loadDesignSpace(targetSpaceID);

		if (designSpace.isCyclic()) {
			System.out.println("isCyclic: True");
		
		} else {
			System.out.println("isCyclic: False");

			designSpace.weightBlankEdges();
			
			DesignAnalysis designAnalysis = new DesignAnalysis(designSpace);
    	
			bestPath = designAnalysis.getBestPath();
			
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
		}
        
        return bestPath;
    }

	public Map<String, Map<String, Object>> partAnalytics(String targetSpaceID) {

		DesignSpace designSpace = loadDesignSpace(targetSpaceID);

		DesignAnalysis designAnalysis = new DesignAnalysis(designSpace);

		Map<String, Map<String, Object>> thisPartAnalytics = designAnalysis.partAnalytics();

		System.out.println(thisPartAnalytics);

		return thisPartAnalytics;
	}

	public void weightDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, String groupID, int tolerance, int weightTolerance)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {

    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
		outputSpace.setGroupID(groupID);
    	
    	WeightOperator.apply(inputSpaces.get(0), inputSpaces.get(1), outputSpace, tolerance, weightTolerance);

    	saveDesignSpace(outputSpace);

	}

	public void reverseDesignSpace(String inputSpaceID, String outputSpaceID, String groupID, Boolean reverseOrientation)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {

		ArrayList<String> inputSpaceIDs = new ArrayList<>();
		inputSpaceIDs.add(inputSpaceID);

    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
		outputSpace.setGroupID(groupID);
    	
    	ReverseOperator.apply(inputSpaces.get(0), outputSpace, reverseOrientation);

    	saveDesignSpace(outputSpace);

	}
    
    public Set<List<String>> sampleDesignSpace(String targetSpaceID, int numDesigns, int minLength, int maxLength, boolean isWeighted, boolean positiveOnly, boolean isSampleSpace) {
    	DesignSpace designSpace = loadDesignSpace(targetSpaceID);

		if (isWeighted && !isSampleSpace) {
			designSpace.weightBlankEdges();
		}
    	
        DesignSampler designSampler = new DesignSampler(designSpace);
        
        return designSampler.sample(numDesigns, minLength, maxLength, isWeighted, positiveOnly, isSampleSpace);
    }

	public Boolean createSampleSpace(String targetSpaceID, String groupID) {
		DesignSpace inputSpace = loadDesignSpace(targetSpaceID);
		

		if (listDesignSpaces().contains(targetSpaceID + "_sampleSpace")) {
			return false;
		
		} else {
			DesignSpace outputSpace = new DesignSpace(targetSpaceID + "_sampleSpace", groupID);

			inputSpace.weightBlankEdges();

			double avgWeight = Double.parseDouble(inputSpace.getAvgScoreofAllNonBlankEdges());
			double stdev = inputSpace.getStdev();

			outputSpace.copyNodeSpace(inputSpace);

			for (Node node : outputSpace.getNodes()) {

				HashMap<Edge, ArrayList<Double>> edgeToWeights = new HashMap<>();


				System.out.println(node);

				double totalWeight = 0.0;

				for (Edge edge : node.getEdges()) {
					ArrayList<Double> scaledWeights = new ArrayList<>();

					double to_min = 0;
					double to_max = 1;
					double from_min = -3;
					double from_max = 3;

					for (Double weight : edge.getWeight()) {
						double x = (weight - avgWeight) / stdev;
						double y = (x - from_min) / (from_max - from_min) * (to_max - to_min) + to_min;
						totalWeight = totalWeight + y;

						scaledWeights.add(y);
					}

					edgeToWeights.put(edge, scaledWeights);
				}

				
				if (node.getEdges().size() == 1 && node.getEdges().iterator().next().getWeight().size() == 1) {
					ArrayList<Double> newWeights = new ArrayList<>();
					newWeights.add(1.0);
					node.getEdges().iterator().next().setWeight(newWeights);

				} else {

					for (Edge edge : edgeToWeights.keySet()) {
						ArrayList<Double> newWeights = new ArrayList<>();
						
						for (Double weight : edgeToWeights.get(edge)) {
							newWeights.add(weight / totalWeight);
						}

						edge.setWeight(newWeights);
					}
				}
			}

			saveDesignSpace(outputSpace);
			return true;
		}
	}

	// Utility which converts CSV to ArrayList using split operation
	public static ArrayList<String> csvToArrayList(String csvLine) {
		ArrayList<String> csvArray = new ArrayList<String>();

		if (csvLine != null) {
			String[] splitData = csvLine.split("\\s*,\\s*");
			for (int i = 0; i < splitData.length; i++) {
				if (!(splitData[i] == null) || !(splitData[i].length() == 0)) {
					csvArray.add(splitData[i].trim());
				}
			}
		}

		return csvArray;
	}
	
	private void deleteCommits(Set<Commit> deletedCommits) {
		Set<Snapshot> deletedSnapshots = new HashSet<Snapshot>();
		
		for (Commit deletedCommit : deletedCommits) {
			deletedSnapshots.add(deletedCommit.getSnapshot());
		}
		
		deleteSnapshots(deletedSnapshots);
		
		commitRepository.deleteAll(deletedCommits);
	}
	
	private void deleteNodes(Set<Node> deletedNodes) {
		nodeRepository.deleteAll(deletedNodes);
	}
	
	private void deleteSnapshots(Set<Snapshot> deletedSnapshots) {
		Set<Node> deletedNodes = new HashSet<Node>();
		
		for (Snapshot deletedSnapshot : deletedSnapshots) {
			deletedNodes.addAll(deletedSnapshot.getNodes());
		}
		
		deleteNodes(deletedNodes);
		
		snapshotRepository.deleteAll(deletedSnapshots);
	}
	
	private Branch findBranch(String targetSpaceID, String targetBranchID) {
		Set<Branch> targetBranches = branchRepository.findBranch(targetSpaceID, targetBranchID);
		if (targetBranches.size() > 0) {
			return targetBranches.iterator().next();
		} else {
			return null;
		}
	}

	private DesignSpace findDesignSpace(String targetSpaceID) {
    	return designSpaceRepository.findBySpaceID(targetSpaceID);
    }

	private DesignSpace loadDesignSpace(String targetSpaceID) {
		System.out.println("\nLoad Design Space\n");
		Long graphId = getDesignSpaceGraphID(targetSpaceID);
        DesignSpace targetSpace = null;
		if (graphId != null) {
            targetSpace = designSpaceRepository.findById(graphId).orElse(null);
        }

		// Set Tail Node for all Edges
		for (Node node : targetSpace.getNodes()) {
			for (Edge edge : node.getEdges()) {
				edge.setTail(node);
			}
		}


//      No version history
//		for (Commit commit : targetSpace.getCommits()) {
//			commit.setSnapshot(reloadSnapshot(commit.getSnapshot()));
//		}

		System.out.println("Design Space Loaded\n");

		return targetSpace;
	}

	private Snapshot reloadSnapshot(Snapshot snap) {
		Long graphId = snap.getGraphID();
        if (graphId != null) {
            return snapshotRepository.findById(graphId).orElse(null);
        }
        return null;
	}

	private Set<String> getBranchIDs(String targetSpaceID) {
		return branchRepository.getBranchIDs(targetSpaceID);
	}
	
	private Long getDesignSpaceGraphID(String targetSpaceID) {
		Set<Integer> graphIDs = designSpaceRepository.getDesignSpaceGraphID(targetSpaceID);
		
		if (graphIDs.size() > 0) {
			return new Long(graphIDs.iterator().next());
		} else {
			return null;
		}
	}
	
	public boolean hasBranch(String targetSpaceID, String targetBranchID) {
		return findBranch(targetSpaceID, targetBranchID) != null;
	}
	
	public boolean hasDesignSpace(String targetSpaceID) {
		return findDesignSpace(targetSpaceID) != null;
	}
	
	private Map<String, Object> mapBranchesToD3Format(List<Map<String, Object>> branchMap) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
	
	    if (branchMap.size() > 0) {
	    	List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	        List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	        
	        Map<Map<String, Object>, Integer> nodeAddresses = new HashMap<Map<String, Object>, Integer>();
	        Set<String> branchIDs = new HashSet<String>();
	    	
	    	d3Graph.put("spaceID", branchMap.get(0).get("spaceID"));
	    	
	    	Map<String, Object> tail = makeD3("knoxID", "head", "knoxClass", "Head");
	    	Map<String, Object> head = makeD3("knoxID", branchMap.get(0).get("headBranchID"), "knoxClass", "Branch");
	    	
	    	links.add(makeLink(tail, head, nodes, nodeAddresses));
	    	
	    	for (Map<String, Object> row : branchMap) {
	    		String tailID = (String) row.get("tailID");
	    		String headID = (String) row.get("headID");
	    		if (tailID != null && headID != null) {
	    			tail = makeD3("knoxID", row.get("tailID"), "knoxClass", "Commit");
	    			tail.put("copyIndex", row.get("tailCopyIndex"));
	    			head = makeD3("knoxID", row.get("headID"), "knoxClass", "Commit");
	    			head.put("copyIndex", row.get("headCopyIndex"));
	    			links.add(makeLink(tail, head, nodes, nodeAddresses));
	    		}
	
	    		String branchID = (String) row.get("branchID");
	    		if (!branchIDs.contains(branchID) && row.get("latestCommitID") != null) {
	    			branchIDs.add(branchID);
	    			tail = makeD3("knoxID", branchID, "knoxClass", "Branch");
	    			head = makeD3("knoxID", row.get("latestCommitID"), "knoxClass", "Commit");
	    			head.put("copyIndex", row.get("latestCopyIndex"));
	    			links.add(makeLink(tail, head, nodes, nodeAddresses));
	    		}
	    	}
	    	for (Map<String, Object> node : nodes) {
	    		node.remove("copyIndex");
	    	}
	    	d3Graph.putAll(makeD3("nodes", nodes, "links", links));
	    }
	    
	    return d3Graph;
	}

	private int locateD3Node(Map<String, Object> node, List<Map<String, Object>> nodes) {
		for (int i = 0; i < nodes.size(); i++) {
			if (node.containsKey("nodeID") && nodes.get(i).containsKey("nodeID")) {
				if (((String) node.get("nodeID")).equals((String) nodes.get(i).get("nodeID"))) {
					return i;
				}
			}	
		}
		
		return -1;
	}
	
	private Map<String, Object> mapDesignSpaceToD3Format(List<DesignSpaceEdgeDTO> spaceMap) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
		
	    List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	    
	    List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	    
	    int i = 0;
	    
	    for (DesignSpaceEdgeDTO row : spaceMap) {
	        if (d3Graph.isEmpty()) {
	        	d3Graph.put("spaceID", row.getSpaceID());
	        }

	        Map<String, Object> tail = makeD3("nodeID", row.getTailID(), "nodeTypes", row.getTailTypes());

	        int source = locateD3Node(tail, nodes);
	        
	        if (source == -1) {
	        	nodes.add(tail);
	        	
	        	source = i++;
	        }
	        
	        Map<String, Object> head = makeD3("nodeID", row.getHeadID(), "nodeTypes", row.getHeadTypes());
	       
	        int target = locateD3Node(head, nodes);
	        
	        if (target == -1) {
	        	nodes.add(head);
	        	
	        	target = i++;
	        }
	       
	        Map<String, Object> link = makeD3("source", source, "target", target);
	        
	        if (row.getComponentRoles() != null) {
	        	link.put("componentRoles", row.getComponentRoles());
	        }

	        if (row.getComponentIDs() != null) {
	        	link.put("componentIDs", row.getComponentIDs());
	        }

			if (row.getWeight() != null) {
	        	link.put("weight", row.getWeight());
	        }

			link.put("orientation", row.getOrientation());
	        
	        links.add(link);
	    }
	    
	    d3Graph.putAll(makeD3("nodes", nodes, "links", links));
	    
	    return d3Graph;
	}
	
	private Map<String, Object> mapDesignSpaceToD3Format(DesignSpace space) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
		
		Map<String, Integer> nodeIndices = new HashMap<String, Integer>();
		
	    List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	    
	    List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	    
	    d3Graph.put("spaceID", space.getSpaceID());
	    
	    for (Node node : space.getNodes()) {
	    	if (node.hasNodeType()) {
	    		nodes.add(makeD3("nodeID", node.getNodeID(), "nodeTypes", node.getNodeTypes()));
	    	} else {
	    		nodes.add(makeD3("nodeID", node.getNodeID()));
	    	}
	    	
	    	nodeIndices.put(node.getNodeID(), new Integer(nodes.size()));
	    }
	    
	    for (Node node : space.getNodes()) {
	    	if (node.hasEdges()) {
	    		for (Edge edge : node.getEdges()) {
	    			ArrayList<String> compRoles;
	    			
	    			if (edge.hasComponentRoles()) {
	    				compRoles = edge.getComponentRoles();
	    			} else {
	    				compRoles = new ArrayList<String>();
	    			}
	    			
	    			Map<String, Object> link = makeD3("source", nodeIndices.get(edge.getTail().getNodeID()), 
	    					"target", nodeIndices.get(edge.getHead().getNodeID()));
	    			
	    			link.put("componentRoles", compRoles.toArray(new String[compRoles.size()]));
	    			
	    			links.add(link);
	    		}
	    	}
	    }
	    
	    d3Graph.putAll(makeD3("nodes", nodes, "links", links));
	    
	    return d3Graph;
	}

	private Map<String, Object> makeD3(String key1, Object value1, String key2, Object value2) {
	    Map<String, Object> result = new HashMap<String, Object>();
	    
	    result.put(key1, value1);
	    
	    result.put(key2, value2);
	    
	    return result;
	}
	
	private Map<String, Object> makeD3(String key, Object value) {
	    Map<String, Object> result = new HashMap<String, Object>();
	    
	    result.put(key, value);
	    
	    return result;
	}

	private Map<String, Object> makeLink(Map<String, Object> tail, Map<String, Object> head, List<Map<String,Object>> nodes, Map<Map<String, Object>, Integer> nodeAddresses) {
		 int source;
	     if (nodeAddresses.containsKey(tail)) {
	     	source = nodeAddresses.get(tail);
	     } else {
	     	source = nodes.size();
	     	nodes.add(tail);
	     	nodeAddresses.put(tail, source);
	     }
	     int target;
	     if (nodeAddresses.containsKey(head)) {
	     	target = nodeAddresses.get(head);
	     } else {
	     	target = nodes.size();
	     	nodes.add(head);
	     	nodeAddresses.put(head, target);
	     }
	     return makeD3("source", source, "target", target);
	}
	
	private void saveDesignSpace(DesignSpace space) {

		HashMap<String, Set<Edge>> nodeIDToEdges = space.mapNodeIDsToEdges();

		space.clearEdges();
		
//      No version history
//		Set<Commit> commits = space.getCommits();
//
//		HashMap<String, HashMap<String, Set<Edge>>> commitIDToEdges = new HashMap<String, HashMap<String, Set<Edge>>>();
//
//		for (Commit commit : commits) {
//			commitIDToEdges.put(commit.getCommitID(), 
//					commit.getSnapshot().mapNodeIDsToEdges());
//
//			commit.getSnapshot().clearEdges();
//		}
		
		designSpaceRepository.save(space);

		space.loadEdges(nodeIDToEdges);
		
//      No version history
//		for (Commit commit : commits) {
//			commit.getSnapshot().loadEdges(commitIDToEdges.get(commit.getCommitID()));
//
//		}
		System.out.println("\nSaving DesignSpace!\n");
		
		designSpaceRepository.save(space);
	}
	
	private String convertCSVRole(String csvRole) {
		switch (csvRole) {
		case "cds":
			return "http://identifiers.org/so/SO:0000316";
		case "promoter":
			return "http://identifiers.org/so/SO:0000167";
		case "ribosomeBindingSite":
			return "http://identifiers.org/so/SO:0000139";
		case "terminator":
			return "http://identifiers.org/so/SO:0000141";
		default:
			return "http://knox.org/role/" + csvRole;
		}
	}

    private void validateListParameter(String parameterName, List<String> parameter)
        throws ParameterEmptyException {
        if (parameter.isEmpty()) {
            throw new ParameterEmptyException(parameterName);
        }
    }

    private void validateDesignSpaceOperator(String targetSpaceID) {
        if (!hasDesignSpace(targetSpaceID)) {
            throw new DesignSpaceNotFoundException(targetSpaceID);
        }
    }

    private void validateGenerativeDesignSpaceOperator(String outputSpaceID) {
        if (hasDesignSpace(outputSpaceID)) {
            throw new DesignSpaceConflictException(outputSpaceID);
        }
    }

    private void validateCombinationalDesignSpaceOperator(
        List<String> inputSpaceIDs, String outputSpaceID)
        throws ParameterEmptyException, DesignSpaceNotFoundException,
               DesignSpaceConflictException,
               DesignSpaceBranchesConflictException {
        if (inputSpaceIDs.size() == 0) {
            throw new ParameterEmptyException("inputSpaceIDs");
        }

        if (outputSpaceID.length() == 0) {
            throw new ParameterEmptyException("outputSpaceID");
        }

        for (String inputSpaceID : inputSpaceIDs) {
            if (!hasDesignSpace(inputSpaceID)) {
                throw new DesignSpaceNotFoundException(inputSpaceID);
            }
        }

        if (!inputSpaceIDs.contains(outputSpaceID) &&
            hasDesignSpace(outputSpaceID)) {
            throw new DesignSpaceConflictException(outputSpaceID);
        }

//        Set<String> conflictingSpaceIDs = new HashSet<String>();

//        Set<String> conflictingBranchIDs = new HashSet<String>();
//
//        HashMap<String, String> branchIDToSpaceID =
//            new HashMap<String, String>();
//
//        for (String inputSpaceID : inputSpaceIDs) {
//            for (String branchID : getBranchIDs(inputSpaceID)) {
//                if (!branchIDToSpaceID.containsKey(branchID)) {
//                    branchIDToSpaceID.put(branchID, inputSpaceID);
//                } else if (!branchIDToSpaceID.get(branchID).equals(
//                               inputSpaceID)) {
//                    conflictingSpaceIDs.add(branchIDToSpaceID.get(branchID));
//
//                    conflictingSpaceIDs.add(inputSpaceID);
//
//                    conflictingBranchIDs.add(branchID);
//                }
//            }
//        }
//
//        if (conflictingBranchIDs.size() > 0) {
//            throw new DesignSpaceBranchesConflictException(conflictingSpaceIDs, 
//            		conflictingBranchIDs);
//        }
    }

    private void printSpace(DesignSpace d) {
        System.out.println(d.getSpaceID());
        for (Node n : d.getNodes()) {
            System.out.println(n.getNodeID());
            if (n.hasEdges()) {
                for (Edge e : n.getEdges()) {
                    System.out.println(e.getTail().getNodeID() + "-" +
                                       e.getHead().getNodeID());
                }
            }
        }
    }
}
