package knox.spring.data.neo4j.services;

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
    
    public void createBranch(String targetSpaceID, String outputBranchID) {
    	designSpaceRepository.createBranch(targetSpaceID, outputBranchID);
    }
    
    public void createDesignSpace(String outputSpaceID) {
    	designSpaceRepository.createDesignSpace(outputSpaceID);
    }
    
    public void checkoutBranch(String targetSpaceID, String targetBranchID) {
    	deleteNodes(targetSpaceID);
    	designSpaceRepository.checkoutBranch(targetSpaceID, targetBranchID);
    	deleteNodeCopyIndices(targetSpaceID);
    }
    
    public void commitToBranch(String targetSpaceID) {
    	designSpaceRepository.commitToBranch(targetSpaceID);
    }

    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return spaceMapToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public Map<String, Object> d3GraphBranches(String targetSpaceID) {
    	return branchMapToD3Format(designSpaceRepository.mapBranches(targetSpaceID));
    }
    
    public void joinBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	copyBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	
    	Set<Node> startNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.ACCEPT.value);
    	
    	copyBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	
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
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	deleteCommitCopyIndices(targetSpaceID, outputBranchID);
    }
    
    public void orBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	copyBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	copyBranch(targetSpaceID, inputBranchID2, outputBranchID);

    	Set<Node> startNodes = getNodesByType(targetSpaceID, outputBranchID, NodeType.START.value);
    
    	createTypedNode(outputBranchID, outputBranchID, "n00", NodeType.START.value);
    	
    	for (Node startNode : startNodes) {
    		deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
    		createEdge(targetSpaceID, outputBranchID, "n00", startNode.getNodeID());
    	}
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	deleteCommitCopyIndices(targetSpaceID, outputBranchID);
    }
    
    public void andBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	joinBranches(targetSpaceID, inputBranchID1, inputBranchID2, "knox1");
    	joinBranches(targetSpaceID, inputBranchID1, inputBranchID2, "knox2");
    	
    	orBranches(targetSpaceID, "knox1", "knox2", outputBranchID);
    	
    	deleteBranch(targetSpaceID, "knox1");
    	deleteBranch(targetSpaceID, "knox2");
    }
    
    public void insertBranch(String targetSpaceID, String inputBranchID1, String inputBranchID2, String targetNodeID, String outputBranchID) {
    	copyBranch(targetSpaceID, inputBranchID1, outputBranchID);

    	Set<Node> startNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(targetSpaceID, outputBranchID, NodeType.ACCEPT.value);
    	
    	copyBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	Node nodeCopy = findNodeCopy(targetSpaceID, inputBranchID2, targetNodeID, outputBranchID);
    	
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
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	deleteCommitCopyIndices(targetSpaceID, outputBranchID);
    }
    
    public void joinDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	copyDesignSpace(inputSpaceID1, outputSpaceID);
    	
    	Set<Node> startNodes1 = getNodesByType(outputSpaceID, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(outputSpaceID, NodeType.ACCEPT.value);
    	
    	copyDesignSpace(inputSpaceID2, outputSpaceID);
    	
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
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	Set<String> inputBranchIDs1 = getBranchIDs(inputSpaceID1);
    	Set<String> inputBranchIDs2 = getBranchIDs(inputSpaceID2);
    	
    	aggregateBranches(inputSpaceID1, inputSpaceID2, inputBranchIDs1, inputBranchIDs2, outputSpaceID);
    	
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
    	copyDesignSpace(inputSpaceID1, outputSpaceID);
    	copyDesignSpace(inputSpaceID2, outputSpaceID);

    	Set<Node> startNodes = getNodesByType(outputSpaceID, NodeType.START.value);
    
    	createTypedNode(outputSpaceID, "n00", NodeType.START.value);
    	
    	for (Node startNode : startNodes) {
    		deleteNodeType(outputSpaceID, startNode.getNodeID());
    		createEdge(outputSpaceID, "n00", startNode.getNodeID());
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	Set<String> inputBranchIDs1 = getBranchIDs(inputSpaceID1);
    	Set<String> inputBranchIDs2 = getBranchIDs(inputSpaceID2);
    	
    	aggregateBranches(inputSpaceID1, inputSpaceID2, inputBranchIDs1, inputBranchIDs2, outputSpaceID);
    	
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
    
    public void andDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	joinDesignSpaces(inputSpaceID1, inputSpaceID2, "knox1");
    	joinDesignSpaces(inputSpaceID2, inputSpaceID1, "knox2");
    	
    	orDesignSpaces("knox1", "knox2", outputSpaceID);
    	
    	deleteDesignSpace("knox1");
    	deleteDesignSpace("knox2");
    	
    	Set<String> inputBranchIDs1 = getBranchIDs(inputSpaceID1);
    	Set<String> inputBranchIDs2 = getBranchIDs(inputSpaceID2);
    	
    	aggregateBranches(inputSpaceID1, inputSpaceID2, inputBranchIDs1, inputBranchIDs2, outputSpaceID);
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);

    	if (!inputBranchIDs1.contains(headBranchID2) && !inputBranchIDs1.contains(outputSpaceID) 
    			&& !inputBranchIDs2.contains(outputSpaceID)) {
    		andBranches(outputSpaceID, headBranchID1, headBranchID2, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID1, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID2, outputSpaceID);
    		deleteBranch(outputSpaceID, outputSpaceID);
    	}
    	
    	selectHeadBranch(outputSpaceID, headBranchID1);
    }
    
	public void insertDesignSpace(String inputSpaceID1, String inputSpaceID2, String targetNodeID, String outputSpaceID) {
    	copyDesignSpace(inputSpaceID1, outputSpaceID);

    	Set<Node> startNodes1 = getNodesByType(outputSpaceID, NodeType.START.value);
    	Set<Node> acceptNodes1 = getNodesByType(outputSpaceID, NodeType.ACCEPT.value);
    	
    	copyDesignSpace(inputSpaceID2, outputSpaceID);
    	
    	Node nodeCopy = findNodeCopy(inputSpaceID2, targetNodeID, outputSpaceID);
    	
    	if (nodeCopy != null) {
    		Set<Edge> removedEdges = removeOutgoingEdges(outputSpaceID, nodeCopy.getNodeID());

    		if (removedEdges.size() > 0) {
    			for (Node acceptNode1 : acceptNodes1) {
    				deleteNodeType(outputSpaceID, acceptNode1.getNodeID());
    			}
    		} else {
    			Set<Node> acceptNodes = getNodesByType(outputSpaceID, NodeType.ACCEPT.value);
    			for (Node acceptNode : acceptNodes) {
    				if (nodeCopy.getNodeID().equals(acceptNode.getNodeID())) {
    					deleteNodeType(outputSpaceID, nodeCopy.getNodeID());
    				}
    			}
    		}
    		if (startNodes1.size() > 0) {
    			Node startNode1 = startNodes1.iterator().next();
    			deleteNodeType(outputSpaceID, startNode1.getNodeID());
    			createEdge(outputSpaceID, nodeCopy.getNodeID(), startNode1.getNodeID());
    		}
    		for (Node acceptNode1 : acceptNodes1) {
    			for (Edge removedEdge : removedEdges) {
    				if (removedEdge.hasRoles()) {
    					createComponentEdge(outputSpaceID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    				} else {
    					createEdge(outputSpaceID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID());
    				}
    			}
    		}
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	Set<String> inputBranchIDs1 = getBranchIDs(inputSpaceID1);
    	Set<String> inputBranchIDs2 = getBranchIDs(inputSpaceID2);
    	
    	aggregateBranches(inputSpaceID1, inputSpaceID2, inputBranchIDs1, inputBranchIDs2, outputSpaceID);
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);

    	if (!inputBranchIDs1.contains(headBranchID2) && !inputBranchIDs1.contains(outputSpaceID) 
    			&& !inputBranchIDs2.contains(outputSpaceID)) {
    		insertBranch(outputSpaceID, headBranchID1, headBranchID2, targetNodeID, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID1, outputSpaceID);
    		fastForwardBranch(outputSpaceID, headBranchID2, outputSpaceID);
    		deleteBranch(outputSpaceID, outputSpaceID);
    	}
    	
    	selectHeadBranch(outputSpaceID, headBranchID1);
    }
	
	public void deleteDesignSpace(String targetSpaceID) {
    	if (findDesignSpace(targetSpaceID) != null) {
    		designSpaceRepository.deleteDesignSpace(targetSpaceID);
    	}
    }
	
	public void deleteNodes(String targetSpaceID) {
    	designSpaceRepository.deleteNodes(targetSpaceID);
    }
	
	public void deleteBranch(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteBranch(targetSpaceID, targetBranchID);
	}
    
    private DesignSpace findDesignSpace(String targetSpaceID) {
    	return designSpaceRepository.findBySpaceID(targetSpaceID);
    }
    
    private void copyDesignSpace(String inputSpaceID, String outputSpaceID) {
        designSpaceRepository.copyDesignSpace(inputSpaceID, outputSpaceID);
    }
    
    private void fastForwardBranch(String targetSpaceID, String targetBranchID1, String targetBranchID2) {
    	designSpaceRepository.fastForwardBranch(targetSpaceID, targetBranchID1, targetBranchID2);
    }
    
    private void copyBranch(String targetSpaceID, String inputBranchID, String outputBranchID) {
    	designSpaceRepository.copyBranch(targetSpaceID, inputBranchID, outputBranchID);
    }
    
    private void copyBranch(String inputSpaceID, String inputBranchID, String outputSpaceID, String outputBranchID) {
        designSpaceRepository.copyBranch(inputSpaceID, inputBranchID, outputSpaceID, outputBranchID);
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
    
    private void selectHeadBranch(String targetSpaceID, String targetBranchID) {
    	designSpaceRepository.selectHeadBranch(targetSpaceID, targetBranchID);
    }
    
    private void deleteNodeCopyIndices(String targetSpaceID) {
    	designSpaceRepository.deleteNodeCopyIndices(targetSpaceID);
    }
    
    private void deleteNodeCopyIndices(String targetSpaceID, String targetBranchID) {
    	designSpaceRepository.deleteNodeCopyIndices(targetSpaceID, targetBranchID);
    }
    
    private void deleteCommitCopyIndices(String targetSpaceID, String targetBranchID) {
    	designSpaceRepository.deleteCommitCopyIndices(targetSpaceID, targetBranchID);
    }
    
    private Set<Node> getNodesByType(String targetSpaceID, String nodeType) {
    	return designSpaceRepository.getNodesByType(targetSpaceID, nodeType);
    }
    
    private Set<Node> getNodesByType(String targetSpaceID, String targetBranchID, String nodeType) {
    	return designSpaceRepository.getNodesByType(targetSpaceID, targetBranchID, nodeType);
    }
    
    private Set<String> getBranchIDs(String targetSpaceID) {
    	Set<String> branchIDs = new HashSet<String>();
    	List<Map<String, Object>> rows = designSpaceRepository.getBranchIDs(targetSpaceID);
    	for (Map<String, Object> row : rows) {
    		branchIDs.add((String) row.get("branchID"));
    	}
    	return branchIDs;
    }
    
    private String getHeadBranchID(String targetSpaceID) {
    	List<Map<String, Object>> rows = designSpaceRepository.getHeadBranchID(targetSpaceID);
    	if (rows.size() == 1) {
    		return (String) rows.get(0).get("headBranchID");
    	} else {
    		return null;
    	}
    }
    
    private void aggregateBranches(String inputSpaceID1, String inputSpaceID2, Set<String> inputBranchIDs1,
    		Set<String> inputBranchIDs2, String outputSpaceID) {
    	for (String inputBranchID1 : inputBranchIDs1) {
    		copyBranch(inputSpaceID1, inputBranchID1, outputSpaceID, inputBranchID1);
    	} 
    	for (String inputBranchID2 : inputBranchIDs2) {
    		if (!inputBranchIDs1.contains(inputBranchID2)) {
    			copyBranch(inputSpaceID2, inputBranchID2, outputSpaceID, inputBranchID2);
    		}
    	}
    }
    
    public void createNode(String targetSpaceID, String outputNodeID) {
    	designSpaceRepository.createNode(targetSpaceID, outputNodeID);
    }
    
    private void createTypedNode(String targetSpaceID, String outputNodeID, String nodeType) {
    	designSpaceRepository.createTypedNode(targetSpaceID, outputNodeID, nodeType);
    }
    
    private void createTypedNode(String targetSpaceID, String targetBranchID, String outputNodeID, String nodeType) {
    	designSpaceRepository.createTypedNode(targetSpaceID, targetBranchID, outputNodeID, nodeType);
    }
    
    private void deleteNodeType(String targetSpaceID, String targetNodeID) {
    	designSpaceRepository.deleteNodeType(targetSpaceID, targetNodeID);
    }
    
    private void deleteNodeType(String targetSpaceID, String targetBranchID, String targetNodeID) {
    	designSpaceRepository.deleteNodeType(targetSpaceID, targetBranchID, targetNodeID);
    }
    
    private Set<Edge> getOutgoingEdges(String targetSpaceID, String targetNodeID) {
    	return designSpaceRepository.getOutgoingEdges(targetSpaceID, targetNodeID);
    }
    
    private Set<Edge> getOutgoingEdges(String targetSpaceID, String targetBranchID, String targetNodeID) {
    	return designSpaceRepository.getOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
    }
    
    public void createEdge(String targetSpaceID, String targetTailID, String targetHeadID) {
    	designSpaceRepository.createEdge(targetSpaceID, targetTailID, targetHeadID);
    }
    
    private void createEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID) {
    	designSpaceRepository.createEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID);
    }
    
    private void createComponentEdge(String targetSpaceID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
    	designSpaceRepository.createComponentEdge(targetSpaceID, targetTailID, targetHeadID, componentIDs, componentRoles);
    }
    
    private void createComponentEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
    	designSpaceRepository.createComponentEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID, componentIDs, componentRoles);
    }
    
    private Set<Edge> removeOutgoingEdges(String targetSpaceID, String targetNodeID) {
    	Set<Edge> removedEdges = getOutgoingEdges(targetSpaceID, targetNodeID);
    	if (removedEdges.size() > 0) {
    		designSpaceRepository.removeOutgoingEdges(targetSpaceID, targetNodeID);
    	}
    	return removedEdges;
    }
    
    private Set<Edge> removeOutgoingEdges(String targetSpaceID, String targetBranchID, String targetNodeID) {
    	Set<Edge> removedEdges = getOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
    	if (removedEdges.size() > 0) {
    		designSpaceRepository.removeOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
    	}
    	return removedEdges;
    }
    
    private Map<String, Object> spaceMapToD3Format(List<Map<String, Object>> spaceMap) {
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
    
    private Map<String, Object> branchMapToD3Format(List<Map<String, Object>> branchMap) {
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

    private Map<String, Object> makeD3(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
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
