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
    
    @RequestMapping(value = "/branch", method = RequestMethod.POST)
    public ResponseEntity<String> createBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "outputBranchID", required = true) String outputBranchID) {
    	designSpaceService.createBranch(targetSpaceID, outputBranchID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/checkout", method = RequestMethod.PUT)
    public ResponseEntity<String> checkoutBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID,
    		@RequestParam(value = "targetBranchID", required = true) String targetBranchID) {
    	designSpaceService.checkoutBranch(targetSpaceID, targetBranchID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/branch/commitTo", method = RequestMethod.POST)
    public ResponseEntity<String> commitToBranch(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
    	designSpaceService.commitToBranch(targetSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
    	designSpaceService.deleteDesignSpace(targetSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace", method = RequestMethod.POST)
    public ResponseEntity<String> createDesignSpace(@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
    	designSpaceService.createDesignSpace(outputSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace/and", method = RequestMethod.POST)
    public ResponseEntity<String> andDesignSpaces(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1, 
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
    	designSpaceService.andDesignSpaces(inputSpaceID1, inputSpaceID2, outputSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/designSpace/graph/d3", method = RequestMethod.GET)
    public Map<String, Object> d3GraphDesignSpace(@RequestParam(value = "targetSpaceID", required = true) String targetSpaceID) {
        return designSpaceService.d3GraphDesignSpace(targetSpaceID);
    }
    
    @RequestMapping(value = "/designSpace/insert", method = RequestMethod.POST)
    public ResponseEntity<String> insertDesignSpace(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1,
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "targetNodeID", required = true) String targetNodeID,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
    	designSpaceService.insertDesignSpace(inputSpaceID1, inputSpaceID2, targetNodeID, outputSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT); 
    }
    
    @RequestMapping(value = "/designSpace/join", method = RequestMethod.POST)
    public ResponseEntity<String> joinDesignSpaces(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1, 
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
    	designSpaceService.joinDesignSpaces(inputSpaceID1, inputSpaceID2, outputSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }
    
    @RequestMapping(value = "/designSpace/or", method = RequestMethod.POST)
    public ResponseEntity<String> orDesignSpaces(@RequestParam(value = "inputSpaceID1", required = true) String inputSpaceID1, 
    		@RequestParam(value = "inputSpaceID2", required = true) String inputSpaceID2,
    		@RequestParam(value = "outputSpaceID", required = true) String outputSpaceID) {
    	designSpaceService.orDesignSpaces(inputSpaceID1, inputSpaceID2, outputSpaceID);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

}
