package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;
import knox.spring.data.neo4j.repositories.NodeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class DesignSpaceService {

    @Autowired DesignSpaceRepository designSpaceRepository;
    
    @Autowired NodeRepository nodeRepository;
    
    public void checkoutBranch(String targetSpaceID, String targetBranchID) {
    	designSpaceRepository.checkoutBranch(targetSpaceID, targetBranchID);
    }
    
    public void commitToBranch(String targetSpaceID) {
    	designSpaceRepository.commitToBranch(targetSpaceID);
    }

    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return toD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public void joinDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	copyDesignSpace(inputSpaceID1, outputSpaceID);
    	
    	Set<Node> startNodes1 = findNodesByType(outputSpaceID, NodeType.START.value);
    	Set<Node> acceptNodes1 = findNodesByType(outputSpaceID, NodeType.ACCEPT.value);
    	
    	copyDesignSpace(inputSpaceID2, outputSpaceID);
    	
    	Set<Node> startNodes = findNodesByType(outputSpaceID, NodeType.START.value);
    	
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
    	
    	deleteCopyIDs(outputSpaceID);
    }
    
    public void orDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	copyDesignSpace(inputSpaceID1, outputSpaceID);
    	copyDesignSpace(inputSpaceID2, outputSpaceID);

    	Set<Node> startNodes = findNodesByType(outputSpaceID, NodeType.START.value);
    
    	createTypedNode(outputSpaceID, "n00", NodeType.START.value);
    	
    	for (Node startNode : startNodes) {
    		deleteNodeType(outputSpaceID, startNode.getNodeID());
    		createEdge(outputSpaceID, "n00", startNode.getNodeID());
    	}
    	
    	deleteCopyIDs(outputSpaceID);
    }
    
    public void andDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) {
    	joinDesignSpaces(inputSpaceID1, inputSpaceID2, "knox1");
    	joinDesignSpaces(inputSpaceID2, inputSpaceID1, "knox2");
    	
    	orDesignSpaces("knox1", "knox2", outputSpaceID);
    	
    	designSpaceRepository.deleteDesignSpace("knox1");
    	designSpaceRepository.deleteDesignSpace("knox2");
    }
    
	public void insertDesignSpace(String inputSpaceID1, String inputSpaceID2, String targetNodeID, String outputSpaceID) {
    	copyDesignSpace(inputSpaceID1, outputSpaceID);

    	Set<Node> startNodes1 = findNodesByType(outputSpaceID, NodeType.START.value);
    	Set<Node> acceptNodes1 = findNodesByType(outputSpaceID, NodeType.ACCEPT.value);
    	
    	copyDesignSpace(inputSpaceID2, outputSpaceID);
    	
    	Node nodeCopy = findNodeCopy(inputSpaceID2, targetNodeID, outputSpaceID);
    	
    	if (nodeCopy != null) {
    		Set<Edge> removedEdges = removeOutgoingEdges(outputSpaceID, nodeCopy.getNodeID());

    		if (removedEdges.size() > 0) {
    			for (Node acceptNode1 : acceptNodes1) {
    				deleteNodeType(outputSpaceID, acceptNode1.getNodeID());
    			}
    		} else {
    			Set<Node> acceptNodes = findNodesByType(outputSpaceID, NodeType.ACCEPT.value);
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
    	
    	deleteCopyIDs(outputSpaceID);
    }
	
	public void deleteDesignSpace(String targetSpaceID) {
    	if (findDesignSpace(targetSpaceID) != null) {
    		designSpaceRepository.deleteDesignSpace(targetSpaceID);
    	}
    }
    
    private DesignSpace findDesignSpace(String targetSpaceID) {
    	return designSpaceRepository.findBySpaceID(targetSpaceID);
    }
    
    private void copyDesignSpace(String inputSpaceID, String outputSpaceID) {
        designSpaceRepository.copyDesignSpace(inputSpaceID, outputSpaceID);
    }
    
    private Node findNodeCopy(String targetSpaceID1, String targetNodeID, String targetSpaceID2) {
    	Set<Node> nodeCopy = designSpaceRepository.findNodeCopy(targetSpaceID1, targetNodeID, targetSpaceID2);
    	if (nodeCopy.size() > 0) {
    		return nodeCopy.iterator().next();
    	} else {
    		return null;
    	}
    }
    
    private void deleteCopyIDs(String targetSpaceID) {
    	designSpaceRepository.deleteCopyIDs(targetSpaceID);
    }
    
    private Set<Node> findNodesByType(String targetSpaceID, String nodeType) {
    	return designSpaceRepository.findNodesByType(targetSpaceID, nodeType);
    }
    
    private void createTypedNode(String targetSpaceID, String targetNodeID, String nodeType) {
    	designSpaceRepository.createTypedNode(targetSpaceID, targetNodeID, nodeType);
    }
    
    private void deleteNodeType(String targetSpaceID, String targetNodeID) {
    	designSpaceRepository.deleteNodeType(targetSpaceID, targetNodeID);
    }
    
    private Set<Edge> findOutgoingEdges(String targetSpaceID, String targetNodeID) {
    	return designSpaceRepository.findOutgoingEdges(targetSpaceID, targetNodeID);
    }
    
    private void createEdge(String targetSpaceID, String targetTailID, String targetHeadID) {
    	designSpaceRepository.createEdge(targetSpaceID, targetTailID, targetHeadID);
    }
    
    private void createComponentEdge(String targetSpaceID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
    	designSpaceRepository.createComponentEdge(targetSpaceID, targetTailID, targetHeadID, componentIDs, componentRoles);
    }
    
    private Set<Edge> removeOutgoingEdges(String targetSpaceID, String targetNodeID) {
    	Set<Edge> removedEdges = findOutgoingEdges(targetSpaceID, targetNodeID);
    	if (removedEdges.size() > 0) {
    		designSpaceRepository.deleteOutgoingEdges(targetSpaceID, targetNodeID);
    	}
    	return removedEdges;
    }
    
    private Map<String, Object> toD3Format(List<Map<String, Object>> spaceMap) {
    	Map<String, Object> d3Graph = new HashMap<String, Object>();
        List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
        int i = 0;
        for (Map<String, Object> row : spaceMap) {
            if (d3Graph.isEmpty()) {
            	d3Graph.put("spaceID", row.get("spaceID"));
            }
            if (d3Graph.get("spaceID").equals(row.get("spaceID"))) {
            	Map<String, Object> tail = makeMap("nodeID", row.get("tailID"), "nodeType", row.get("tailType"));
            	int source = nodes.indexOf(tail);
            	if (source == -1) {
            		nodes.add(tail);
            		source = i++;
            	}
            	Map<String, Object> head = makeMap("nodeID", row.get("headID"), "nodeType", row.get("headType"));
            	int target = nodes.indexOf(head);
            	if (target == -1) {
            		nodes.add(head);
            		target = i++;
            	}
            	Map<String, Object> link = makeMap("source", source, "target", target);
            	if (row.containsKey("componentRoles") && row.get("componentRoles") != null) {
            		link.put("componentRoles", row.get("componentRoles"));
            	}
            	links.add(link);
            }
        }
        d3Graph.putAll(makeMap("nodes", nodes, "links", links));
        return d3Graph;
    }

    private Map<String, Object> makeMap(String key1, Object value1, String key2, Object value2) {
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
