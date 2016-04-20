package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.Commit;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Snapshot;
import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.NodeNotFoundException;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

@Service
@Transactional
public class DesignSpaceService {

    @Autowired DesignSpaceRepository designSpaceRepository;
    
    public static final String RESERVED_ID = "knox";
    
    public void deleteBranch(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteBranch(targetSpaceID, targetBranchID);
	}
    
    public void copyHeadBranch(String targetSpaceID, String outputBranchID) {
    	designSpaceRepository.copyHeadBranch(targetSpaceID, outputBranchID);
    }
    
    public void andBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	Branch inputBranch1 = targetSpace.findBranch(inputBranchID1);
    	Branch inputBranch2 = targetSpace.findBranch(inputBranchID2);
    	
    	Branch outputBranch = targetSpace.createBranch(outputBranchID, 
				Math.max(inputBranch1.getIDIndex(), inputBranch2.getIDIndex()));
    	
    	Commit inputCommit1 = inputBranch1.getLatestCommit();
    	Commit inputCommit2 = inputBranch2.getLatestCommit();
    	
    	outputBranch.addCommit(inputCommit1);
    	outputBranch.addCommit(inputCommit2);
    	
    	Commit outputCommit = outputBranch.createCommit();
    	
    	outputCommit.addPredecessor(inputCommit1);
    	outputCommit.addPredecessor(inputCommit2);
    	
    	Snapshot outputSnapshot = outputCommit.createSnapshot();
    	outputSnapshot.setIDIndex(0);
    	
    	Node startNode1 = inputCommit1.getSnapshot().getStartNode();
    	Node startNode2 = inputCommit2.getSnapshot().getStartNode();
    	
    	HashMap<String, Set<Node>> inputToOutputNodes1 = new HashMap<String, Set<Node>>();
		HashMap<String, Set<Node>> inputToOutputNodes2 = new HashMap<String, Set<Node>>();
    	if (startNode1 != null && startNode2 != null) {
    		mergeNodeSpaces(startNode1, startNode2, outputSnapshot, new HashMap<String, Node>(), 
    				inputToOutputNodes1, inputToOutputNodes2, true);
    	}
    	
    	designSpaceRepository.save(targetSpace);
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void checkoutBranch(String targetSpaceID, String targetBranchID) {
    	deleteNodes(targetSpaceID);
    	designSpaceRepository.checkoutBranch(targetSpaceID, targetBranchID);
    	deleteNodeCopyIndices(targetSpaceID);
    }
    
    public void commitToHeadBranch(String targetSpaceID) {
    	String headBranchID = getHeadBranchID(targetSpaceID);
    	createCommit(targetSpaceID, headBranchID);
    	copyDesignSpaceToSnapshot(targetSpaceID, headBranchID);
    }
    
    public Map<String, Object> d3GraphBranches(String targetSpaceID) {
    	return mapBranchesToD3Format(designSpaceRepository.mapBranches(targetSpaceID));
    }
    
    public void insertBranch(String targetSpaceID, String inputBranchID1, String inputBranchID2, String targetNodeID, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);

    	Node startNode1 = getStartNode(targetSpaceID, outputBranchID);
    	Set<Node> acceptNodes1 = getAcceptNodes(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	Node nodeCopy = findNodeCopy(targetSpaceID, inputBranchID2, targetNodeID, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	if (nodeCopy != null) {
    		Set<Edge> removedEdges = removeOutgoingEdges(targetSpaceID, outputBranchID, nodeCopy.getNodeID());

    		if (removedEdges.size() > 0) {
    			for (Node acceptNode1 : acceptNodes1) {
    				deleteNodeType(targetSpaceID, outputBranchID, acceptNode1.getNodeID());
    			}
    		} else {
    			Set<Node> acceptNodes = getAcceptNodes(targetSpaceID, outputBranchID);
    			for (Node acceptNode : acceptNodes) {
    				if (nodeCopy.getNodeID().equals(acceptNode.getNodeID())) {
    					deleteNodeType(targetSpaceID, outputBranchID, nodeCopy.getNodeID());
    				}
    			}
    		}
    		if (startNode1 != null) {
    			deleteNodeType(targetSpaceID, outputBranchID, startNode1.getNodeID());
    			createEdge(targetSpaceID, outputBranchID, nodeCopy.getNodeID(), startNode1.getNodeID());
    		}
    		for (Node acceptNode1 : acceptNodes1) {
    			for (Edge removedEdge : removedEdges) {
    				if (removedEdge.hasComponentIDs() && removedEdge.hasComponentRoles()) {
    					createComponentEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    				} else {
    					createEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID());
    				}
    			}
    		}
    	}
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void joinBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);
    	
