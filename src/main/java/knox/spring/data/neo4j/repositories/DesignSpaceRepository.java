package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.DesignSpace;

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
	@Query("MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}) "
			+ "CREATE (tail)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(head)")
	void createComponentEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID, 
			@Param("componentIDs") ArrayList<String> componentIDs, @Param("componentRoles") ArrayList<String> componentRoles);

	@Query("CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: 0, mergeIndex: 0})-[:ARCHIVES]->(b:Branch {branchID: {outputSpaceID}, idIndex: 0}) "
			+ "CREATE (output)-[:SELECTS]->(b)")
	void createDesignSpace(@Param("outputSpaceID") String outputSpaceID);

	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "OPTIONAL MATCH (target)-[:CONTAINS]->(n:Node) "
			+ "OPTIONAL MATCH (target)-[:ARCHIVES]->(b:Branch)-[:CONTAINS]->(c:Commit)-[:CONTAINS]->(s:Snapshot) "
			+ "OPTIONAL MATCH (s)-[:CONTAINS]->(sn:Node) "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n "
			+ "DETACH DELETE b "
			+ "DETACH DELETE c "
			+ "DETACH DELETE s "
			+ "DETACH DELETE sn")
	void deleteDesignSpace(@Param("targetSpaceID") String targetSpaceID);

	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "REMOVE n.copyIndex")
	void deleteNodeCopyIndices(@Param("targetSpaceID") String targetSpaceID);

	DesignSpace findBySpaceID(@Param("spaceID") String spaceID);

	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "RETURN ID(target) as graphID")
	Set<Integer> getGraphID(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRoles as componentRoles, "
			+ "n.nodeID as headID, n.nodeType as headType")
	List<Map<String, Object>> mapDesignSpace(@Param("targetSpaceID") String targetSpaceID);

	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "SET target.spaceID = {outputSpaceID}")
	void renameDesignSpace(@Param("targetSpaceID") String targetSpaceID, @Param("outputSpaceID") String outputSpaceID);

	@Query("MATCH (input:DesignSpace {spaceID: {inputSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "WITH input, collect(n) as nodes "
			+ "MERGE (output:DesignSpace {spaceID: {outputSpaceID}}) "
			+ "ON CREATE SET output.idIndex = size(nodes), output.mergeIndex = input.mergeIndex "
			+ "ON MATCH SET output.idIndex = output.idIndex + size(nodes) "
			+ "FOREACH(ignoreMe IN CASE WHEN output.mergeIndex < input.mergeIndex THEN [1] ELSE [] END | "
			+ "SET output.mergeIndex = input.mergeIndex) "
			+ "WITH input, nodes, output "
			+ "UNWIND range(0, size(nodes) - 1) as nodeIndex "
			+ "WITH input, nodeIndex, nodes[nodeIndex] as n, output "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyIndex: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyIndex: ID(n), nodeType: n.nodeType})) "
			+ "WITH input, n as m, output "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(input) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(output))")
	void unionDesignSpace(@Param("inputSpaceID") String inputSpaceID, @Param("outputSpaceID") String outputSpaceID);
}
