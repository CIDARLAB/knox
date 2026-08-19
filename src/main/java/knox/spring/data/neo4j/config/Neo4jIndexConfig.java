package knox.spring.data.neo4j.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Neo4jIndexConfig {

    private final Driver driver;

    public Neo4jIndexConfig(Driver driver) {
        this.driver = driver;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        try (Session session = driver.session()) {

            // CONSTRAINTS

            // spaceID Unique Constraint
            session.run(
                "CREATE CONSTRAINT space_id_unique IF NOT EXISTS " +
                "FOR (d:DesignSpace) " +
                "REQUIRE d.spaceID IS UNIQUE"
            );

            // groupID Unique Constraint
            session.run(
                "CREATE CONSTRAINT group_id_unique IF NOT EXISTS " +
                "FOR (d:DesignGroup) " +
                "REQUIRE d.groupID IS UNIQUE"
            );

            // experimentName Unique Constraint
            session.run(
                "CREATE CONSTRAINT experiment_name_unique IF NOT EXISTS " + 
                "FOR (e:Experiment) "  + 
                "REQUIRE e.experimentName IS UNIQUE"
            );
            
            // jobID Unique Constraint
            session.run(
                "CREATE CONSTRAINT job_id_unique IF NOT EXISTS " +
                "FOR (j:Job) " +
                "REQUIRE j.jobID IS UNIQUE"
            );

            // partLibraryName Unqiue Constraint
            session.run(
                "CREATE CONSTRAINT part_library_name_unique IF NOT EXISTS " +
                "FOR (p:PartLibrary) " +
                "REQUIRE p.partLibraryName IS UNIQUE"
            );

            // evaluationName Unique Constraint
            session.run(
                "CREATE CONSTRAINT evaluation_name_unique IF NOT EXISTS " +
                "FOR (e:RuleEvaluation) " +
                "REQUIRE e.evaluationName IS UNIQUE"
            );
        
        }
    }
}
