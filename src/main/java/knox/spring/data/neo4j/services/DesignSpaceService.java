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

    private Map<String, Object> toD3Format(Iterator<Map<String, Object>> result) {
    	Map<String, Object> graph = new HashMap<String, Object>();
        List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
        int i = 0;
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            if (graph.isEmpty()) {
            	graph.put("displayID", row.get("graphID"));
            }
            Map<String, Object> tail = map("displayID", row.get("tailID"), "nodeType", row.get("tailType"));
            int source = nodes.indexOf(tail);
            if (source == -1) {
            	nodes.add(tail);
            	source = i++;
            }
            Map<String, Object> head = map("displayID", row.get("headID"), "nodeType", row.get("headType"));
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
        graph.putAll(map("nodes", nodes, "links", links));
        return graph;
    }

    private Map<String, Object> map(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
    }

    public Map<String, Object> findDesignSpace(String id) {
        Iterator<Map<String, Object>> result = designSpaceRepository.findDesignSpace(id).iterator();
        return toD3Format(result);
    }
    
    public Map<String, Object> joinDesignSpaces(String id1, String id2, String id3) {
    	Iterator<Map<String, Object>> result = designSpaceRepository.joinDesignSpaces(id1, id2, id3).iterator();
    	if (result.hasNext()) {
    		return result.next();
    	} else {
    		return new HashMap<String, Object>();
    	}
    }
    
}