    	Node startNode1 = getStartNode(targetSpaceID, outputBranchID);
    	Set<Node> acceptNodes1 = getAcceptNodes(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	if (startNode1 != null) {
    		for (Node startNode : getStartNodes(targetSpaceID, outputBranchID)) {
    			if (!startNode.getNodeID().equals(startNode1.getNodeID())) {
    				deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
    				for (Node acceptNode1 : acceptNodes1) {
    					deleteNodeType(targetSpaceID, outputBranchID, acceptNode1.getNodeID());
    					createEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), startNode.getNodeID());
    				}
    			}
    		}
    	}
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void mergeBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	Branch inputBranch1 = targetSpace.findBranch(inputBranchID1);
    	Branch inputBranch2 = targetSpace.findBranch(inputBranchID2);
    	
    	Branch outputBranch = targetSpace.createBranch(outputBranchID, 
				Math.max(inputBranch1.getIDIndex(), inputBranch2.getIDIndex()));
    	
    	Commit inputCommit1 = inputBranch1.getLatestCommit();
    	Commit inputCommit2 = inputBranch2.getLatestCommit();
    	
    	outputBranch.addCommit(inputCommit1);
    	outputBranch.addCommit(inputCommit2);
    	
    	Commit outputCommit = outputBranch.createCommit();
    	
    	outputCommit.addPredecessor(inputCommit1);
    	outputCommit.addPredecessor(inputCommit2);
    	
    	Snapshot outputSnapshot = outputCommit.createSnapshot();
    	outputSnapshot.setIDIndex(0);
    	
    	Node startNode1 = inputCommit1.getSnapshot().getStartNode();
    	Node startNode2 = inputCommit2.getSnapshot().getStartNode();
    	
    	HashMap<String, Set<Node>> inputToOutputNodes1 = new HashMap<String, Set<Node>>();
		HashMap<String, Set<Node>> inputToOutputNodes2 = new HashMap<String, Set<Node>>();
    	if (startNode1 != null && startNode2 != null) {
    		mergeNodeSpaces(startNode1, startNode2, outputSnapshot, new HashMap<String, Node>(), 
    				inputToOutputNodes1, inputToOutputNodes2, false);
    	}
   
    	if (startNode1 != null) {
    		Set<Node> visitedNodes1 = new HashSet<Node>();
    		visitedNodes1.add(startNode1);
    		mergeNodeSpace(startNode1, outputSnapshot, inputToOutputNodes1, 
    				new HashMap<String, Node>(), visitedNodes1);
    	}

    	if (startNode2 != null) {
    		Set<Node> visitedNodes2 = new HashSet<Node>();
    		visitedNodes2.add(startNode2);
    		mergeNodeSpace(startNode2, outputSnapshot, inputToOutputNodes2, 
    				new HashMap<String, Node>(), visitedNodes2);
    	}
    	
    	designSpaceRepository.save(targetSpace);
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void orBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    
    	Node startNodePrime = createTypedNode(targetSpaceID, outputBranchID, Node.NodeType.START.getValue());
    	
    	for (Node startNode : getStartNodes(targetSpaceID, outputBranchID)) {
    		if (!startNode.getNodeID().equals(startNodePrime.getNodeID())) {
    			deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
        		createEdge(targetSpaceID, outputBranchID, startNodePrime.getNodeID(), startNode.getNodeID());
    		}
    	}
        
    	Node acceptNodePrime = createTypedNode(targetSpaceID, outputBranchID, Node.NodeType.ACCEPT.getValue());
    	
    	for (Node acceptNode : getAcceptNodes(targetSpaceID, outputBranchID)) {
    		if (!acceptNode.getNodeID().equals(acceptNodePrime.getNodeID())) {
    			deleteNodeType(targetSpaceID, outputBranchID, acceptNode.getNodeID());
    			createEdge(targetSpaceID, outputBranchID, acceptNode.getNodeID(), acceptNodePrime.getNodeID());
    		}
    	}
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void resetHeadBranch(String targetSpaceID, String targetCommitID) {
    	validateDesignSpaceOperator(targetSpaceID);
    	designSpaceRepository.resetHeadBranch(targetSpaceID, targetCommitID);
    }
   
    public void deleteDesignSpace(String targetSpaceID) {
    	validateDesignSpaceOperator(targetSpaceID);
    	
    	designSpaceRepository.deleteDesignSpace(targetSpaceID);
    }
    
    public void createDesignSpace(String outputSpaceID) {
    	validateGenerativeDesignSpaceOperator(outputSpaceID);
    	
    	designSpaceRepository.createDesignSpace(outputSpaceID);
    }
    
    public void andDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	DesignSpace inputSpace1 = loadDesignSpace(inputSpaceID1, 2);
    	DesignSpace inputSpace2 = loadDesignSpace(inputSpaceID2, 2);
    	
    	DesignSpace outputSpace;
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		outputSpace = inputSpace1;
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		outputSpace = inputSpace2; 
    	} else {
    		outputSpace = new DesignSpace(outputSpaceID, 0, 
    				Math.max(inputSpace1.getMergeIndex(), inputSpace1.getMergeIndex()));
    	}
    	
    	Node startNode1 = inputSpace1.getStartNode();
    	Node startNode2 = inputSpace2.getStartNode();
    	
    	if (startNode1 != null && startNode2 != null) {
    		mergeNodeSpaces(startNode1, startNode2, outputSpace, new HashMap<String, Node>(), 
    				new HashMap<String, Set<Node>>(), new HashMap<String, Set<Node>>(), true);
    	}
    	
    	designSpaceRepository.save(outputSpace);
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}

    	andBranches(outputSpaceID, headBranchID1, headBranchID2, null);
    	
    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return mapDesignSpaceToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public void insertDesignSpace(String inputSpaceID1, String inputSpaceID2, String targetNodeID, String outputSpaceID)
    		throws NodeNotFoundException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateNodeOperator(inputSpaceID1, targetNodeID);
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	Node startNode2 = null;
    	Set<Node> acceptNodes2 = new HashSet<Node>();

    	Node targetNode;

    	if (outputSpaceID.equals(inputSpaceID1)) {
    		Node startNode1 = getStartNode(outputSpaceID);
    		Set<Node> acceptNodes1 = getAcceptNodes(outputSpaceID);

    		unionDesignSpace(inputSpaceID2, outputSpaceID);

    		for (Node startNode : getStartNodes(outputSpaceID)) {
    			if (!startNode1.equals(startNode)) {
    				startNode2 = startNode;
    			}
    		}
    		for (Node acceptNode : getAcceptNodes(outputSpaceID)) {
    			if (!acceptNodes1.contains(acceptNode)) {
    				acceptNodes2.add(acceptNode);
    			}
    		}

    		targetNode = findNode(outputSpaceID, targetNodeID);
    	} else {
    		if (!outputSpaceID.equals(inputSpaceID2)) {
    			unionDesignSpace(inputSpaceID2, outputSpaceID);
    		}

    		startNode2 = getStartNode(outputSpaceID);
    		acceptNodes2.addAll(getAcceptNodes(outputSpaceID));

    		unionDesignSpace(inputSpaceID1, outputSpaceID);

    		targetNode = findNodeCopy(inputSpaceID1, targetNodeID, outputSpaceID);
    	}

    	deleteNodeCopyIndices(outputSpaceID);

    	Set<Edge> removedEdges = removeOutgoingEdges(outputSpaceID, targetNode.getNodeID());

    	if (removedEdges.size() > 0) {
    		for (Node acceptNode2 : acceptNodes2) {
    			deleteNodeType(outputSpaceID, acceptNode2.getNodeID());
    		}
    	} 

    	if (startNode2 != null) {
    		deleteNodeType(outputSpaceID, startNode2.getNodeID());
    		createEdge(outputSpaceID, targetNode.getNodeID(), startNode2.getNodeID());
    	}
    	for (Node acceptNode2 : acceptNodes2) {
    		for (Edge removedEdge : removedEdges) {
    			if (removedEdge.hasComponentIDs() && removedEdge.hasComponentRoles()) {
    				createComponentEdge(outputSpaceID, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    			} else {
    				createEdge(outputSpaceID, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID());
    			}
    		}
    	}

    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}

    	insertBranch(outputSpaceID, headBranchID1, headBranchID2, targetNodeID, null);

    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    public void joinDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) 
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	Set<Node> acceptNodes1 = new HashSet<Node>();
    	Node startNode2 = null;
    	
    	if (outputSpaceID.equals(inputSpaceID2)) {
    		startNode2 = getStartNode(outputSpaceID);
    		Set<Node> acceptNodes2 = getAcceptNodes(outputSpaceID);
    		
    		unionDesignSpace(inputSpaceID1, outputSpaceID);
    		
    		for (Node acceptNode : getAcceptNodes(outputSpaceID)) {
    			if (!acceptNodes2.contains(acceptNode)) {
    				acceptNodes1.add(acceptNode);
    			}
    		}
    	} else {
    		if (!outputSpaceID.equals(inputSpaceID1)) {
        		unionDesignSpace(inputSpaceID1, outputSpaceID);
        	}
    		
    		Node startNode1 = getStartNode(outputSpaceID);
        	acceptNodes1.addAll(getAcceptNodes(outputSpaceID));
    		
    		unionDesignSpace(inputSpaceID2, outputSpaceID);

    		for (Node startNode : getStartNodes(outputSpaceID)) {
    			if (!startNode1.equals(startNode)) {
    				startNode2 = startNode;
    			}
    		}	
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	if (startNode2 != null) {
    		deleteNodeType(outputSpaceID, startNode2.getNodeID());
    		for (Node acceptNode1 : acceptNodes1) {
    			deleteNodeType(outputSpaceID, acceptNode1.getNodeID());
    			createEdge(outputSpaceID, acceptNode1.getNodeID(), startNode2.getNodeID());
    		}
    	}

    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}

    	joinBranches(outputSpaceID, headBranchID1, headBranchID2, null);
    	
    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    public void mergeDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) 
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	DesignSpace inputSpace1 = loadDesignSpace(inputSpaceID1, 2);
    	DesignSpace inputSpace2 = loadDesignSpace(inputSpaceID2, 2);
    	
    	DesignSpace outputSpace;
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		outputSpace = inputSpace1;
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		outputSpace = inputSpace2; 
    	} else {
    		outputSpace = new DesignSpace(outputSpaceID, 0, 
    				Math.max(inputSpace1.getMergeIndex(), inputSpace1.getMergeIndex()));
    	}
    	
    	Node startNode1 = inputSpace1.getStartNode();
    	Node startNode2 = inputSpace2.getStartNode();
    	
    	HashMap<String, Set<Node>> inputToOutputNodes1 = new HashMap<String, Set<Node>>();
		HashMap<String, Set<Node>> inputToOutputNodes2 = new HashMap<String, Set<Node>>();
    	if (startNode1 != null && startNode2 != null) {
    		mergeNodeSpaces(startNode1, startNode2, outputSpace, new HashMap<String, Node>(), 
    				inputToOutputNodes1, inputToOutputNodes2, false);
    	}
   
    	if (startNode1 != null) {
    		Set<Node> visitedNodes1 = new HashSet<Node>();
    		visitedNodes1.add(startNode1);
    		mergeNodeSpace(startNode1, outputSpace, inputToOutputNodes1, 
    				new HashMap<String, Node>(), visitedNodes1);
    	}

    	if (startNode2 != null) {
    		Set<Node> visitedNodes2 = new HashSet<Node>();
    		visitedNodes2.add(startNode2);
    		mergeNodeSpace(startNode2, outputSpace, inputToOutputNodes2, 
    				new HashMap<String, Node>(), visitedNodes2);
    	}
    	
    	designSpaceRepository.save(outputSpace);
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}

    	mergeBranches(outputSpaceID, headBranchID1, headBranchID2, null);
    	
    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    private void mergeNodeSpace(Node inputNode, 
    		NodeSpace outputSpace, HashMap<String, Set<Node>> inputToOutputNodes, 
    		HashMap<String, Node> inputToSurplusOutputNodes, Set<Node> visitedInputNodes) {
    	if (inputNode.hasEdges()) {
    		for (Edge inputEdge : inputNode.getEdges()) {
    			Node inputSuccessor = inputEdge.getHead();
    			Node outputSuccessor = null;
    			if (inputToSurplusOutputNodes.containsKey(inputSuccessor.getNodeID())) {
    				outputSuccessor = inputToSurplusOutputNodes.get(inputSuccessor);
    			} else if (!inputToOutputNodes.containsKey(inputSuccessor.getNodeID())) {
    				outputSuccessor = outputSpace.copyNode(inputSuccessor);
    				inputToSurplusOutputNodes.put(inputSuccessor.getNodeID(), outputSuccessor);
    			}
    			if (outputSuccessor != null) {
    				if (inputToOutputNodes.containsKey(inputNode.getNodeID())) {
    					for (Node outputNode : inputToOutputNodes.get(inputNode.getNodeID())) {
        					outputNode.addEdge(new Edge(outputNode, outputSuccessor, 
        							inputEdge.getComponentIDs(), inputEdge.getComponentRoles()));
        				}
    				}
    				if (inputToSurplusOutputNodes.containsKey(inputNode.getNodeID())) {
    					Node outputNode = inputToSurplusOutputNodes.get(inputNode.getNodeID());
    					outputNode.addEdge(new Edge(outputNode, outputSuccessor, 
    							inputEdge.getComponentIDs(), inputEdge.getComponentRoles()));
    				}
    			}
    			if (!visitedInputNodes.contains(inputSuccessor)) {
    				visitedInputNodes.add(inputSuccessor);
    				mergeNodeSpace(inputSuccessor, outputSpace, inputToOutputNodes, 
    						inputToSurplusOutputNodes, visitedInputNodes);
    			}
    		}
    	}
    }
    
    private Node mergeNodeSpaces(Node inputNode1, Node inputNode2, NodeSpace outputSpace, HashMap<String, Node> mergedInputsToOutputNodes, 
    		HashMap<String, Set<Node>> inputToOutputNodes1, HashMap<String, Set<Node>> inputToOutputNodes2, 
    		boolean intersectComponents) {
    	Node outputNode;
    	if (inputNode1.getNodeType() != null || inputNode2.getNodeType() == null) {
    		outputNode = outputSpace.copyNode(inputNode1);
    	} else {
    		outputNode = outputSpace.copyNode(inputNode2);
    	}
    	if (!inputToOutputNodes1.containsKey(inputNode1.getNodeID())) {
    		inputToOutputNodes1.put(inputNode1.getNodeID(), new HashSet<Node>());
    	}
    	if (!inputToOutputNodes2.containsKey(inputNode2.getNodeID())) {
    		inputToOutputNodes2.put(inputNode2.getNodeID(), new HashSet<Node>());
    	}
    	inputToOutputNodes1.get(inputNode1.getNodeID()).add(outputNode);
    	inputToOutputNodes2.get(inputNode2.getNodeID()).add(outputNode);
    	mergedInputsToOutputNodes.put(inputNode1.getNodeID() + inputNode2.getNodeID(), outputNode);
    	
    	if (inputNode1.hasEdges() && inputNode2.hasEdges()) {
    		for (Edge edge1 : inputNode1.getEdges()) {
    			for (Edge edge2 : inputNode2.getEdges()) {
    				if (edge1.hasSameComponentRoles(edge2)) {
    					Node inputSuccessor1 = edge1.getHead();
    					Node inputSuccessor2 = edge2.getHead();
    					Node outputSuccessor;
    					String mergedInputIDs = inputSuccessor1.getNodeID() + inputSuccessor2.getNodeID();
    					if (mergedInputsToOutputNodes.containsKey(mergedInputIDs)) {
    						outputSuccessor = mergedInputsToOutputNodes.get(mergedInputIDs);
    					} else {
    						outputSuccessor = mergeNodeSpaces(inputSuccessor1, inputSuccessor2, outputSpace, 
    								mergedInputsToOutputNodes, inputToOutputNodes1, inputToOutputNodes2, 
    								intersectComponents);
    					}
    					if (intersectComponents) {
    						outputNode.addEdge(intersectEdges(edge1, edge2, outputNode, outputSuccessor));
    					} else {
    						outputNode.addEdge(unionEdges(edge1, edge2, outputNode, outputSuccessor));
    					}
    				}
    			}
    		}
    	}
    	return outputNode;
    }
    
    public void minimizeDesignSpace(String targetSpaceID) 
    		throws DesignSpaceNotFoundException {
    	validateDesignSpaceOperator(targetSpaceID);
    	
    	Map<String, Set<Edge>> nodeToIncomingEdges = new HashMap<String, Set<Edge>>();

    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 2);
    	Stack<Node> nodeStack = new Stack<Node>();
    	for (Node node : targetSpace.getNodes()) {
    		nodeStack.push(node);
    		if (node.hasEdges()) {
    			for (Edge edge : node.getEdges()) {
    				if (!nodeToIncomingEdges.containsKey(edge.getHead().getNodeID())) {
    					nodeToIncomingEdges.put(edge.getHead().getNodeID(), new HashSet<Edge>());
    				}
    				nodeToIncomingEdges.get(edge.getHead().getNodeID()).add(edge);
    			}
    		}
    	}
    	
    	Set<String> deletedNodeIDs = new HashSet<String>();
    	while (nodeStack.size() > 0) {
    		Node node = nodeStack.pop();
    		if (!deletedNodeIDs.contains(node.getNodeID())) {
    			for (Edge blankEdge : node.minimizeEdges()) {
    				if (!blankEdge.isCyclicEdge()) {
    					Node successor = blankEdge.getHead();
    					if (successor.hasEdges()) {
    						for (Edge edge : successor.getEdges()) {
    							node.copyEdge(edge);
    							for (Edge incomingEdge : nodeToIncomingEdges.get(successor.getNodeID())) {
    								if (!blankEdge.equals(incomingEdge)) {
    									incomingEdge.setHead(node);
    								}
    							}
    						}
    					}
    					nodeToIncomingEdges.remove(successor.getNodeID());
    					deletedNodeIDs.add(successor.getNodeID());
    					if (successor.isAcceptNode()) {
    						node.setNodeType(Node.NodeType.ACCEPT.getValue());
    					}
    				}
    			}
    			if (node.hasMinimizableEdges()) {
    				nodeStack.push(node);
    			}
    		}
    	}
    	targetSpace.deleteNodesByID(deletedNodeIDs);
    	
    	designSpaceRepository.save(targetSpace);
    }
    
    public void orDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) 
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	if (!outputSpaceID.equals(inputSpaceID1)) {
    		unionDesignSpace(inputSpaceID1, outputSpaceID);
    	}
    	if (!outputSpaceID.equals(inputSpaceID2)) {
    		unionDesignSpace(inputSpaceID2, outputSpaceID);
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);

    	Set<Node> startNodes = getStartNodes(outputSpaceID);
    
    	Node startNodePrime = createTypedNode(outputSpaceID, Node.NodeType.START.getValue());
    	
    	for (Node startNode : startNodes) {
    		deleteNodeType(outputSpaceID, startNode.getNodeID());
    		createEdge(outputSpaceID, startNodePrime.getNodeID(), startNode.getNodeID());
    	}
    	
    	Set<Node> acceptNodes = getAcceptNodes(outputSpaceID);
        
    	Node acceptNodePrime = createTypedNode(outputSpaceID, Node.NodeType.ACCEPT.getValue());
    	
    	for (Node acceptNode : acceptNodes) {
    		deleteNodeType(outputSpaceID, acceptNode.getNodeID());
    		createEdge(outputSpaceID, acceptNode.getNodeID(), acceptNodePrime.getNodeID());
    	}
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}
    	
    	orBranches(outputSpaceID, headBranchID1, headBranchID2, null);

    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    private void copyDesignSpaceToSnapshot(String inputSpaceID, String outputBranchID) {
		designSpaceRepository.copyDesignSpaceToSnapshot(inputSpaceID, outputBranchID);
	}

	private void copySnapshots(String inputSpaceID, String inputBranchID, String outputSpaceID, String outputBranchID) {
	    designSpaceRepository.copySnapshots(inputSpaceID, inputBranchID, outputSpaceID, outputBranchID);
	}

	public void createEdge(String targetSpaceID, String targetTailID, String targetHeadID) {
    	designSpaceRepository.createEdge(targetSpaceID, targetTailID, targetHeadID);
    }
	
	public void createNode(String targetSpaceID) {
    	designSpaceRepository.createNode(targetSpaceID);
	}
    
	private void createCommit(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.createCommit(targetSpaceID, targetBranchID);
	}

	private void createComponentEdge(String targetSpaceID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		designSpaceRepository.createComponentEdge(targetSpaceID, targetTailID, targetHeadID, componentIDs, componentRoles);
	}

	private void createComponentEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		designSpaceRepository.createComponentEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID, componentIDs, componentRoles);
	}

	private void createEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID) {
		designSpaceRepository.createEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID);
	}

	private Node createTypedNode(String targetSpaceID, String nodeType) {
		Set<Node> typedNodes = designSpaceRepository.createTypedNode(targetSpaceID, nodeType);
		if (typedNodes.size() > 0) {
			return typedNodes.iterator().next();
		} else {
			return null;
		}
	}

	private Node createTypedNode(String targetSpaceID, String targetBranchID, String nodeType) {
		Set<Node> typedNodes = designSpaceRepository.createTypedNode(targetSpaceID, targetBranchID, nodeType);
		if (typedNodes.size() > 0) {
			return typedNodes.iterator().next();
		} else {
			return null;
		}
	}

	private void deleteCommitCopyIndices(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteCommitCopyIndices(targetSpaceID, targetBranchID);
	}

	private void deleteNodeCopyIndices(String targetSpaceID) {
    	designSpaceRepository.deleteNodeCopyIndices(targetSpaceID);
    }
    
    private void deleteNodeCopyIndices(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteNodeCopyIndices(targetSpaceID, targetBranchID);
	}

	private void deleteNodes(String targetSpaceID) {
		designSpaceRepository.deleteNodes(targetSpaceID);
	}

	private void deleteNodeType(String targetSpaceID, String targetNodeID) {
		designSpaceRepository.deleteNodeType(targetSpaceID, targetNodeID);
	}

	private void deleteNodeType(String targetSpaceID, String targetBranchID, String targetNodeID) {
		designSpaceRepository.deleteNodeType(targetSpaceID, targetBranchID, targetNodeID);
	}

	private void fastForwardBranch(String targetSpaceID, String targetBranchID1, String targetBranchID2) {
		designSpaceRepository.fastForwardBranch(targetSpaceID, targetBranchID1, targetBranchID2);
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
	
	private DesignSpace loadDesignSpace(String targetSpaceID, int depth) {
		return designSpaceRepository.findOne(getGraphID(targetSpaceID), depth);
	}
	
	private Node findNode(String targetSpaceID, String targetNodeID) {
		Set<Node> targetNodes = designSpaceRepository.findNode(targetSpaceID, targetNodeID);
		if (targetNodes.size() > 0) {
			return targetNodes.iterator().next();
		} else {
			return null;
		}
	}
    
    private Node findNodeCopy(String targetSpaceID1, String targetNodeID, String targetSpaceID2) {
		Set<Node> nodeCopies = designSpaceRepository.findNodeCopy(targetSpaceID1, targetNodeID, targetSpaceID2);
		if (nodeCopies.size() > 0) {
			return nodeCopies.iterator().next();
		} else {
			return null;
		}
	}

	private Node findNodeCopy(String targetSpaceID, String targetBranchID1, String targetNodeID, String targetBranchID2) {
		Set<Node> nodeCopies = designSpaceRepository.findNodeCopy(targetSpaceID, targetBranchID1, targetNodeID, targetBranchID2);
		if (nodeCopies.size() > 0) {
			return nodeCopies.iterator().next();
		} else {
			return null;
		}
	}

	private Set<String> getBranchIDs(String targetSpaceID) {
		return designSpaceRepository.getBranchIDs(targetSpaceID);
	}
	
	private Set<String> getCommonBranchIDs(String targetSpaceID1, String targetSpaceID2) {
		return designSpaceRepository.getCommonBranchIDs(targetSpaceID1, targetSpaceID2);
	}

	private String getHeadBranchID(String targetSpaceID) {
		Set<String> headBranchIDs = designSpaceRepository.getHeadBranchID(targetSpaceID);
		if (headBranchIDs.size() > 0) {
			return headBranchIDs.iterator().next();
		} else {
			return null;
		}
	}
	
	private Long getGraphID(String targetSpaceID) {
		Set<Integer> graphIDs = designSpaceRepository.getGraphID(targetSpaceID);
		if (graphIDs.size() > 0) {
			return new Long(graphIDs.iterator().next());
		} else {
			return null;
		}
	}
	
	private Set<Node> getAcceptNodes(String targetSpaceID) {
		return getNodesByType(targetSpaceID, Node.NodeType.ACCEPT.getValue());
	}
	
	private Set<Node> getAcceptNodes(String targetSpaceID, String targetBranchID) {
		return getNodesByType(targetSpaceID, targetBranchID, Node.NodeType.ACCEPT.getValue());
	}

	private Set<Node> getNodesByType(String targetSpaceID, String nodeType) {
		return designSpaceRepository.getNodesByType(targetSpaceID, nodeType);
	}

	private Set<Node> getNodesByType(String targetSpaceID, String targetBranchID, String nodeType) {
		return designSpaceRepository.getNodesByType(targetSpaceID, targetBranchID, nodeType);
	}
	
	private Set<Edge> getOutgoingEdges(String targetSpaceID, String targetNodeID) {
		return designSpaceRepository.getOutgoingEdges(targetSpaceID, targetNodeID);
	}

	private Set<Edge> getOutgoingEdges(String targetSpaceID, String targetBranchID, String targetNodeID) {
		return designSpaceRepository.getOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
	}
	
	private Node getStartNode(String targetSpaceID) {
		Set<Node> startNodes = getNodesByType(targetSpaceID, Node.NodeType.START.getValue());
		if (startNodes.size() > 0) {
			return startNodes.iterator().next();
		} else {
			return null;
		}
	}
	
	private Node getStartNode(String targetSpaceID, String targetBranchID) {
		Set<Node> startNodes = getNodesByType(targetSpaceID, targetBranchID, Node.NodeType.START.getValue());
		if (startNodes.size() > 0) {
			return startNodes.iterator().next();
		} else {
			return null;
		}
	}
	
	private Set<Node> getStartNodes(String targetSpaceID) {
		return getNodesByType(targetSpaceID, Node.NodeType.START.getValue());
	}
	
	private Set<Node> getStartNodes(String targetSpaceID, String targetBranchID) {
		return getNodesByType(targetSpaceID, targetBranchID, Node.NodeType.START.getValue());
	}
	
	public boolean hasBranch(String targetSpaceID, String targetBranchID) {
		return findBranch(targetSpaceID, targetBranchID) != null;
	}
	
	public boolean hasCommonBranches(String targetSpaceID1, String targetSpaceID2) {
		return getCommonBranchIDs(targetSpaceID1, targetSpaceID2).size() > 0;
	}
	
	public boolean hasDesignSpace(String targetSpaceID) {
		return findDesignSpace(targetSpaceID) != null;
	}
	
	public boolean hasNode(String targetSpaceID, String targetNodeID) {
		return findNode(targetSpaceID, targetNodeID) != null;
	}
	
//	private void indexVersionMerger(String targetSpaceID) {
//		designSpaceRepository.indexVersionMerger(targetSpaceID);
//	}
	
	private void indexVersionMerger(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.indexVersionMerger(targetSpaceID, targetBranchID);
	}
	
	private Edge intersectEdges(Edge edge1, Edge edge2, Node tail, Node head) {
    	Set<String> intersectedCompIDs = new HashSet<String>();
    	Set<String> intersectedCompRoles = new HashSet<String>();
    	if (edge1.hasComponentIDs()) {
    		intersectedCompIDs.addAll(edge1.getComponentIDs());
    	}
    	if (edge1.hasComponentRoles()) {
    		intersectedCompRoles.addAll(edge1.getComponentRoles());
    	}
    	for (String compID : intersectedCompIDs) {
    		if (!edge2.hasComponentID(compID)) {
    			intersectedCompIDs.remove(compID);
    		}
    	}
    	for (String compRole : intersectedCompRoles) {
    		if (!edge2.hasComponentRole(compRole)) {
    			intersectedCompRoles.remove(compRole);
    		}
    	}
    	return new Edge(tail, head, new ArrayList<String>(intersectedCompIDs), 
    			new ArrayList<String>(intersectedCompRoles));
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

	private Map<String, Object> mapDesignSpaceToD3Format(List<Map<String, Object>> spaceMap) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
	    List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	    List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	    int i = 0;
	    for (Map<String, Object> row : spaceMap) {
	        if (d3Graph.isEmpty()) {
	        	d3Graph.put("spaceID", row.get("spaceID"));
	        }
	        Map<String, Object> tail = makeD3("nodeID", row.get("tailID"), "nodeType", row.get("tailType"));
	        int source = nodes.indexOf(tail);
	        if (source == -1) {
	        	nodes.add(tail);
	        	source = i++;
	        }
	        Map<String, Object> head = makeD3("nodeID", row.get("headID"), "nodeType", row.get("headType"));
	        int target = nodes.indexOf(head);
	        if (target == -1) {
	        	nodes.add(head);
	        	target = i++;
	        }
	        Map<String, Object> link = makeD3("source", source, "target", target);
	        if (row.containsKey("componentRoles") && row.get("componentRoles") != null) {
	        	link.put("componentRoles", row.get("componentRoles"));
	        }
	        links.add(link);
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

	private void mergeBranch(String targetSpaceID, String inputBranchID, String outputBranchID) {
	    designSpaceRepository.mergeBranch(targetSpaceID, inputBranchID, outputBranchID);
	}

	private void mergeBranch(String inputSpaceID, String inputBranchID, String outputSpaceID, String outputBranchID) {
	    designSpaceRepository.mergeBranch(inputSpaceID, inputBranchID, outputSpaceID, outputBranchID);
	}
	
	private void mergeVersionHistory(String inputSpaceID, String outputSpaceID) {
    	Set<String> inputBranchIDs = getBranchIDs(inputSpaceID);
    	for (String inputBranchID : inputBranchIDs) {
    		mergeBranch(inputSpaceID, inputBranchID, outputSpaceID, inputBranchID);
    		copySnapshots(inputSpaceID, inputBranchID, outputSpaceID, inputBranchID);
    	}
    	for (String inputBranchID : inputBranchIDs) {
    		deleteCommitCopyIndices(outputSpaceID, inputBranchID);
    	}
    }
	
	private Set<Edge> removeOutgoingEdges(String targetSpaceID, String targetNodeID) {
		Set<Edge> removedEdges = getOutgoingEdges(targetSpaceID, targetNodeID);
		if (removedEdges.size() > 0) {
			designSpaceRepository.deleteOutgoingEdges(targetSpaceID, targetNodeID);
		}
		return removedEdges;
	}

	private Set<Edge> removeOutgoingEdges(String targetSpaceID, String targetBranchID, String targetNodeID) {
		Set<Edge> removedEdges = getOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
		if (removedEdges.size() > 0) {
			designSpaceRepository.deleteOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
		}
		return removedEdges;
	}

	private void selectHeadBranch(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.selectHeadBranch(targetSpaceID, targetBranchID);
	}

	private void unionDesignSpace(String inputSpaceID, String outputSpaceID) {
        designSpaceRepository.unionDesignSpace(inputSpaceID, outputSpaceID);
    }
	
	private Edge unionEdges(Edge edge1, Edge edge2, Node tail, Node head) {
    	Set<String> mergedCompIDs = new HashSet<String>();
    	Set<String> mergedCompRoles = new HashSet<String>();
    	if (edge1.hasComponentIDs()) {
    		mergedCompIDs.addAll(edge1.getComponentIDs());
    	}
    	if (edge2.hasComponentIDs()) {
    		mergedCompIDs.addAll(edge2.getComponentIDs());
    	}
    	if (edge1.hasComponentRoles()) {
    		mergedCompRoles.addAll(edge1.getComponentRoles());
    	}
    	if (edge2.hasComponentRoles()) {
    		mergedCompRoles.addAll(edge2.getComponentRoles());
    	}
    	return new Edge(tail, head, new ArrayList<String>(mergedCompIDs), 
    			new ArrayList<String>(mergedCompRoles));
    }
    
    private void unionSnapshot(String targetSpaceID, String inputBranchID, String outputBranchID) {
        designSpaceRepository.unionSnapshot(targetSpaceID, inputBranchID, outputBranchID);
    }
    
    private void validateNodeOperator(String targetSpaceID, String targetNodeID) 
    		throws NodeNotFoundException {
    	if (!hasNode(targetSpaceID, targetNodeID)) {
    		throw new NodeNotFoundException(targetSpaceID, targetNodeID);
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
    
    private void validateCombinationalDesignSpaceOperator(String inputSpaceID1, String inputSpaceID2, String outputSpaceID)
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	if (!hasDesignSpace(inputSpaceID1)) {
    		throw new DesignSpaceNotFoundException(inputSpaceID1);
    	} else if (!hasDesignSpace(inputSpaceID2)){
    		throw new DesignSpaceNotFoundException(inputSpaceID2);
    	} else if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2) && hasDesignSpace(outputSpaceID)) {
    		throw new DesignSpaceConflictException(outputSpaceID);
    	} else if (hasCommonBranches(inputSpaceID1, inputSpaceID2)) {
    		throw new DesignSpaceBranchesConflictException(inputSpaceID1, inputSpaceID2);
    	}
    }
    
    
    
}
