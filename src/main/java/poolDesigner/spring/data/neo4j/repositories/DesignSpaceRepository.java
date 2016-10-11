package poolDesigner.spring.data.neo4j.repositories;

import poolDesigner.spring.data.neo4j.domain.DesignSpace;

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
@RepositoryRestResource(collectionResourceRel = "poolDesigner", path = "poolDesigner")
public interface DesignSpaceRepository extends GraphRepository<DesignSpace> {
//	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(tail:Node {nodeID: 'n' + (target.idIndex - 1)}) "
//			+ "CREATE (tail)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(:Node {nodeID: 'n' + target.idIndex})<-[:CONTAINS]-(target) "
//			+ "SET target.idIndex = target.idIndex + 1")
//	void createComponentEdge(@Param("targetSpaceID") String targetSpaceID, @Param("componentIDs") ArrayList<String> componentIDs, @Param("componentRoles") ArrayList<String> componentRoles);
	
//	@Query("CREATE (:DesignSpace {spaceID: {outputSpaceID}, idIndex: 1})-[:CONTAINS]->(:Node {nodeID: 'n0', nodeType: 'start'})")
//	void createDesignSpace(@Param("outputSpaceID") String outputSpaceID);
	
//	@Query("CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: 2}) "
//			+ "CREATE (output)-[:CONTAINS]->(m:Node {nodeID: 'n0', nodeType: 'start'}) "
//			+ "CREATE (output)-[:CONTAINS]->(n:Node {nodeID: 'n1', nodeType: 'accept'}) "
//			+ "CREATE (m)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(n)")
//	void createDesignSpace(@Param("outputSpaceID") String outputSpaceID, @Param("componentIDs") ArrayList<String> componentIDs, @Param("componentRoles") ArrayList<String> componentRoles);

	@Query("CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: size({allCompIDs}) + 1}) "
			+ "WITH output "
			+ "UNWIND range(0, size({allCompIDs})) AS i "
			+ "CREATE (output)-[:CONTAINS]->(n:Node {nodeID: 'n' + i}) "
			+ "WITH COLLECT(n) AS ns "
			+ "UNWIND range(0, size(ns) - 1) AS i "
			+ "WITH ns[0] as nStart, ns[size(ns) - 1] as nAccept, ns[i] AS n1, ns[i + 1] AS n2, {allCompIDs}[i] AS compIDs, {allCompRoles}[i] AS compRoles "
			+ "MERGE (n1)-[:PRECEDES {componentIDs: compIDs, componentRoles: compRoles}]->(n2) "
			+ "SET nStart.nodeType = 'start' "
			+ "SET nAccept.nodeType = 'accept'")
	void createDesignSpace(@Param("outputSpaceID") String outputSpaceID, 
			@Param("allCompIDs") ArrayList<ArrayList<String>> allCompIDs, 
			@Param("allCompRoles") ArrayList<ArrayList<String>> allCompRoles);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "OPTIONAL MATCH (target)-[:CONTAINS]->(n:Node) "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n ")
	void deleteDesignSpace(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (n) DETACH DELETE n")
	void deleteAll();

	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "REMOVE n.copyIndex")
	void deleteNodeCopyIndices(@Param("targetSpaceID") String targetSpaceID);

	DesignSpace findBySpaceID(@Param("spaceID") String spaceID);

	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "RETURN ID(target) as graphID")
	Set<Integer> getGraphID(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "RETURN n.nodeID")
	Set<String> getNodeIDs(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (d:DesignSpace) "
			+ "RETURN d.spaceID")
	Set<String> getDesignSpaceIDs();
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(n:Node) "
			+ "WHERE count(n) >= size "
			+ "RETURN target.spaceID")
	Set<String> getDesignSpaceIDsBySize(@Param("size") int size);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(n:Node)-[e:PRECEDES]->(m:Node)<-[:CONTAINS]-(target:DesignSpace)"
			+ "WHERE target.spaceID = {targetSpaceID} AND has(e.componentIDs) "
			+ "RETURN e.componentIDs")
	Set<String> getComponentIDs(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(n:Node)-[e:PRECEDES]->(m:Node)<-[:CONTAINS]-(target:DesignSpace)"
			+ "WHERE target.spaceID = {targetSpaceID} AND has(e.componentRoles) "
			+ "RETURN e.componentRoles")
	Set<String> getComponentRoles(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRoles as componentRoles, "
			+ "n.nodeID as headID, n.nodeType as headType")
	List<Map<String, Object>> mapDesignSpace(@Param("targetSpaceID") String targetSpaceID);

	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "SET target.spaceID = {outputSpaceID}")
	void renameDesignSpace(@Param("targetSpaceID") String targetSpaceID, @Param("outputSpaceID") String outputSpaceID);

//	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}}) "
//			+ "SET n.nodeType = {nodeType}")
//	void setNodeType(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID,
//			@Param("nodeType") String nodeType);
	
	@Query("MATCH (input:DesignSpace {spaceID: {inputSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "WITH input, collect(n) as nodes "
			+ "MERGE (output:DesignSpace {spaceID: {outputSpaceID}}) "
			+ "ON CREATE SET output.idIndex = size(nodes) "
			+ "ON MATCH SET output.idIndex = output.idIndex + size(nodes) "
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
