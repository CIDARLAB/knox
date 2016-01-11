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
    					connectNodes(outputID, acceptID1, startID);
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
    		connectNodes(outputID, startID0, startID);
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
    	
    	Set<String> headIDs = (copyID.length() > 0) ? deleteOutgoingEdges(outputID, copyID) : new HashSet<String>();
    	
    	if (headIDs.size() > 0) {
    		for (String acceptID1 : acceptIDs1) {
    			removeNodeType(outputID, acceptID1);
    		}
    	}
    	if (startIDs1.size() > 0) {
    		String startID1 = startIDs1.iterator().next();
    		removeNodeType(outputID, startID1);
    		if (copyID.length() > 0) {
    			connectNodes(outputID, copyID, startID1);
    		}
    	}
    	for (String acceptID1 : acceptIDs1) {
    		for (String headID : headIDs) {
    			connectNodes(outputID, acceptID1, headID);
    		}
    	}
    	
    	return copy2;
    }
    
    private Set<String> findNodesByType(String targetID, String nodeType) {
    	Set<String> nodeIDs = new HashSet<String>();
    	Iterator<Map<String, Object>> rows = designSpaceRepository.findNodesByType(targetID, nodeType).iterator();
    	while (rows.hasNext()) {
    		nodeIDs.add((String) rows.next().get("nodeID"));
    	}
    	return nodeIDs;
    }
    
    private boolean removeNodeType(String targetID, String nodeID) {
    	Iterator<Map<String, Object>> rows = designSpaceRepository.removeNodeType(targetID, nodeID).iterator();
    	return rows.hasNext();
    }
    
    private Set<String> findNodeCopy(String targetID1, String nodeID, String targetID2) {
    	Set<String> nodeIDs = new HashSet<String>();
    	Iterator<Map<String, Object>> rows = designSpaceRepository.findNodeCopy(targetID1, nodeID, targetID2).iterator();
    	while (rows.hasNext()) {
    		nodeIDs.add((String) rows.next().get("nodeID"));
    	}
    	return nodeIDs;
    }
    
    private boolean connectNodes(String targetID, String tailID, String headID) {
    	Iterator<Map<String, Object>> rows = designSpaceRepository.connectNodes(targetID, tailID, headID).iterator();
    	return rows.hasNext();
    }
    
    private boolean createTypedNode(String targetID, String nodeID, String nodeType) {
    	Iterator<Map<String, Object>> rows = designSpaceRepository.createTypedNode(targetID, nodeID, nodeType).iterator();
    	return rows.hasNext();
    }
    
    private Set<String> deleteOutgoingEdges(String targetID, String nodeID) {
    	Set<String> headIDs = new HashSet<String>();
    	Iterator<Map<String, Object>> rows = designSpaceRepository.deleteOutgoingEdges(targetID, nodeID).iterator();
    	while (rows.hasNext()) {
    		Map<String, Object> row = rows.next();
    		if (row.get("headID") != null) {
    			headIDs.add((String) row.get("headID"));
    		}
    	}
    	return headIDs;
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
            	if (row.get("componentRole") != null) {
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
