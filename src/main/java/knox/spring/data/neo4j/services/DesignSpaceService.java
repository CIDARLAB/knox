package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.Commit;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Snapshot;
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
import knox.spring.data.neo4j.repositories.BranchRepository;
import knox.spring.data.neo4j.repositories.CommitRepository;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;
import knox.spring.data.neo4j.repositories.EdgeRepository;
import knox.spring.data.neo4j.repositories.NodeRepository;
import knox.spring.data.neo4j.repositories.SnapshotRepository;
import knox.spring.data.neo4j.sample.DesignSampler;
import knox.spring.data.neo4j.sample.DesignSampler.EnumerateType;
import knox.spring.data.neo4j.sbol.SBOLConversion;

import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceOntology;
import org.sbolstandard.core2.SBOLDocument;
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
    
    public void joinDesignSpaces(List<String> inputSpaceIDs) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	joinDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0));
    }
    
    public void joinDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);

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
    
    public void orDesignSpaces(List<String> inputSpaceIDs) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	orDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0));
    }
    
    public void orDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	List<NodeSpace> inputSpaces = new LinkedList<NodeSpace>();
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
    	
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
	
	public void repeatDesignSpaces(List<String> inputSpaceIDs, boolean isOptional) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	repeatDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), isOptional);
    }
    
    public void repeatDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, boolean isOptional) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	List<NodeSpace> inputSpaces = new LinkedList<NodeSpace>();
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
    	
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
    
    public void andDesignSpaces(List<String> inputSpaceIDs, int tolerance, boolean isComplete,
    		Set<String> roles) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    	    DesignSpaceConflictException, DesignSpaceBranchesConflictException{
    	andDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), tolerance, isComplete, roles);
    }
    
    public void andDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, int tolerance,
    		boolean isComplete, Set<String> roles)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
