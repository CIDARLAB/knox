package knox.spring.data.neo4j;

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

    @RequestMapping("/findDesignSpace")
    public Map<String, Object> findDesignSpace(@RequestParam(value = "targetID", required = false) String targetID) {
        return designSpaceService.findDesignSpace(targetID == null ? "test1" : targetID);
    }
    
    @RequestMapping("/deleteDesignSpace")
    public Map<String, Object> deleteDesignSpace(@RequestParam(value = "targetID", required = false) String targetID) {
        return designSpaceService.deleteDesignSpace(targetID == null ? "test1" : targetID);
    }
    
    @RequestMapping("/copyDesignSpace")
    public Map<String, Object> copyDesignSpace(@RequestParam(value = "inputID", required = false) String inputID, 
    		@RequestParam(value = "outputID", required = false) String outputID) {
        return designSpaceService.copyDesignSpace(inputID == null ? "test1" : inputID, outputID == null ? "test3" : outputID);
    }
    
    @RequestMapping("/joinDesignSpaces")
    public Map<String, Object> joinDesignSpaces(@RequestParam(value = "inputID1", required = false) String inputID1, 
    		@RequestParam(value = "inputID2", required = false) String inputID2,
    		@RequestParam(value = "outputID", required = false) String outputID) {
        return designSpaceService.joinDesignSpaces(inputID1 == null ? "test1" : inputID1, inputID2 == null ? "test2" : inputID2, 
        		outputID == null ? "test3" : outputID);
    }
    
    @RequestMapping("/orDesignSpaces")
    public Map<String, Object> orDesignSpaces(@RequestParam(value = "inputID1", required = false) String inputID1, 
    		@RequestParam(value = "inputID2", required = false) String inputID2,
    		@RequestParam(value = "outputID", required = false) String outputID) {
        return designSpaceService.orDesignSpaces(inputID1 == null ? "test1" : inputID1, inputID2 == null ? "test2" : inputID2, 
        		outputID == null ? "test3" : outputID);
    }
    
    @RequestMapping("/andDesignSpaces")
    public Map<String, Object> andDesignSpaces(@RequestParam(value = "inputID1", required = false) String inputID1, 
    		@RequestParam(value = "inputID2", required = false) String inputID2,
    		@RequestParam(value = "outputID", required = false) String outputID) {
        return designSpaceService.andDesignSpaces(inputID1 == null ? "test1" : inputID1, inputID2 == null ? "test2" : inputID2, 
        		outputID == null ? "test3" : outputID);
    }

}
