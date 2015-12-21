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
    public Map<String, Object> findDesignSpace(@RequestParam(value = "id", required = false) String id) {
        return designSpaceService.findDesignSpace(id == null ? "test1" : id);
    }
    
    @RequestMapping("/joinDesignSpaces")
    public Map<String, Object> joinDesignSpaces(@RequestParam(value = "id1", required = false) String id1, @RequestParam(value = "id2", required = false) String id2,
    		@RequestParam(value = "id3", required = false) String id3) {
        return designSpaceService.joinDesignSpaces(id1 == null ? "test1" : id1, id2 == null ? "test2" : id2, id3 == null ? "test3" : id3);
    }

}
