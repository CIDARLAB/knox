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
            // DesignSpace indexes
            session.run("CREATE INDEX design_space_id IF NOT EXISTS FOR (d:DesignSpace) ON (d.spaceID)");
            session.run("CREATE INDEX design_space_group IF NOT EXISTS FOR (d:DesignSpace) ON (d.groupID)");

            // RuleEvaluation indexes
            session.run("CREATE INDEX rule_eval_name IF NOT EXISTS FOR (d:RuleEvaluation) ON (d.evaluationName)");
            
            // Node indexes
            //session.run("CREATE INDEX node_id IF NOT EXISTS FOR (n:Node) ON (n.nodeID)");
            
            // Branch indexes
            //session.run("CREATE INDEX branch_id IF NOT EXISTS FOR (b:Branch) ON (b.branchID)");
            
            // Composite index for group queries
            //session.run("CREATE INDEX design_space_group_round IF NOT EXISTS FOR (d:DesignSpace) ON (d.groupID, d.round)");
        }
    }
}
