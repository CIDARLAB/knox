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
	
//	Node findByTitle(@Param("title") String title);
//	
//	@Query("MATCH (m:Movie) WHERE m.title =~ ('(?i).*'+{title}+'.*') RETURN m")
//    Collection<Node> findByTitleContaining(@Param("title") String title);

    @Query("MATCH (d:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(d:DesignSpace) "
    		+ "WHERE d.spaceID =~ ('(?i).*'+{targetId}+'.*') "
    		+ "RETURN d.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRole as componentRole, "
    		+ "n.nodeID as headID, n.nodeType as headType")
    List<Map<String,Object>> findDesignSpace(@Param("targetId") String targetId);
    
    @Query("MERGE (d3:DesignSpace {spaceID: {targetId}}) "
			+ "WITH d3 "
			+ "MATCH (d12:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(d12:DesignSpace) "
			+ "WHERE d12.spaceID = {inputID1} OR d12.spaceID = {inputID2} "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.spaceID = {inputID1} OR m.nodeType = 'start' AND d12.spaceID = {inputID2}) AND has(e.componentID) AND has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: d3.spaceID + ID(n)})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (m.nodeType = 'start' AND d12.spaceID = {inputID1} OR m.nodeType = 'accept' AND d12.spaceID = {inputID2}) AND has(e.componentID) AND has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: d3.spaceID + ID(n)})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.spaceID = {inputID1} OR m.nodeType = 'start' AND d12.spaceID = {inputID2}) AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' AND d12.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: d3.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (d12.spaceID = {inputID1} AND m.nodeType = 'start' OR d12.spaceID = {inputID2} AND m.nodeType = 'accept') AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' AND d12.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: d3.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.spaceID = {inputID1} OR m.nodeType = 'start' AND d12.spaceID = {inputID2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: d3.spaceID + ID(n)})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (m.nodeType = 'start' AND d12.spaceID = {inputID1} OR m.nodeType = 'accept' AND d12.spaceID = {inputID2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.spaceID = {inputID1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: d3.spaceID + ID(n)})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.spaceID = {inputID1} OR m.nodeType = 'start' AND d12.spaceID = {inputID2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' AND d12.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: d3.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (d12.spaceID = {inputID1} AND m.nodeType = 'start' OR d12.spaceID = {inputID2} AND m.nodeType = 'accept') AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' AND d12.spaceID = {inputID2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: d3.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "WITH d3 "
			+ "LIMIT 1 "
			+ "MATCH (d1:DesignSpace {spaceID: {inputID1}})-[:CONTAINS]->(m:Node {nodeType: 'accept'}), (d2:DesignSpace {spaceID: {inputID2}})-[:CONTAINS]->(n:Node {nodeType: 'start'}) "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {nodeID: d3.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: d3.spaceID + ID(n)})<-[:CONTAINS]-(d3) "
			+ "RETURN d3.spaceID AS spaceID")
    List<Map<String,Object>> joinDesignSpaces(@Param("inputID1") String inputID1, @Param("inputID2") String inputID2, @Param("targetId") String targetId);
    
//    List<Map<String,Object>> orDesignSpaces(@Param("inputID1") String inputID1, @Param("inputID2") String inputID2, @Param("targetId") String targetId);
    
}

