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
        List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
        int i = 0;
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
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
            link.put("componentRole", row.get("componentRole"));
            links.add(link);
        }
        return map("nodes", nodes, "links", links);
    }

    private Map<String, Object> map(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> result = new HashMap<String, Object>(2);
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
    }

    public Map<String, Object> graph(int limit) {
        Iterator<Map<String, Object>> result = designSpaceRepository.graph(limit).iterator();
        return toD3Format(result);
    }
}
