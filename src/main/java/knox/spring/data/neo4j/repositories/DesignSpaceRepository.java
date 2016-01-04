package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Node;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Map;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface DesignSpaceRepository extends GraphRepository<Node> {

	@Query("MATCH (d:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(d:DesignSpace) "
			+ "WHERE d.spaceID =~ ('(?i).*'+{targetID}+'.*') "
			+ "RETURN d.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRole as componentRole, "
			+ "n.nodeID as headID, n.nodeType as headType")
	List<Map<String, Object>> findDesignSpace(@Param("targetID") String targetID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node) "
			+ "WITH target.spaceID as spaceID, target, n "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n "
			+ "WITH collect(spaceID) as spaceIDs "
			+ "RETURN spaceIDs[0] as spaceID")
	List<Map<String, Object>> deleteDesignSpace(@Param("targetID") String targetID);
	
	@Query("MERGE (output:DesignSpace {spaceID: {outputID}}) "
			+ "WITH output "
			+ "MATCH (input:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(input:DesignSpace) "
			+ "WHERE input.spaceID = {inputID} "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "		
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "RETURN output.spaceID AS spaceID "
			+ "LIMIT 1")
	List<Map<String, Object>> copyDesignSpace(@Param("inputID") String inputID, @Param("outputID") String outputID);
    
    @Query("MERGE (output:DesignSpace {spaceID: {outputID}}) "
			+ "WITH output "
			+ "MATCH (inputs:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(inputs:DesignSpace) "
			+ "WHERE inputs.spaceID = {inputID1} OR inputs.spaceID = {inputID2} "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND inputs.spaceID = {inputID1} OR m.nodeType = 'start' AND inputs.spaceID = {inputID2}) AND has(e.componentID) AND has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND inputs.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (m.nodeType = 'start' AND inputs.spaceID = {inputID1} OR m.nodeType = 'accept' AND inputs.spaceID = {inputID2}) AND has(e.componentID) AND has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND inputs.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND inputs.spaceID = {inputID1} OR m.nodeType = 'start' AND inputs.spaceID = {inputID2}) AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' AND inputs.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (inputs.spaceID = {inputID1} AND m.nodeType = 'start' OR inputs.spaceID = {inputID2} AND m.nodeType = 'accept') AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' AND inputs.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND inputs.spaceID = {inputID1} OR m.nodeType = 'start' AND inputs.spaceID = {inputID2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND inputs.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (m.nodeType = 'start' AND inputs.spaceID = {inputID1} OR m.nodeType = 'accept' AND inputs.spaceID = {inputID2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND inputs.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND inputs.spaceID = {inputID1} OR m.nodeType = 'start' AND inputs.spaceID = {inputID2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' AND inputs.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (inputs.spaceID = {inputID1} AND m.nodeType = 'start' OR inputs.spaceID = {inputID2} AND m.nodeType = 'accept') AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' AND inputs.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "WITH output "
			+ "LIMIT 1 "
			+ "MATCH (input1:DesignSpace {spaceID: {inputID1}})-[:CONTAINS]->(m:Node {nodeType: 'accept'}), (input2:DesignSpace {spaceID: {inputID2}})-[:CONTAINS]->(n:Node {nodeType: 'start'}) "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output) "
			+ "RETURN output.spaceID AS spaceID "
			+ "LIMIT 1")
    List<Map<String, Object>> joinDesignSpaces(@Param("inputID1") String inputID1, @Param("inputID2") String inputID2, @Param("outputID") String outputID);
      
    @Query("MERGE (output:DesignSpace {spaceID: {outputID}}) "
			+ "WITH output "
			+ "MATCH (inputs:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(inputs:DesignSpace) "
			+ "WHERE inputs.spaceID = {inputID1} OR inputs.spaceID = {inputID2} "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'start') AND has(e.componentID) AND has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN m.nodeType = 'accept' AND has(e.componentID) AND has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'start') AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN m.nodeType = 'accept' AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'start') AND NOT has(e.componentID) AND NOT has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN m.nodeType = 'accept' AND NOT has(e.componentID) AND NOT has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'start') AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN m.nodeType = 'accept' AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(output)) "
			+ "WITH output "
			+ "LIMIT 1 "
			+ "MATCH (input1:DesignSpace {spaceID: {inputID1}})-[:CONTAINS]->(n1:Node {nodeType: 'start'}), (input2:DesignSpace {spaceID: {inputID2}})-[:CONTAINS]->(n2:Node {nodeType: 'start'}) "
			+ "WITH output, input1, input2, n1, n2 "
			+ "LIMIT 1 "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(n1) + output.spaceID + ID(n2), nodeType: 'start'})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n1)})<-[:CONTAINS]-(output) "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: output.spaceID + ID(n1) + output.spaceID + ID(n2), nodeType: 'start'})-[:PRECEDES]->(:Node {nodeID: output.spaceID + ID(n2)})<-[:CONTAINS]-(output) "
			+ "RETURN output.spaceID AS spaceID")
    List<Map<String, Object>> orDesignSpaces(@Param("inputID1") String inputID1, @Param("inputID2") String inputID2, @Param("outputID") String outputID);
    
//    @Query("MATCH (output:DesignSpace {spaceID: {outputID}) "
//    		+ "RETURN output.spaceID as spaceID")
//    List<Map<String, Object>> andDesignSpaces(@Param("inputID1") String inputID1, @Param("inputID2") String inputID2, @Param("outputID") String outputID);
    
}

