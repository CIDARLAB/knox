package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.repositories.DesignSpaceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class DesignSpaceService {

    @Autowired DesignSpaceRepository designSpaceRepository;

    public Map<String, Object> d3GraphDesignSpace(String targetID) {
        Iterator<Map<String, Object>> rows = designSpaceRepository.mapDesignSpace(targetID).iterator();
        return toD3Format(rows);
    }
    
    public Map<String, Object> deleteDesignSpace(String targetID) {
        Iterator<Map<String, Object>> rows = designSpaceRepository.deleteDesignSpace(targetID).iterator();
        if (rows.hasNext()) {
    		return rows.next();
    	} else {
    		return new HashMap<String, Object>();
    	}
    }
    
    public Map<String, Object> copyDesignSpace(String inputID, String outputID, int idIndex) {
        Iterator<Map<String, Object>> rows = designSpaceRepository.copyDesignSpace(inputID, outputID, idIndex).iterator();
        if (rows.hasNext()) {
    		return rows.next();
    	} else {
    		return new HashMap<String, Object>();
    	}
    }
    
    public Map<String, Object> joinDesignSpaces(String inputID1, String inputID2, String outputID) {
    	Map<String, Object> copy1 = copyDesignSpace(inputID1, outputID, 1);
    	
    	Set<String> startIDs1 = findNodesByType(outputID, NodeType.START.value);
    	Set<String> acceptIDs1 = findNodesByType(outputID, NodeType.ACCEPT.value);
    	
    	Map<String, Object> copy2 = copyDesignSpace(inputID2, outputID, (int) copy1.get("nextIndex"));
    	
    	Set<String> startIDs = findNodesByType(outputID, NodeType.START.value);
    	
    	if (startIDs.size() > 0) {
    		for (String startID : startIDs) {
    			String startID1 = startIDs1.iterator().next();
    			if (!startID.equals(startID1)) {
    				removeNodeType(outputID, startID);
    				for (String acceptID1 : acceptIDs1) {
    					removeNodeType(outputID, acceptID1);
    					createEdge(outputID, acceptID1, startID);
    		    	}
    			}
    		}
    	}
    	
    	return copy2;
    }
    
    public Map<String, Object> orDesignSpaces(String inputID1, String inputID2, String outputID) {
    	Map<String, Object> copy1 = copyDesignSpace(inputID1, outputID, 1);
    	Map<String, Object> copy2 = copyDesignSpace(inputID2, outputID, (int) copy1.get("nextIndex"));

    	Set<String> startIDs = findNodesByType(outputID, NodeType.START.value);
    
    	String startID0 = "n0";
    	createTypedNode(outputID, startID0, NodeType.START.value);
    	
    	for (String startID : startIDs) {
    		removeNodeType(outputID, startID);
    		createEdge(outputID, startID0, startID);
    	}
    	
    	return copy2;
    }
    
    public Map<String, Object> andDesignSpaces(String inputID1, String inputID2, String outputID) {
    	joinDesignSpaces(inputID1, inputID2, "knox1");
    	joinDesignSpaces(inputID2, inputID1, "knox2");
    	
    	Map<String, Object> output = orDesignSpaces("knox1", "knox2", outputID);
    	
    	designSpaceRepository.deleteDesignSpace("knox1");
    	designSpaceRepository.deleteDesignSpace("knox2");
    	
    	return output;
    }
    
    public Map<String, Object> insertDesignSpace(String inputID1, String inputID2, String nodeID, String outputID) {
    	Map<String, Object> copy1 = copyDesignSpace(inputID1, outputID, 1);

    	Set<String> startIDs1 = findNodesByType(outputID, NodeType.START.value);
    	Set<String> acceptIDs1 = findNodesByType(outputID, NodeType.ACCEPT.value);
    	
    	Map<String, Object> copy2 = copyDesignSpace(inputID2, outputID, (int) copy1.get("nextIndex"));
    	
    	Set<String> nodeIDs = findNodeCopy(inputID2, nodeID, outputID);
    	String copyID = (nodeIDs.size() > 0) ? nodeIDs.iterator().next() : "";
    	
    	List<Map<String, Object>> edges = (copyID.length() > 0) ? deleteOutgoingEdges(outputID, copyID) : new LinkedList<Map<String, Object>>();
    	
    	if (edges.size() > 0) {
    		for (String acceptID1 : acceptIDs1) {
    			removeNodeType(outputID, acceptID1);
    		}
    	}
    	if (startIDs1.size() > 0) {
    		String startID1 = startIDs1.iterator().next();
    		removeNodeType(outputID, startID1);
    		if (copyID.length() > 0) {
    			createEdge(outputID, copyID, startID1);
    		}
    	}
    	for (String acceptID1 : acceptIDs1) {
    		for (Map<String, Object> edge : edges) {
    			if (edge.containsKey("componentRole")) {
    				createComponentEdge(outputID, acceptID1, (String) edge.get("headID"), (String) edge.get("componentID"), (String) edge.get("componentRole"));
    			} else {
    				createEdge(outputID, acceptID1, (String) edge.get("headID"));
    			}
    		}
    	}
    	
    	return copy2;
    }
    
    private Set<String> findNodesByType(String targetID, String nodeType) {
    	Set<String> nodeIDs = new HashSet<String>();
    	List<Map<String, Object>> rows = designSpaceRepository.findNodesByType(targetID, nodeType);
    	if (rows.size() > 0) {
    		nodeIDs.add((String) rows.get(0).get("nodeID"));
    	}
    	return nodeIDs;
    }
    
    private boolean removeNodeType(String targetID, String nodeID) {
    	List<Map<String, Object>> rows = designSpaceRepository.removeNodeType(targetID, nodeID);
    	return rows.size() > 0;
    }
    
    private Set<String> findNodeCopy(String targetID1, String nodeID, String targetID2) {
    	Set<String> nodeIDs = new HashSet<String>();
    	List<Map<String, Object>> rows = designSpaceRepository.findNodeCopy(targetID1, nodeID, targetID2);
    	if (rows.size() > 0) {
    		nodeIDs.add((String) rows.get(0).get("nodeID"));
    	}
    	return nodeIDs;
    }
    
    private boolean createEdge(String targetID, String tailID, String headID) {
    	List<Map<String, Object>> rows = designSpaceRepository.createEdge(targetID, tailID, headID);
    	return rows.size() > 0;
    }
    
    private boolean createComponentEdge(String targetID, String tailID, String headID, String componentID, String componentRole) {
    	List<Map<String, Object>> rows = designSpaceRepository.createComponentEdge(targetID, tailID, headID, componentID, componentRole);
    	return rows.size() > 0;
    }
    
    private boolean createTypedNode(String targetID, String nodeID, String nodeType) {
    	List<Map<String, Object>> rows = designSpaceRepository.createTypedNode(targetID, nodeID, nodeType);
    	return rows.size() > 0;
    }
    
    private List<Map<String, Object>> deleteOutgoingEdges(String targetID, String nodeID) {
    	List<Map<String, Object>> rows = designSpaceRepository.deleteOutgoingEdges(targetID, nodeID);
    	for (Map<String, Object> row : rows) {
    		if (row.containsKey("componentID")) {
    			row.remove("componentID");
    		}
    		if (row.containsKey("componentRole")) {
    			row.remove("componentRole");
    		}
    	}
    	return rows;
    }
    
    private Map<String, Object> toD3Format(Iterator<Map<String, Object>> rows) {
    	Map<String, Object> d3Graph = new HashMap<String, Object>();
        List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
        int i = 0;
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            if (d3Graph.isEmpty()) {
            	d3Graph.put("spaceID", row.get("spaceID"));
            }
            if (d3Graph.get("spaceID").equals(row.get("spaceID"))) {
            	Map<String, Object> tail = buildMap("nodeID", row.get("tailID"), "nodeType", row.get("tailType"));
            	int source = nodes.indexOf(tail);
            	if (source == -1) {
            		nodes.add(tail);
            		source = i++;
            	}
            	Map<String, Object> head = buildMap("nodeID", row.get("headID"), "nodeType", row.get("headType"));
            	int target = nodes.indexOf(head);
            	if (target == -1) {
            		nodes.add(head);
            		target = i++;
            	}
            	Map<String, Object> link = buildMap("source", source, "target", target);
            	if (row.containsKey("componentRole")) {
            		link.put("componentRole", row.get("componentRole"));
            	}
            	links.add(link);
            }
        }
        d3Graph.putAll(buildMap("nodes", nodes, "links", links));
        return d3Graph;
    }

    private Map<String, Object> buildMap(String key1, Object value1, String key2, Object value2) {
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
