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
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node) "
			+ "WITH target.spaceID as spaceID, target, n "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n "
			+ "RETURN collect(spaceID)[0] as spaceID")
	List<Map<String, Object>> deleteDesignSpace(@Param("targetID") String targetID);
	
	@Query("MATCH (input:DesignSpace {spaceID: {inputID}})-[:CONTAINS]->(n:Node) "
			+ "WITH input, collect(n) as nodes "
			+ "UNWIND range(0, size(nodes) - 1) as nodeIndex "
			+ "WITH input, nodeIndex, nodes[nodeIndex] as n, size(nodes) as nodeCount "
			+ "MERGE (output:DesignSpace {spaceID: {outputID}}) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + ({idIndex} + nodeIndex), copyID: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + ({idIndex} + nodeIndex), copyID: ID(n), nodeType: n.nodeType})) "
			+ "WITH input, n as m, nodeCount, output "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(input) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentID) AND NOT has(e.componentRole) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentID) AND has(e.componentRole) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES {componentID: e.componentID, componentRole: e.componentRole}]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(output)) "
			+ "RETURN collect(output.spaceID)[0] AS spaceID, nodeCount")
	List<Map<String, Object>> copyDesignSpace(@Param("inputID") String inputID, @Param("outputID") String outputID, @Param("idIndex") int idIndex);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node {nodeType: {nodeType}}) "
			+ "RETURN n.nodeID as nodeID")
	List<Map<String, Object>> findNodesByType(@Param("targetID") String targetID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node {nodeID: {nodeID}}) "
			+ "REMOVE n.nodeType "
			+ "RETURN n.nodeID as nodeID")
	List<Map<String, Object>> removeNodeType(@Param("targetID") String targetID, @Param("nodeID") String nodeID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(m:Node {nodeID: {nodeID}}) "
			+ "OPTIONAL MATCH (m)-[e:Precedes]->(n:Node) "
			+ "DELETE e "
			+ "RETURN m.nodeID as tailID, n.nodeID as headID")
	List<Map<String, Object>> deleteOutgoingEdges(@Param("targetID") String targetID, @Param("nodeID") String nodeID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}}) "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(n:Node {nodeID: {nodeID}, nodeType: {nodeType}}) "
			+ "RETURN n.nodeID as nodeID")
	List<Map<String, Object>> createTypedNode(@Param("targetID") String targetID, @Param("nodeID") String nodeID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (tail:Node {nodeID: {tailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(head:Node {nodeID: {headID}}) "
			+ "CREATE UNIQUE (tail)-[:PRECEDES]->(head) "
			+ "RETURN tail.nodeID as tailID, head.nodeID as headID")
	List<Map<String, Object>> connectNodes(@Param("targetID") String targetID, @Param("tailID") String tailID, @Param("headID") String headID);
    
}

