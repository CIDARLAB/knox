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
    		+ "WHERE d.displayID =~ ('(?i).*'+{id}+'.*') "
    		+ "RETURN d.displayID as graphID, m.displayID as tailID, m.nodeType as tailType, e.componentRole as componentRole, "
    		+ "n.displayID as headID, n.nodeType as headType")
    List<Map<String,Object>> findDesignSpace(@Param("id") String id);
    
    @Query("MERGE (d3:DesignSpace {displayID: {id3}}) "
			+ "WITH d3 "
			+ "MATCH (d12:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(d12:DesignSpace) "
			+ "WHERE d12.displayID = {id1} OR d12.displayID = {id2} "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.displayID = {id1} OR m.nodeType = 'start' AND d12.displayID = {id2}) AND has(e.componentID) AND has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.displayID = {id1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {displayID: n.displayID})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (m.nodeType = 'start' AND d12.displayID = {id1} OR m.nodeType = 'accept' AND d12.displayID = {id2}) AND has(e.componentID) AND has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.displayID = {id1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID, nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {displayID: n.displayID})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.displayID = {id1} OR m.nodeType = 'start' AND d12.displayID = {id2}) AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' AND d12.displayID = {id2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {displayID: n.displayID, nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (d12.displayID = {id1} AND m.nodeType = 'start' OR d12.displayID = {id2} AND m.nodeType = 'accept') AND has(e.componentID) AND has(e.componentRole) AND n.nodeType = 'accept' AND d12.displayID = {id2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID, nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {displayID: n.displayID, nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.displayID = {id1} OR m.nodeType = 'start' AND d12.displayID = {id2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.displayID = {id1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID})-[:PRECEDES]->(:Node {displayID: n.displayID})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (m.nodeType = 'start' AND d12.displayID = {id1} OR m.nodeType = 'accept' AND d12.displayID = {id2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND (NOT has(n.nodeType) OR n.nodeType = 'accept' AND d12.displayID = {id1}) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID, nodeType: m.nodeType})-[:PRECEDES]->(:Node {displayID: n.displayID})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (NOT has(m.nodeType) OR m.nodeType = 'accept' AND d12.displayID = {id1} OR m.nodeType = 'start' AND d12.displayID = {id2}) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' AND d12.displayID = {id2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID})-[:PRECEDES]->(:Node {displayID: n.displayID, nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "FOREACH(ignoreMe IN CASE WHEN (d12.displayID = {id1} AND m.nodeType = 'start' OR d12.displayID = {id2} AND m.nodeType = 'accept') AND NOT has(e.componentID) AND NOT has(e.componentRole) AND n.nodeType = 'accept' AND d12.displayID = {id2} THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID, nodeType: m.nodeType})-[:PRECEDES]->(:Node {displayID: n.displayID, nodeType: n.nodeType})<-[:CONTAINS]-(d3)) "
			+ "WITH d3 "
			+ "LIMIT 1 "
			+ "MATCH (d1:DesignSpace {displayID: {id1}})-[:CONTAINS]->(m:Node {nodeType: 'accept'}), (d2:DesignSpace {displayID: {id2}})-[:CONTAINS]->(n:Node {nodeType: 'start'}) "
			+ "CREATE UNIQUE (d3)-[:CONTAINS]->(:Node {displayID: m.displayID})-[:PRECEDES]->(:Node {displayID: n.displayID})<-[:CONTAINS]-(d3) "
			+ "RETURN d3.displayID AS displayID")
    List<Map<String,Object>> joinDesignSpaces(@Param("id1") String id1, @Param("id2") String id2, @Param("id3") String id3);
    
}

