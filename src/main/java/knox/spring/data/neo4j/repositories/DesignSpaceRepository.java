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

	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetID} "
			+ "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRole as componentRole, "
			+ "n.nodeID as headID, n.nodeType as headType")
	List<Map<String, Object>> mapDesignSpace(@Param("targetID") String targetID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node {nodeType: {nodeType}}) "
			+ "RETURN n.nodeID as nodeID, ID(n) as id")
	List<Map<String, Object>> getNodesByType(@Param("targetID") String targetID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node) "
			+ "WITH target.spaceID as spaceID, target, n "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n "
			+ "WITH collect(spaceID) as spaceIDs "
			+ "RETURN spaceIDs[0] as spaceID")
	List<Map<String, Object>> deleteDesignSpace(@Param("targetID") String targetID);
	
	@Query("MERGE (copy:DesignSpace {spaceID: {copyID}}) "
			+ "WITH copy "
			+ "MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetID} "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: copy.spaceID + ID(n)})<-[:CONTAINS]-(copy)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: copy.spaceID + ID(n)})<-[:CONTAINS]-(copy)) "		
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: copy.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(copy)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND has(e.componentID) AND has(e.componentRole) AND has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {nodeID: copy.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(copy)) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: copy.spaceID + ID(n)})<-[:CONTAINS]-(copy)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: copy.spaceID + ID(n)})<-[:CONTAINS]-(copy)) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m)})-[:PRECEDES]->(:Node {nodeID: copy.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(copy)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(m.nodeType) AND NOT has(e.componentID) AND NOT has(e.componentRole) AND has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (copy)-[:CONTAINS]->(:Node {nodeID: copy.spaceID + ID(m), nodeType: m.nodeType})-[:PRECEDES]->(:Node {nodeID: copy.spaceID + ID(n), nodeType: n.nodeType})<-[:CONTAINS]-(copy)) "
			+ "RETURN copy.spaceID AS spaceID "
			+ "LIMIT 1")
	List<Map<String, Object>> copyDesignSpace(@Param("targetID") String targetID, @Param("copyID") String copyID);
	
	@Query("MATCH (n:Node {nodeID: {nodeID}}) "
			+ "REMOVE n.nodeType "
			+ "RETURN n.nodeID as nodeID")
	List<Map<String, Object>> removeNodeType(@Param("nodeID") String nodeID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}}) "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(n:Node {nodeID: {nodeID}, nodeType: {nodeType}}) "
			+ "RETURN n.nodeID as nodeID, ID(n) as id")
	List<Map<String, Object>> createTypedNode(@Param("targetID") String targetID, @Param("nodeID") String nodeID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (m:Node {nodeID: {tailID}}), (n:Node {nodeID: {headID}}) "
			+ "CREATE UNIQUE (m)-[:PRECEDES]->(n) "
			+ "RETURN m.nodeID as tailID, n.nodeID as headID")
	List<Map<String, Object>> connectNodes(@Param("tailID") String tailID, @Param("headID") String headID);
    
}

