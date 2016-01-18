package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface DesignSpaceRepository extends GraphRepository<DesignSpace> {
	
	DesignSpace findBySpaceID(@Param("spaceID") String spaceID);

	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetID} "
			+ "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRoles as componentRoles, "
			+ "n.nodeID as headID, n.nodeType as headType")
	List<Map<String, Object>> mapDesignSpace(@Param("targetID") String targetID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node) "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n")
	void deleteDesignSpace(@Param("targetID") String targetID);
	
	@Query("MATCH (input:DesignSpace {spaceID: {inputID}})-[:CONTAINS]->(n:Node) "
			+ "WITH input, collect(n) as nodes "
			+ "UNWIND range(0, size(nodes) - 1) as nodeIndex "
			+ "WITH input, nodeIndex, nodes[nodeIndex] as n "
			+ "MERGE (output:DesignSpace {spaceID: {outputID}}) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + ({idIndex} + nodeIndex), copyID: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + ({idIndex} + nodeIndex), copyID: ID(n), nodeType: n.nodeType})) "
			+ "WITH input, n as m, output "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(input) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(output))")
	void copyDesignSpace(@Param("inputID") String inputID, @Param("outputID") String outputID, @Param("idIndex") int idIndex);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetID1}})-[:CONTAINS]->(n1:Node {nodeID: {nodeID}}), (:DesignSpace {spaceID: {targetID2}})-[:CONTAINS]->(n2:Node {copyID: ID(n1)}) "
			+ "RETURN n2")
	Set<Node> findNodeCopy(@Param("targetID1") String targetID1, @Param("nodeID") String nodeID, @Param("targetID2") String targetID2);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node) "
			+ "WITH n, n.copyID as copyID "
			+ "REMOVE n.copyID "
			+ "RETURN copyID")
	List<Map<String, Object>> removeCopyIDs(@Param("targetID") String targetID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node {nodeType: {nodeType}}) "
			+ "RETURN n")
	Set<Node> findNodesByType(@Param("targetID") String targetID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetID}}) "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(n:Node {nodeID: {nodeID}, nodeType: {nodeType}})")
	void createTypedNode(@Param("targetID") String targetID, @Param("nodeID") String nodeID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(n:Node {nodeID: {nodeID}}) "
			+ "WITH n, n.nodeType as nodeType "
			+ "REMOVE n.nodeType "
			+ "RETURN nodeType")
	List<Map<String, Object>> removeNodeType(@Param("targetID") String targetID, @Param("nodeID") String nodeID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {tailID}})-[e:PRECEDES]->(n:Node {nodeID: {headID}})<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetID} "
			+ "RETURN e")
	Set<Edge> findEdge(@Param("targetID") String targetID, @Param("tailID") String tailID, @Param("headID") String headID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {nodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetID} "
			+ "RETURN e")
	Set<Edge> findOutgoingEdges(@Param("targetID") String targetID, @Param("nodeID") String nodeID);
	
	@Query("MATCH (tail:Node {nodeID: {tailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(head:Node {nodeID: {headID}}) "
			+ "CREATE UNIQUE (tail)-[e:PRECEDES]->(head)")
	void createEdge(@Param("targetID") String targetID, @Param("tailID") String tailID, @Param("headID") String headID);
	
	@Query("MATCH (tail:Node {nodeID: {tailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetID}})-[:CONTAINS]->(head:Node {nodeID: {headID}}) "
			+ "CREATE UNIQUE (tail)-[e:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(head)")
	void createComponentEdge(@Param("targetID") String targetID, @Param("tailID") String tailID, @Param("headID") String headID, 
			@Param("componentIDs") ArrayList<String> componentIDs, @Param("componentRoles") ArrayList<String> componentRoles);  
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {nodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetID} "
			+ "DELETE e")
	void removeOutgoingEdges(@Param("targetID") String targetID, @Param("nodeID") String nodeID);
	
}
