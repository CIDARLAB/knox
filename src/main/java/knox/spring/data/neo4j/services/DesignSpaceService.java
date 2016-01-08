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

    private Map<String, Object> toD3Format(Iterator<Map<String, Object>> spaceProperties) {
    	Map<String, Object> d3Graph = new HashMap<String, Object>();
        List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
        int i = 0;
        while (spaceProperties.hasNext()) {
            Map<String, Object> row = spaceProperties.next();
            if (d3Graph.isEmpty()) {
            	d3Graph.put("spaceID", row.get("spaceID"));
            }
            if (d3Graph.get("spaceID").equals(row.get("spaceID"))) {
            	Map<String, Object> tail = map("nodeID", row.get("tailID"), "nodeType", row.get("tailType"));
            	int source = nodes.indexOf(tail);
            	if (source == -1) {
            		nodes.add(tail);
            		source = i++;
            	}
            	Map<String, Object> head = map("nodeID", row.get("headID"), "nodeType", row.get("headType"));
            	int target = nodes.indexOf(head);
            	if (target == -1) {
            		nodes.add(head);
            		target = i++;
            	}
            	Map<String, Object> link = map("source", source, "target", target);
            	if (row.get("componentRole") != null) {
            		link.put("componentRole", row.get("componentRole"));
            	}
            	links.add(link);
            }
        }
        d3Graph.putAll(map("nodes", nodes, "links", links));
        return d3Graph;
    }

    private Map<String, Object> map(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
    }

    public Map<String, Object> d3GraphDesignSpace(String targetID) {
        Iterator<Map<String, Object>> spaceProperties = designSpaceRepository.mapDesignSpace(targetID).iterator();
        return toD3Format(spaceProperties);
    }
    
    public Map<String, Object> deleteDesignSpace(String targetID) {
        Iterator<Map<String, Object>> deleteProperties = designSpaceRepository.deleteDesignSpace(targetID).iterator();
        if (deleteProperties.hasNext()) {
    		return deleteProperties.next();
    	} else {
    		return new HashMap<String, Object>();
    	}
    }
    
    public Map<String, Object> copyDesignSpace(String inputID, String outputID) {
        Iterator<Map<String, Object>> copyProperties = designSpaceRepository.copyDesignSpace(inputID, outputID).iterator();
        if (copyProperties.hasNext()) {
    		return copyProperties.next();
    	} else {
    		return new HashMap<String, Object>();
    	}
    }
    
    public Map<String, Object> joinDesignSpaces(String inputID1, String inputID2, String outputID) {
    	designSpaceRepository.copyDesignSpace(inputID1, outputID);
    	designSpaceRepository.copyDesignSpace(inputID2, outputID);
    	
    	Iterator<Map<String, Object>> startProperties = designSpaceRepository.getNodesByType(inputID2, NodeType.START.value).iterator();
    	String startID;
    	if (startProperties.hasNext()) {
    		startID = outputID + Integer.toString((Integer) startProperties.next().get("id"));
    	} else {
    		startID = "";
    	}
    	if (startID.length() > 0) {
    		designSpaceRepository.removeNodeType(startID);
    	}
    	
    	Iterator<Map<String, Object>> acceptProperties = designSpaceRepository.getNodesByType(inputID1, NodeType.ACCEPT.value).iterator();
    	Set<String> acceptIDs = new HashSet<String>();
    	while (acceptProperties.hasNext()) {
    		acceptIDs.add(outputID + Integer.toString((Integer) acceptProperties.next().get("id")));
    	}
    	for (String acceptID : acceptIDs) {
    		designSpaceRepository.removeNodeType(acceptID);
    		if (startID.length() > 0) {
    			designSpaceRepository.connectNodes(acceptID, startID);
    		}
    	}
    	
    	Map<String, Object> result = new HashMap<String, Object>();
    	result.put("spaceID", outputID);
    	return result;
    }
    
    public Map<String, Object> orDesignSpaces(String inputID1, String inputID2, String outputID) {
    	designSpaceRepository.copyDesignSpace(inputID1, outputID);
    	designSpaceRepository.copyDesignSpace(inputID2, outputID);
    	
    	Iterator<Map<String, Object>> startProperties1 = designSpaceRepository.getNodesByType(inputID1, NodeType.START.value).iterator();
    	String startID1;
    	if (startProperties1.hasNext()) {
    		startID1 = outputID + Integer.toString((Integer) startProperties1.next().get("id"));
    	} else {
    		startID1 = "";
    	}
    	if (startID1.length() > 0) {
    		designSpaceRepository.removeNodeType(startID1);
    	}
    	
    	Iterator<Map<String, Object>> startProperties2 = designSpaceRepository.getNodesByType(inputID2, NodeType.START.value).iterator();
    	String startID2;
    	if (startProperties2.hasNext()) {
    		startID2 = outputID + Integer.toString((Integer) startProperties2.next().get("id"));
    	} else {
    		startID2 = "";
    	}
    	if (startID2.length() > 0) {
    		designSpaceRepository.removeNodeType(startID2);
    	}
    	
    	if (startID1.length() > 0 && startID2.length() > 0) {
    		String startID3 = outputID + startID1 + outputID + startID2;
    		designSpaceRepository.createTypedNode(outputID, startID3, NodeType.START.value);
    		designSpaceRepository.connectNodes(startID3, startID1);
    		designSpaceRepository.connectNodes(startID3, startID2);
    	}
    	
    	Map<String, Object> result = new HashMap<String, Object>();
    	result.put("spaceID", outputID);
    	return result;
    }
    
    public Map<String, Object> andDesignSpaces(String inputID1, String inputID2, String outputID) {
    	joinDesignSpaces(inputID1, inputID2, "knox1");
    	joinDesignSpaces(inputID2, inputID1, "knox2");
    	
    	Map<String, Object> andProperties = orDesignSpaces("knox1", "knox2", outputID);
    	
    	designSpaceRepository.deleteDesignSpace("knox1");
    	designSpaceRepository.deleteDesignSpace("knox2");
    	
    	return andProperties;
    }
    
    public Map<String, Object> insertDesignSpace(String inputID, String targetNodeID, String outputID) {
//    	Iterator<Map<String, Object>> result = designSpaceRepository.insertDesignSpace(inputID, targetNodeID, outputID).iterator();
//    	if (result.hasNext()) {
//    		return result.next();
//    	} else {
    		return new HashMap<String, Object>();
//    	}
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
