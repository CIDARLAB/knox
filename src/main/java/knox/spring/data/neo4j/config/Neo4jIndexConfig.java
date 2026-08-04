package knox.spring.data.neo4j.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Neo4jIndexConfig {

    @Autowired
    private Driver driver;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        try (Session session = driver.session()) {

            // INDEXES

            // DesignSpace indexes
            //session.run("CREATE INDEX design_space_id IF NOT EXISTS FOR (d:DesignSpace) ON (d.spaceID)");
            //session.run("CREATE INDEX design_space_group IF NOT EXISTS FOR (d:DesignSpace) ON (d.groupID)");

            // RuleEvaluation indexes
            //session.run("CREATE INDEX rule_eval_name IF NOT EXISTS FOR (d:RuleEvaluation) ON (d.evaluationName)");
            
            // Node indexes
            //session.run("CREATE INDEX node_id IF NOT EXISTS FOR (n:Node) ON (n.nodeID)");
            
            // Branch indexes
            //session.run("CREATE INDEX branch_id IF NOT EXISTS FOR (b:Branch) ON (b.branchID)");
            
            // Composite index for group queries
            //session.run("CREATE INDEX design_space_group_round IF NOT EXISTS FOR (d:DesignSpace) ON (d.groupID, d.round)");
        


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
