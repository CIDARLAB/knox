package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
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
    	joinBranches(targetSpaceID, inputBranchID1, inputBranchID2, "knox1");
    	joinBranches(targetSpaceID, inputBranchID1, inputBranchID2, "knox2");
    	
    	orBranches(targetSpaceID, "knox1", "knox2", outputBranchID);
    	
    	deleteBranch(targetSpaceID, "knox1");
    	deleteBranch(targetSpaceID, "knox2");
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
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);

    	Set<Node> startNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.ACCEPT.value);
    	
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
    			Set<Node> acceptNodes = getNodesByType(targetSpaceID, outputBranchID, NodeType.ACCEPT.value);
    			for (Node acceptNode : acceptNodes) {
    				if (nodeCopy.getNodeID().equals(acceptNode.getNodeID())) {
    					deleteNodeType(targetSpaceID, outputBranchID, nodeCopy.getNodeID());
    				}
    			}
    		}
    		if (startNodes1.size() > 0) {
    			Node startNode1 = startNodes1.iterator().next();
    			deleteNodeType(targetSpaceID, outputBranchID, startNode1.getNodeID());
    			createEdge(targetSpaceID, outputBranchID, nodeCopy.getNodeID(), startNode1.getNodeID());
    		}
    		for (Node acceptNode1 : acceptNodes1) {
    			for (Edge removedEdge : removedEdges) {
    				if (removedEdge.hasRoles()) {
    					createComponentEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    				} else {
    					createEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID());
    				}
    			}
    		}
    	}
    }
    
    public void joinBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);
    	
    	Set<Node> startNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.ACCEPT.value);
    	
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	Set<Node> startNodes = getNodesByType(targetSpaceID, outputBranchID, NodeType.START.value);
    	
    	if (startNodes1.size() > 0) {
    		Node startNode1 = startNodes1.iterator().next();
    		for (Node startNode : startNodes) {
    			if (!startNode.getNodeID().equals(startNode1.getNodeID())) {
    				deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
    				for (Node acceptNode1 : acceptNodes1) {
    					deleteNodeType(targetSpaceID, outputBranchID, acceptNode1.getNodeID());
    					createEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), startNode.getNodeID());
    				}
    			}
    		}
    	}
    }
    
    public void orBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);

    	Set<Node> startNodes = getNodesByType(targetSpaceID, outputBranchID, NodeType.START.value);
    
    	Node startNodePrime = createTypedNode(outputBranchID, outputBranchID, NodeType.START.value);
    	
    	for (Node startNode : startNodes) {
    		deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
    		createEdge(targetSpaceID, outputBranchID, startNodePrime.getNodeID(), startNode.getNodeID());
    	}
    	
    	Set<Node> acceptNodes = getNodesByType(targetSpaceID, outputBranchID, NodeType.ACCEPT.value);
        
    	Node acceptNodePrime = createTypedNode(outputBranchID, outputBranchID, NodeType.ACCEPT.value);
    	
    	for (Node acceptNode : acceptNodes) {
    		deleteNodeType(targetSpaceID, outputBranchID, acceptNode.getNodeID());
    		createEdge(targetSpaceID, outputBranchID, acceptNode.getNodeID(), acceptNodePrime.getNodeID());
    	}
    }
   
    public void deleteDesignSpace(String targetSpaceID) {
    	designSpaceRepository.deleteDesignSpace(targetSpaceID);
    }
    
    public void createDesignSpace(String outputSpaceID) {
    	designSpaceRepository.createDesignSpace(outputSpaceID);
    }
    
    public void andDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	joinDesignSpaces(inputSpaceID1, inputSpaceID2, "knox1");
    	joinDesignSpaces(inputSpaceID2, inputSpaceID1, "knox2");
    	
    	orDesignSpaces("knox1", "knox2", outputSpaceID);
    	
    	deleteDesignSpace("knox1");
    	deleteDesignSpace("knox2");
    }
    
    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return mapDesignSpaceToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public void insertDesignSpace(String inputSpaceID1, String inputSpaceID2, String targetNodeID, String outputSpaceID) {
    	unionDesignSpace(inputSpaceID2, outputSpaceID);

    	Set<Node> startNodes2 = getNodesByType(outputSpaceID, NodeType.START.value);
    	Set<Node> acceptNodes2 = getNodesByType(outputSpaceID, NodeType.ACCEPT.value);
    	
    	unionDesignSpace(inputSpaceID1, outputSpaceID);
    	
    	Node targetNode = findNodeCopy(inputSpaceID1, targetNodeID, outputSpaceID);
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	Set<Edge> removedEdges = removeOutgoingEdges(outputSpaceID, targetNode.getNodeID());

    	if (removedEdges.size() > 0) {
    		for (Node acceptNode2 : acceptNodes2) {
    			deleteNodeType(outputSpaceID, acceptNode2.getNodeID());
    		}
    	} 
    	
    	if (startNodes2.size() > 0) {
    		Node startNode2 = startNodes2.iterator().next();
    		deleteNodeType(outputSpaceID, startNode2.getNodeID());
    		createEdge(outputSpaceID, targetNode.getNodeID(), startNode2.getNodeID());
    	}
    	for (Node acceptNode2 : acceptNodes2) {
    		for (Edge removedEdge : removedEdges) {
    			if (removedEdge.hasRoles()) {
    				createComponentEdge(outputSpaceID, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    			} else {
    				createEdge(outputSpaceID, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID());
    			}
    		}
    	}
    		
    	unionVersionHistories(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);

    	insertBranch(outputSpaceID, headBranchID1, headBranchID2, targetNodeID, RESERVED_ID);
    	fastForwardBranch(outputSpaceID, headBranchID1, RESERVED_ID);
    	fastForwardBranch(outputSpaceID, headBranchID2, RESERVED_ID);
    	deleteBranch(outputSpaceID, RESERVED_ID);
    	
    	selectHeadBranch(outputSpaceID, headBranchID1);
    }
    
    public void insertDesignSpace(String inputSpaceID1, String inputSpaceID2, String targetNodeID) {
    	Set<Node> startNodes1 = getNodesByType(inputSpaceID1, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(inputSpaceID1, NodeType.ACCEPT.value);
    	
    	unionDesignSpace(inputSpaceID2, inputSpaceID1);
    	
    	deleteNodeCopyIndices(inputSpaceID1);
    	
    	Set<Node> startNodes2 = new HashSet<Node>();
    	Set<Node> acceptNodes2 = new HashSet<Node>();
    	
    	for (Node startNode : getNodesByType(inputSpaceID1, NodeType.START.value)) {
    		if (!startNodes1.contains(startNode)) {
    			startNodes2.add(startNode);
    		}
    	}
    	for (Node acceptNode : getNodesByType(inputSpaceID1, NodeType.ACCEPT.value)) {
    		if (!acceptNodes1.contains(acceptNode)) {
    			acceptNodes2.add(acceptNode);
    		}
    	}

    	Node targetNode = findNode(inputSpaceID1, targetNodeID);
    	
    	Set<Edge> removedEdges = removeOutgoingEdges(inputSpaceID1, targetNode.getNodeID());

    	if (removedEdges.size() > 0) {
    		for (Node acceptNode2 : acceptNodes2) {
    			deleteNodeType(inputSpaceID1, acceptNode2.getNodeID());
    		}
    	} 
    	
    	if (startNodes2.size() > 0) {
    		Node startNode2 = startNodes2.iterator().next();
    		deleteNodeType(inputSpaceID1, startNode2.getNodeID());
    		createEdge(inputSpaceID1, targetNode.getNodeID(), startNode2.getNodeID());
    	}
    	for (Node acceptNode2 : acceptNodes2) {
    		for (Edge removedEdge : removedEdges) {
    			if (removedEdge.hasRoles()) {
    				createComponentEdge(inputSpaceID1, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    			} else {
    				createEdge(inputSpaceID1, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID());
    			}
    		}
    	}
    		
    	unionVersionHistory(inputSpaceID2, inputSpaceID1);
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);

    	insertBranch(inputSpaceID1, headBranchID1, headBranchID2, targetNodeID, RESERVED_ID);
    	fastForwardBranch(inputSpaceID1, headBranchID1, RESERVED_ID);
    	fastForwardBranch(inputSpaceID1, headBranchID2, RESERVED_ID);
    	deleteBranch(inputSpaceID1, RESERVED_ID);
    }
    
    public void joinDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	unionDesignSpace(inputSpaceID1, outputSpaceID);
    	
    	Set<Node> startNodes1 = getNodesByType(outputSpaceID, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(outputSpaceID, NodeType.ACCEPT.value);
    	
    	unionDesignSpace(inputSpaceID2, outputSpaceID);
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	Set<Node> startNodes = getNodesByType(outputSpaceID, NodeType.START.value);
    	
    	if (startNodes1.size() > 0) {
    		Node startNode1 = startNodes1.iterator().next();
    		for (Node startNode : startNodes) {
    			if (!startNode.getNodeID().equals(startNode1.getNodeID())) {
    				deleteNodeType(outputSpaceID, startNode.getNodeID());
    				for (Node acceptNode1 : acceptNodes1) {
    					deleteNodeType(outputSpaceID, acceptNode1.getNodeID());
    					createEdge(outputSpaceID, acceptNode1.getNodeID(), startNode.getNodeID());
    				}
    			}
    		}
    	}
    	
    	unionVersionHistories(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	Set<String> inputBranchIDs1 = getBranchIDs(inputSpaceID1);
    	Set<String> inputBranchIDs2 = getBranchIDs(inputSpaceID2);
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);

    	if (!inputBranchIDs1.contains(headBranchID2) && !inputBranchIDs1.contains(outputSpaceID) 
    			&& !inputBranchIDs2.contains(outputSpaceID)) {
    		joinBranches(outputSpaceID, headBranchID1, headBranchID2, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID1, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID2, outputSpaceID);
    		deleteBranch(outputSpaceID, outputSpaceID);
    	}
    	
    	selectHeadBranch(outputSpaceID, headBranchID1);
    }
    
    public void orDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	unionDesignSpace(inputSpaceID1, outputSpaceID);
    	unionDesignSpace(inputSpaceID2, outputSpaceID);
    	
    	deleteNodeCopyIndices(outputSpaceID);

    	Set<Node> startNodes = getNodesByType(outputSpaceID, NodeType.START.value);
    
    	Node startNodePrime = createTypedNode(outputSpaceID, NodeType.START.value);
    	
    	for (Node startNode : startNodes) {
    		deleteNodeType(outputSpaceID, startNode.getNodeID());
    		createEdge(outputSpaceID, startNodePrime.getNodeID(), startNode.getNodeID());
    	}
    	
    	Set<Node> acceptNodes = getNodesByType(outputSpaceID, NodeType.ACCEPT.value);
        
    	Node acceptNodePrime = createTypedNode(outputSpaceID, NodeType.ACCEPT.value);
    	
    	for (Node acceptNode : acceptNodes) {
    		deleteNodeType(outputSpaceID, acceptNode.getNodeID());
    		createEdge(outputSpaceID, acceptNode.getNodeID(), acceptNodePrime.getNodeID());
    	}
    	
    	unionVersionHistories(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	Set<String> inputBranchIDs1 = getBranchIDs(inputSpaceID1);
    	Set<String> inputBranchIDs2 = getBranchIDs(inputSpaceID2);
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);

    	if (!inputBranchIDs1.contains(headBranchID2) && !inputBranchIDs1.contains(outputSpaceID) 
    			&& !inputBranchIDs2.contains(outputSpaceID)) {
    		orBranches(outputSpaceID, headBranchID1, headBranchID2, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID1, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID2, outputSpaceID);
    		deleteBranch(outputSpaceID, outputSpaceID);
    	}
    	
    	selectHeadBranch(outputSpaceID, headBranchID1);
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
		Set<Node> typedNode = designSpaceRepository.createTypedNode(targetSpaceID, nodeType);
		if (typedNode.size() > 0) {
			return typedNode.iterator().next();
		} else {
			return null;
		}
	}

	private Node createTypedNode(String targetSpaceID, String targetBranchID, String nodeType) {
		Set<Node> typedNode = designSpaceRepository.createTypedNode(targetSpaceID, targetBranchID, nodeType);
		if (typedNode.size() > 0) {
			return typedNode.iterator().next();
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
		Set<Branch> targetBranch = designSpaceRepository.findBranch(targetSpaceID, targetBranchID);
		if (targetBranch.size() > 0) {
			return targetBranch.iterator().next();
		} else {
			return null;
		}
	}

	private DesignSpace findDesignSpace(String targetSpaceID) {
    	return designSpaceRepository.findBySpaceID(targetSpaceID);
    }
	
	private Node findNode(String targetSpaceID, String targetNodeID) {
		Set<Node> targetNode = designSpaceRepository.findNode(targetSpaceID, targetNodeID);
		if (targetNode.size() > 0) {
			return targetNode.iterator().next();
		} else {
			return null;
		}
	}
    
    private Node findNodeCopy(String targetSpaceID1, String targetNodeID, String targetSpaceID2) {
		Set<Node> nodeCopy = designSpaceRepository.findNodeCopy(targetSpaceID1, targetNodeID, targetSpaceID2);
		if (nodeCopy.size() > 0) {
			return nodeCopy.iterator().next();
		} else {
			return null;
		}
	}

	private Node findNodeCopy(String targetSpaceID, String targetBranchID1, String targetNodeID, String targetBranchID2) {
		Set<Node> nodeCopy = designSpaceRepository.findNodeCopy(targetSpaceID, targetBranchID1, targetNodeID, targetBranchID2);
		if (nodeCopy.size() > 0) {
			return nodeCopy.iterator().next();
		} else {
			return null;
		}
	}

	private Set<String> getBranchIDs(String targetSpaceID) {
		Set<String> branchIDs = new HashSet<String>();
		List<Map<String, Object>> rows = designSpaceRepository.getBranchIDs(targetSpaceID);
		for (Map<String, Object> row : rows) {
			branchIDs.add((String) row.get("branchID"));
		}
		return branchIDs;
	}
	
	private Set<String> getCommonBranchIDs(String targetSpaceID1, String targetSpaceID2) {
		return designSpaceRepository.getCommonBranchIDs(targetSpaceID1, targetSpaceID2);
	}

	private String getHeadBranchID(String targetSpaceID) {
		List<Map<String, Object>> rows = designSpaceRepository.getHeadBranchID(targetSpaceID);
		if (rows.size() == 1) {
			return (String) rows.get(0).get("headBranchID");
		} else {
			return null;
		}
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
	    		if (!branchIDs.contains(branchID)) {
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
    
    private void unionSnapshot(String targetSpaceID, String inputBranchID, String outputBranchID) {
        designSpaceRepository.unionSnapshot(targetSpaceID, inputBranchID, outputBranchID);
    }
    
    private void unionVersionHistory(String inputSpaceID, String outputSpaceID) {
    	Set<String> inputBranchIDs = getBranchIDs(inputSpaceID);
    	for (String inputBranchID : inputBranchIDs) {
    		mergeBranch(inputSpaceID, inputBranchID, outputSpaceID, inputBranchID);
    		copySnapshots(inputSpaceID, inputBranchID, outputSpaceID, inputBranchID);
    	}
    	for (String inputBranchID : inputBranchIDs) {
    		deleteCommitCopyIndices(outputSpaceID, inputBranchID);
    	}
    }
    
    private void unionVersionHistories(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	Set<String> inputBranchIDs1 = getBranchIDs(inputSpaceID1);
    	Set<String> inputBranchIDs2 = getBranchIDs(inputSpaceID2);
    	for (String inputBranchID1 : inputBranchIDs1) {
    		mergeBranch(inputSpaceID1, inputBranchID1, outputSpaceID, inputBranchID1);
    		copySnapshots(inputSpaceID1, inputBranchID1, outputSpaceID, inputBranchID1);
    	}
    	for (String inputBranchID1 : inputBranchIDs1) {
    		deleteCommitCopyIndices(outputSpaceID, inputBranchID1);
    	}
    	for (String inputBranchID2 : inputBranchIDs2) {
    		mergeBranch(inputSpaceID2, inputBranchID2, outputSpaceID, inputBranchID2);
    		copySnapshots(inputSpaceID2, inputBranchID2, outputSpaceID, inputBranchID2);
    	}
    	for (String inputBranchID2 : inputBranchIDs2) {
    		deleteCommitCopyIndices(outputSpaceID, inputBranchID2);
    	}
    }
    
    private enum NodeType {
    	START ("start"),
    	ACCEPT ("accept");
    	
    	private final String value;
    	
    	NodeType(String value) {
    		this.value = value;
    	}
    }
    
}
