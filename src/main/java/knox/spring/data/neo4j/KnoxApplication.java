package knox.spring.data.neo4j;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.services.DesignSpaceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@Configuration
@Import(MyNeo4jConfiguration.class)
@RestController("/")
public class KnoxApplication extends WebMvcConfigurerAdapter {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(KnoxApplication.class, args);
    }

    @Autowired
    DesignSpaceService designSpaceService;

    @RequestMapping("/d3GraphDesignSpace")
    public Map<String, Object> d3GraphDesignSpace(@RequestParam(value = "targetID", required = false) String targetID) {
        return designSpaceService.d3GraphDesignSpace(targetID == null ? "test1" : targetID);
    }
    
    @RequestMapping("/deleteDesignSpace")
    public Map<String, Object> deleteDesignSpace(@RequestParam(value = "targetID", required = false) String targetID) {
    	DesignSpace deleted = designSpaceService.deleteDesignSpace(targetID == null ? "test1" : targetID);
    	Map<String, Object> target = new HashMap<String, Object>();
    	if (deleted != null) {
    		target.put("spaceID", deleted.getSpaceID());
    	}
        return target;
    }
    
    @RequestMapping("/joinDesignSpaces")
    public Map<String, Object> joinDesignSpaces(@RequestParam(value = "inputID1", required = false) String inputID1, 
    		@RequestParam(value = "inputID2", required = false) String inputID2,
    		@RequestParam(value = "outputID", required = false) String outputID) {
    	DesignSpace joinSpace = designSpaceService.joinDesignSpaces(inputID1 == null ? "test1" : inputID1, inputID2 == null ? "test2" : inputID2, 
        		outputID == null ? "test3" : outputID);
    	Map<String, Object> output = new HashMap<String, Object>();
    	if (joinSpace != null) {
    		output.put("spaceID", joinSpace.getSpaceID());
    	}
        return output;
    }
    
    @RequestMapping("/orDesignSpaces")
    public Map<String, Object> orDesignSpaces(@RequestParam(value = "inputID1", required = false) String inputID1, 
    		@RequestParam(value = "inputID2", required = false) String inputID2,
    		@RequestParam(value = "outputID", required = false) String outputID) {
    	DesignSpace orSpace = designSpaceService.orDesignSpaces(inputID1 == null ? "test1" : inputID1, inputID2 == null ? "test2" : inputID2, 
        		outputID == null ? "test3" : outputID);
    	Map<String, Object> output = new HashMap<String, Object>();
    	if (orSpace != null) {
    		output.put("spaceID", orSpace.getSpaceID());
    	}
        return output;
    }
    
    @RequestMapping("/andDesignSpaces")
    public Map<String, Object> andDesignSpaces(@RequestParam(value = "inputID1", required = false) String inputID1, 
    		@RequestParam(value = "inputID2", required = false) String inputID2,
    		@RequestParam(value = "outputID", required = false) String outputID) {
    	DesignSpace andSpace = designSpaceService.andDesignSpaces(inputID1 == null ? "test1" : inputID1, inputID2 == null ? "test2" : inputID2, 
        		outputID == null ? "test3" : outputID);
    	Map<String, Object> output = new HashMap<String, Object>();
    	if (andSpace != null) {
    		output.put("spaceID", andSpace.getSpaceID());
    	}
        return output;
    }
    
    @RequestMapping("/insertDesignSpace")
    public Map<String, Object> insertDesignSpace(@RequestParam(value = "inputID1", required = false) String inputID1,
    		@RequestParam(value = "inputID2", required = false) String inputID2,
    		@RequestParam(value = "nodeID", required = false) String nodeID,
    		@RequestParam(value = "outputID", required = false) String outputID) {
    	DesignSpace insertSpace = designSpaceService.insertDesignSpace(inputID1 == null ? "test1" : inputID1, inputID2 == null ? "test1" : inputID2,
        		nodeID == null ? "test2" : nodeID, outputID == null ? "test3" : outputID);
    	Map<String, Object> output = new HashMap<String, Object>();
    	if (insertSpace != null) {
    		output.put("spaceID", insertSpace.getSpaceID());
    	}
        return output; 
    }

}
