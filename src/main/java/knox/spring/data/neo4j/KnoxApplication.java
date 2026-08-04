package knox.spring.data.neo4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author Nicholas Roehner
 */
@SpringBootApplication
@EnableAsync
@EntityScan("knox.spring.data.neo4j.domain")
public class KnoxApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnoxApplication.class, args);
    }
}
