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
	
	@Query("MATCH (s:Snapshot)<-[:CONTAINS]-(:Commit)<-[:LATEST]-(b:Branch {branchID: {targetBranchID}})<-[:CONTAINS]-(target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "OPTIONAL MATCH (target)-[h:SELECTS]->(:Branch) "
			+ "FOREACH(ignoreMe IN CASE WHEN h IS NOT NULL THEN [1] ELSE [] END | "
			+ "DELETE h) "
			+ "CREATE UNIQUE (target)-[:SELECTS]->(b) "
			+ "SET target.idIndex = s.idIndex "
			+ "WITH target, s "
			+ "MATCH (s)-[:CONTAINS]->(n:Node) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyID: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyID: ID(n), nodeType: n.nodeType})) "
			+ "WITH target, s, n as m "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(s) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(target)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(target))")
	void checkoutBranch(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);

	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
			+ "OPTIONAL MATCH (hb)-[l:LATEST]->(d:Commit) "
			+ "FOREACH(ignoreMe IN CASE WHEN l IS NOT NULL THEN [1] ELSE [] END | "
			+ "DELETE l) "
			+ "CREATE UNIQUE (hb)-[:LATEST]->(c:Commit {commitID: 'c'+ hb.idIndex})<-[:CONTAINS]-(hb), (c)-[:CONTAINS]->(s:Snapshot {spaceID: target.spaceID, idIndex: target.idIndex}) "
			+ "FOREACH(ignoreMe IN CASE WHEN d IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (c)-[:SUCCEEDS]->(d)) "
			+ "SET hb.idIndex = hb.idIndex + 1 "
			+ "WITH target, s "
			+ "MATCH (target)-[:CONTAINS]->(n:Node) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (s)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyID: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (s)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyID: ID(n), nodeType: n.nodeType})) "
			+ "WITH target, s, n as m "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (s)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(s)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (s)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(s))")
	void commitToBranch(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MERGE (output:DesignSpace {spaceID: {outputSpaceID}}) "
			+ "ON CREATE SET output.idIndex = 0 "
			+ "WITH output "
			+ "MATCH (:DesignSpace {spaceID: {inputSpaceID}})-[:CONTAINS]->(bi:Branch {branchID: {inputBranchID}}) "
			+ "OPTIONAL MATCH (output)-[:SELECTS]->(hb:Branch) "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(bo:Branch {branchID: {outputBranchID}, idIndex: bi.idIndex}) "
			+ "FOREACH(ignoreMe IN CASE WHEN hb IS NULL THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:SELECTS]->(bo)) "
			+ "WITH bi, bo "
			+ "MATCH (bi)-[:LATEST]->(c:Commit)<-[:CONTAINS]-(bi) "
			+ "CREATE UNIQUE (bo)-[:LATEST]->(:Commit {commitID: c.commitID})<-[:CONTAINS]-(bo) "
			+ "WITH bi, bo "
			+ "MATCH (bi)-[:CONTAINS]->(ci:Commit)-[:CONTAINS]->(si:Snapshot) "
			+ "OPTIONAL MATCH (ci)-[:SUCCEEDS]->(d:Commit)<-[:CONTAINS]-(bi) "
			+ "CREATE UNIQUE (bo)-[:CONTAINS]->(co:Commit {commitID: ci.commitID})-[:CONTAINS]->(so:Snapshot {spaceID: si.spaceID, idIndex: si.idIndex}) "
			+ "FOREACH(ignoreMe IN CASE WHEN d IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (co)-[:SUCCEEDS]->(:Commit {commitID: d.commitID})<-[:CONTAINS]-(bo)) "
			+ "WITH si, so "
			+ "MATCH (si)-[:CONTAINS]->(n:Node) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyID: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyID: ID(n), nodeType: n.nodeType})) "
			+ "WITH si, so, n as m "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(si) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(so)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(so))")
	void copyBranch(@Param("inputSpaceID") String inputSpaceID, @Param("inputBranchID") String inputBranchID, @Param("outputSpaceID") String outputSpaceID, @Param("outputBranchID") String outputBranchID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRoles as componentRoles, "
			+ "n.nodeID as headID, n.nodeType as headType")
	List<Map<String, Object>> mapDesignSpace(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n")
	void deleteDesignSpace(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MERGE (output:DesignSpace {spaceID: {outputSpaceID}}) "
			+ "ON CREATE SET output.idIndex = 0 "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Branch {branchID: 'master'})<-[:SELECTS]-(output)")
	void createDesignSpace(@Param("outputSpaceID") String outputSpaceID);
	
	@Query("MATCH (target {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(b:Branch {branchID: {outputBranchID}, idIndex: hb.idIndex})-[:CONTAINS]->(c:Commit)<-[:LATEST]-(hb), (b)-[:LATEST]->(c)")
	void createBranch(@Param("targetSpaceID") String targetSpaceID, @Param("outputBranchID") String outputBranchID);
	
	@Query("MATCH (input:DesignSpace {spaceID: {inputSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "WITH input, collect(n) as nodes "
			+ "MERGE (output:DesignSpace {spaceID: {outputSpaceID}}) "
			+ "ON CREATE SET output.idIndex = size(nodes) "
			+ "ON MATCH SET output.idIndex = output.idIndex + size(nodes) "
			+ "WITH input, nodes, output "
			+ "UNWIND range(0, size(nodes) - 1) as nodeIndex "
			+ "WITH input, nodeIndex, nodes[nodeIndex] as n, output "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyID: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyID: ID(n), nodeType: n.nodeType})) "
			+ "WITH input, n as m, output "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(input) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyID: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyID: ID(n)})<-[:CONTAINS]-(output))")
	void copyDesignSpace(@Param("inputSpaceID") String inputSpaceID, @Param("outputSpaceID") String outputSpaceID);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID1}})-[:CONTAINS]->(n1:Node {nodeID: {targetNodeID}}), (:DesignSpace {spaceID: {targetSpaceID2}})-[:CONTAINS]->(n2:Node {copyID: ID(n1)}) "
			+ "RETURN n2")
	Set<Node> findNodeCopy(@Param("targetSpaceID1") String targetSpaceID1, @Param("targetNodeID") String targetNodeID, @Param("targetSpaceID2") String targetSpaceID2);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "REMOVE n.copyID")
	void deleteCopyIDs(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(:Branch {branchID: {targetBranchID}})-[:CONTAINS]->(:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node) "
			+ "REMOVE n.copyID")
	void deleteBranchCopyIDs(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(:Branch)-[:LATEST]->(:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node) "
			+ "REMOVE n.copyID")
	void deleteLatestHeadCopyIDs(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeType: {nodeType}}) "
			+ "RETURN n")
	Set<Node> getNodesByType(@Param("targetSpaceID") String targetSpaceID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch) "
			+ "RETURN b.branchID as branchID")
	List<Map<String, Object>> getBranchIDs(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}, nodeType: {nodeType}})")
	void createTypedNode(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}}) "
			+ "REMOVE n.nodeType")
	void deleteNodeType(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {targetTailID}})-[e:PRECEDES]->(n:Node {nodeID: {targetHeadID}})<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN e")
	Set<Edge> findEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN e")
	Set<Edge> getOutgoingEdges(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID);
	
	@Query("MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}) "
			+ "CREATE UNIQUE (tail)-[e:PRECEDES]->(head)")
	void createEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID);
	
	@Query("MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}) "
			+ "CREATE UNIQUE (tail)-[e:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(head)")
	void createComponentEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID, 
			@Param("componentIDs") ArrayList<String> componentIDs, @Param("componentRoles") ArrayList<String> componentRoles);  
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "DELETE e")
	void deleteOutgoingEdges(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID);
	
}
