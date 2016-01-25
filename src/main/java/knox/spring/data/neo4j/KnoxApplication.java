package knox.spring.data.neo4j;

import knox.spring.data.neo4j.services.DesignSpaceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
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

    @RequestMapping(value = "/designSpace/graph/d3", method = RequestMethod.GET)
    public Map<String, Object> d3GraphDesignSpace(@RequestParam(value = "targetID", required = false) String targetID) {
        return designSpaceService.d3GraphDesignSpace(targetID == null ? "test1" : targetID);
    }
    
    @RequestMapping(value = "/designSpace", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteDesignSpace(@RequestParam(value = "targetID", required = true) String targetID) {
    	designSpaceService.deleteDesignSpace(targetID == null ? "test1" : targetID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace/join", method = RequestMethod.POST)
    public ResponseEntity<String> joinDesignSpaces(@RequestParam(value = "inputID1", required = true) String inputID1, 
    		@RequestParam(value = "inputID2", required = true) String inputID2,
    		@RequestParam(value = "outputID", required = true) String outputID) {
    	designSpaceService.joinDesignSpaces(inputID1, inputID2, outputID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace/or", method = RequestMethod.POST)
    public ResponseEntity<String> orDesignSpaces(@RequestParam(value = "inputID1", required = true) String inputID1, 
    		@RequestParam(value = "inputID2", required = true) String inputID2,
    		@RequestParam(value = "outputID", required = true) String outputID) {
    	designSpaceService.orDesignSpaces(inputID1, inputID2, outputID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace/and", method = RequestMethod.POST)
    public ResponseEntity<String> andDesignSpaces(@RequestParam(value = "inputID1", required = true) String inputID1, 
    		@RequestParam(value = "inputID2", required = true) String inputID2,
    		@RequestParam(value = "outputID", required = true) String outputID) {
    	designSpaceService.andDesignSpaces(inputID1, inputID2, outputID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace/insert", method = RequestMethod.POST)
    public ResponseEntity<String> insertDesignSpace(@RequestParam(value = "inputID1", required = true) String inputID1,
    		@RequestParam(value = "inputID2", required = true) String inputID2,
    		@RequestParam(value = "nodeID", required = true) String nodeID,
    		@RequestParam(value = "outputID", required = true) String outputID) {
    	designSpaceService.insertDesignSpace(inputID1, inputID2, nodeID, outputID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT); 
    }

}
