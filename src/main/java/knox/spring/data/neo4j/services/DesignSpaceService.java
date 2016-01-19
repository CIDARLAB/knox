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

    public Map<String, Object> d3GraphDesignSpace(String targetID) {
        return toD3Format(designSpaceRepository.mapDesignSpace(targetID));
    }
    
    public DesignSpace joinDesignSpaces(String inputID1, String inputID2, String outputID) {
    	DesignSpace copy1 = copyDesignSpace(inputID1, outputID, 0);
    	
    	Set<Node> startNodes1 = findNodesByType(outputID, NodeType.START.value);
    	Set<Node> acceptNodes1 = findNodesByType(outputID, NodeType.ACCEPT.value);
    	
    	DesignSpace copy2 = copyDesignSpace(inputID2, outputID, copy1.getNodeLinks().size());
    	
    	Set<Node> startNodes = findNodesByType(outputID, NodeType.START.value);
    	
    	if (startNodes1.size() > 0) {
    		Node startNode1 = startNodes1.iterator().next();
    		for (Node startNode : startNodes) {
    			if (!startNode.getNodeID().equals(startNode1.getNodeID())) {
    				removeNodeType(outputID, startNode.getNodeID());
    				for (Node acceptNode1 : acceptNodes1) {
    					removeNodeType(outputID, acceptNode1.getNodeID());
    					createEdge(outputID, acceptNode1.getNodeID(), startNode.getNodeID());
    				}
    			}
    		}
    	}
    	
    	removeCopyIDs(outputID);
    	
    	return copy2;
    }
    
    public DesignSpace orDesignSpaces(String inputID1, String inputID2, String outputID) {
    	DesignSpace copy1 = copyDesignSpace(inputID1, outputID, 1);
    	DesignSpace copy2 = copyDesignSpace(inputID2, outputID, copy1.getNodeLinks().size() + 1);

    	Set<Node> startNodes = findNodesByType(outputID, NodeType.START.value);
    
    	Node startNode0 = createTypedNode(outputID, "n0", NodeType.START.value);
    	
    	for (Node startNode : startNodes) {
    		removeNodeType(outputID, startNode.getNodeID());
    		createEdge(outputID, startNode0.getNodeID(), startNode.getNodeID());
    	}
    	
    	removeCopyIDs(outputID);
    	
    	return copy2;
    }
    
    public DesignSpace andDesignSpaces(String inputID1, String inputID2, String outputID) {
    	joinDesignSpaces(inputID1, inputID2, "knox1");
    	joinDesignSpaces(inputID2, inputID1, "knox2");
    	
    	DesignSpace orSpace = orDesignSpaces("knox1", "knox2", outputID);
    	
    	designSpaceRepository.deleteDesignSpace("knox1");
    	designSpaceRepository.deleteDesignSpace("knox2");
    	
    	return orSpace;
    }
    
	public DesignSpace insertDesignSpace(String inputID1, String inputID2, String nodeID, String outputID) {
    	DesignSpace copy1 = copyDesignSpace(inputID1, outputID, 0);

    	Set<Node> startNodes1 = findNodesByType(outputID, NodeType.START.value);
    	Set<Node> acceptNodes1 = findNodesByType(outputID, NodeType.ACCEPT.value);
    	
    	DesignSpace copy2 = copyDesignSpace(inputID2, outputID, copy1.getNodeLinks().size());
    	
    	Node nodeCopy = findNodeCopy(inputID2, nodeID, outputID);
    	
    	if (nodeCopy != null) {
    		Set<Edge> deletedEdges = removeOutgoingEdges(outputID, nodeCopy.getNodeID());

    		if (deletedEdges.size() > 0) {
    			for (Node acceptNode1 : acceptNodes1) {
    				removeNodeType(outputID, acceptNode1.getNodeID());
    			}
    		} else {
    			Set<Node> acceptNodes = findNodesByType(outputID, NodeType.ACCEPT.value);
    			for (Node acceptNode : acceptNodes) {
    				if (nodeCopy.getNodeID().equals(acceptNode.getNodeID())) {
    					removeNodeType(outputID, nodeCopy.getNodeID());
    				}
    			}
    		}
    		if (startNodes1.size() > 0) {
    			Node startNode1 = startNodes1.iterator().next();
    			removeNodeType(outputID, startNode1.getNodeID());
    			createEdge(outputID, nodeCopy.getNodeID(), startNode1.getNodeID());
    		}
    		for (Node acceptNode1 : acceptNodes1) {
    			for (Edge deletedEdge : deletedEdges) {
    				if (deletedEdge.hasRoles()) {
    					createComponentEdge(outputID, acceptNode1.getNodeID(), deletedEdge.getHead().getNodeID(), deletedEdge.getComponentIDs(), deletedEdge.getComponentRoles());
    				} else {
    					createEdge(outputID, acceptNode1.getNodeID(), deletedEdge.getHead().getNodeID());
    				}
    			}
    		}
    	}
    	
    	removeCopyIDs(outputID);
    	
    	return copy2;
    }
	
	public DesignSpace deleteDesignSpace(String targetID) {
    	DesignSpace deleted = findDesignSpace(targetID);
    	if (deleted != null) {
    		designSpaceRepository.deleteDesignSpace(targetID);
    	}
    	return deleted;
    }
    
    private DesignSpace findDesignSpace(String targetID) {
    	return designSpaceRepository.findBySpaceID(targetID);
    }
    
    private DesignSpace copyDesignSpace(String inputID, String outputID, int idIndex) {
        designSpaceRepository.copyDesignSpace(inputID, outputID, idIndex);
        return findDesignSpace(outputID);
    }
    
    private Node findNodeCopy(String targetID1, String nodeID, String targetID2) {
    	Set<Node> nodeCopy = designSpaceRepository.findNodeCopy(targetID1, nodeID, targetID2);
    	if (nodeCopy.size() > 0) {
    		return nodeCopy.iterator().next();
    	} else {
    		return null;
    	}
    }
    
    private List<Map<String, Object>> removeCopyIDs(String targetID) {
    	return designSpaceRepository.removeCopyIDs(targetID);
    }
    
    private Set<Node> findNodesByType(String targetID, String nodeType) {
    	return designSpaceRepository.findNodesByType(targetID, nodeType);
    }
    
    private Node createTypedNode(String targetID, String nodeID, String nodeType) {
    	designSpaceRepository.createTypedNode(targetID, nodeID, nodeType);
    	return nodeRepository.findByNodeID(nodeID);
    }
    
    private Map<String, Object> removeNodeType(String targetID, String nodeID) {
    	List<Map<String, Object>> nodeType = designSpaceRepository.removeNodeType(targetID, nodeID);
    	if (nodeType.size() > 0) {
    		return nodeType.get(0); 
    	} else {
    		return null;
    	}
    }
    
    private Edge findEdge(String targetID, String tailID, String headID) {
    	Set<Edge> edge = designSpaceRepository.findEdge(targetID, tailID, headID);
    	if (edge.size() > 0) {
    		return edge.iterator().next();
    	} else {
    		return null;
    	}
    }
    
    private Set<Edge> findOutgoingEdges(String targetID, String nodeID) {
    	return designSpaceRepository.findOutgoingEdges(targetID, nodeID);
    }
    
    private Edge createEdge(String targetID, String tailID, String headID) {
    	designSpaceRepository.createEdge(targetID, tailID, headID);
    	return findEdge(targetID, tailID, headID);
    }
    
    private Edge createComponentEdge(String targetID, String tailID, String headID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
    	designSpaceRepository.createComponentEdge(targetID, tailID, headID, componentIDs, componentRoles);
    	return findEdge(targetID, tailID, headID);
    }
    
    private Set<Edge> removeOutgoingEdges(String targetID, String nodeID) {
    	Set<Edge> deletedEdges = findOutgoingEdges(targetID, nodeID);
    	if (deletedEdges.size() > 0) {
    		designSpaceRepository.removeOutgoingEdges(targetID, nodeID);
    	}
    	return deletedEdges;
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