//    	long startTime = System.nanoTime();
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);

    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
    	
    	ANDOperator.apply(inputSpaces, outputSpace, tolerance, isComplete, roles);
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

        ANDOperator.apply(inputSnaps, outputSnap, tolerance, isComplete, roles);
        
        saveDesignSpace(targetSpace);
    }
	
	public void mergeDesignSpaces(List<String> inputSpaceIDs, int tolerance, Set<String> roles) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    	    DesignSpaceConflictException, DesignSpaceBranchesConflictException{
		mergeDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), tolerance, roles);
    }
    
    public void mergeDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, int tolerance, 
    		Set<String> roles)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, 
    		DesignSpaceConflictException, DesignSpaceBranchesConflictException {
//    	long startTime = System.nanoTime();
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);

    	List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(inputSpaceIDs.size());
    	
    	DesignSpace outputSpace = loadIOSpaces(inputSpaceIDs, outputSpaceID, inputSpaces);
    	
    	MergeOperator.apply(inputSpaces, outputSpace, tolerance, roles);
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
    
    public void mergeBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		int tolerance, Set<String> roles) {
    	mergeBranches(targetSpaceID, inputBranchIDs, inputBranchIDs.get(0), tolerance, roles);
    }

    public void mergeBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		String outputBranchID, int tolerance, Set<String> roles) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID);
    	
        List<Branch> inputBranches = new ArrayList<Branch>(inputBranchIDs.size());
        
        Branch outputBranch = loadIOBranches(targetSpace, inputBranchIDs, outputBranchID,
        		inputBranches);
        
        List<NodeSpace> inputSnaps = new ArrayList<NodeSpace>(inputBranches.size());
        
        NodeSpace outputSnap = mergeVersions(targetSpace, inputBranches, outputBranch, 
        		inputSnaps);

        MergeOperator.apply(inputSnaps, outputSnap, tolerance, roles);
        
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

    public void importCSV(List<InputStream> inputCSVStreams, String outputSpacePrefix, 
    		boolean isMerge) {
    	List<BufferedReader> designReaders = new LinkedList<BufferedReader>();
    	
    	List<BufferedReader> compReaders = new LinkedList<BufferedReader>();
    	
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
    	
    	List<NodeSpace> csvSpaces = new LinkedList<NodeSpace>();
    	
    	for (BufferedReader designReader : designReaders) {
    		try {
    			csvSpaces.addAll(processCSVDesigns(designReader, outputSpacePrefix, compIDToRole));
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
    		DesignSpace outputSpace = new DesignSpace(outputSpacePrefix);
    		
    		if (isMerge) {
    			MergeOperator.apply(csvSpaces, outputSpace, 0, new HashSet<String>());

    			saveDesignSpace(outputSpace);
    		} else {
    			OROperator.apply(csvSpaces, outputSpace);
    			
    			saveDesignSpace(outputSpace);
    		}
    	}
    }
    
    public List<DesignSpace> processCSVDesigns(BufferedReader csvReader, String outputSpacePrefix, 
    		HashMap<String, String> compIDToRole) throws IOException {
    	List<DesignSpace> csvSpaces = new LinkedList<DesignSpace>();
    	
    	SequenceOntology so = new SequenceOntology();
    	
    	String csvLine;
    	
		int j = -1;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);

			if (csvArray.size() > 0 && csvArray.get(0).length() > 0) {
				j++;

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

						outputPredecessor.createEdge(outputNode, compIDs, compRoles);

						outputPredecessor = outputNode;
					}
				}

				csvSpaces.add(outputSpace);
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
    
    public void importSBOL(List<SBOLDocument> sbolDocs, String outputSpaceID) 
    		throws SBOLValidationException, IOException, SBOLConversionException, SBOLException {
    	SBOLConversion sbolConv = new SBOLConversion();

    	sbolConv.setSbolDoc(sbolDocs);

    	List<DesignSpace> outputSpaces = sbolConv.convertSBOLsToSpaces();

		for (DesignSpace outputSpace: outputSpaces){
			saveDesignSpace(outputSpace);
		}
    }
    
    public void deleteBranch(String targetSpaceID, String targetBranchID) {
        designSpaceRepository.deleteBranch(targetSpaceID, targetBranchID);
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
    	return mapBranchesToD3Format(designSpaceRepository.mapBranches(targetSpaceID));
    }
    
    public List<String> listDesignSpaces() {
        return designSpaceRepository.listDesignSpaces();
    }

    public void deleteDesignSpace(String targetSpaceID) {
        validateDesignSpaceOperator(targetSpaceID);

        designSpaceRepository.deleteDesignSpace(targetSpaceID);
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
        return mapDesignSpaceToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public List<List<Map<String, Object>>> enumerateDesignSpace(String targetSpaceID, 
    		int numDesigns, int minLength, int maxLength, EnumerateType enumerateType) {
    	long startTime = System.nanoTime();
    	DesignSpace designSpace = loadDesignSpace(targetSpaceID);
    	
        DesignSampler designSampler = new DesignSampler(designSpace);
        
        List<List<Map<String, Object>>> samplerOutput = designSampler.enumerate(numDesigns, minLength, maxLength, enumerateType);
        
        long endTime = System.nanoTime();
    	long duration = (endTime - startTime);
//    	LOG.info("ENUMERATE TIME: " + duration);
        
        return samplerOutput;
    }
    
    public Set<List<String>> sampleDesignSpace(String targetSpaceID, int numDesigns) {
    	DesignSpace designSpace = loadDesignSpace(targetSpaceID);
    	
        DesignSampler designSampler = new DesignSampler(designSpace);
        
        return designSampler.sample(numDesigns);
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
		
		commitRepository.delete(deletedCommits);
	}
	
	private void deleteNodes(Set<Node> deletedNodes) {
		nodeRepository.delete(deletedNodes);
	}
	
	private void deleteSnapshots(Set<Snapshot> deletedSnapshots) {
		Set<Node> deletedNodes = new HashSet<Node>();
		
		for (Snapshot deletedSnapshot : deletedSnapshots) {
			deletedNodes.addAll(deletedSnapshot.getNodes());
		}
		
		deleteNodes(deletedNodes);
		
		snapshotRepository.delete(deletedSnapshots);
	}
	
	private Branch findBranch(String targetSpaceID, String targetBranchID) {
		Set<Branch> targetBranches = designSpaceRepository.findBranch(targetSpaceID, targetBranchID);
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
		DesignSpace targetSpace = designSpaceRepository.findOne(getDesignSpaceGraphID(targetSpaceID), 3);

//      No version history
//		for (Commit commit : targetSpace.getCommits()) {
//			commit.setSnapshot(reloadSnapshot(commit.getSnapshot()));
//		}

		return targetSpace;
	}

	private Snapshot reloadSnapshot(Snapshot snap) {
		return snapshotRepository.findOne(snap.getGraphID(), 2);
	}

	private Set<String> getBranchIDs(String targetSpaceID) {
		return designSpaceRepository.getBranchIDs(targetSpaceID);
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
	
	private Map<String, Object> mapDesignSpaceToD3Format(List<Map<String, Object>> spaceMap) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
		
	    List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	    
	    List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	    
	    int i = 0;
	    
	    for (Map<String, Object> row : spaceMap) {
	        if (d3Graph.isEmpty()) {
	        	d3Graph.put("spaceID", row.get("spaceID"));
	        }
	        
	        Map<String, Object> tail = makeD3("nodeID", row.get("tailID"), "nodeTypes", row.get("tailTypes"));
	        
	        int source = locateD3Node(tail, nodes);
	        
	        if (source == -1) {
	        	nodes.add(tail);
	        	
	        	source = i++;
	        }
	        
	        Map<String, Object> head = makeD3("nodeID", row.get("headID"), "nodeTypes", row.get("headTypes"));
	       
	        int target = locateD3Node(head, nodes);
	        
	        if (target == -1) {
	        	nodes.add(head);
	        	
	        	target = i++;
	        }
	       
	        Map<String, Object> link = makeD3("source", source, "target", target);
	        
	        if (row.containsKey("componentRoles") && row.get("componentRoles") != null) {
	        	link.put("componentRoles", row.get("componentRoles"));
	        }
	        
	        if (row.containsKey("componentIDs") && row.get("componentIDs") != null) {
	        	link.put("componentIDs", row.get("componentIDs"));
	        }

			link.put("orientation", row.get("orientation"));
	        
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
