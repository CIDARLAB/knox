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
	
	@Query("MATCH (s:Snapshot)<-[:CONTAINS]-(c:Commit)<-[:CONTAINS]-(b:Branch {branchID: {targetBranchID}})<-[:CONTAINS]-(target:DesignSpace {spaceID: {targetSpaceID}})-[h:SELECTS]->(:Branch), (b)-[:LATEST]->(c) "
			+ "DELETE h "
			+ "CREATE (target)-[:SELECTS]->(b) "
			+ "SET target.idIndex = s.idIndex "
			+ "WITH target, s "
			+ "MATCH (s)-[:CONTAINS]->(n:Node) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (target)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyIndex: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (target)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyIndex: ID(n), nodeType: n.nodeType})) "
			+ "WITH target, s, n as m "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(s) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(target)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(target))")
	void checkoutBranch(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);

	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
			+ "OPTIONAL MATCH (hb)-[l:LATEST]->(d:Commit)<-[:CONTAINS]-(hb) "
			+ "FOREACH(ignoreMe IN CASE WHEN l IS NOT NULL THEN [1] ELSE [] END | "
			+ "DELETE l) "
			+ "CREATE (hb)-[:LATEST]->(c:Commit {commitID: 'c'+ hb.idIndex})<-[:CONTAINS]-(hb) "
			+ "CREATE (c)-[:CONTAINS]->(s:Snapshot {idIndex: target.idIndex}) "
			+ "FOREACH(ignoreMe IN CASE WHEN d IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE (c)-[:SUCCEEDS]->(d)) "
			+ "SET hb.idIndex = hb.idIndex + 1 "
			+ "WITH target, s "
			+ "MATCH (target)-[:CONTAINS]->(n:Node) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (s)-[:CONTAINS]->(:Node {nodeID: n.nodeID})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (s)-[:CONTAINS]->(:Node {nodeID: n.nodeID, nodeType: n.nodeType})) "
			+ "WITH target, s, n as m "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (s)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(s)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (s)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(s))")
	void commitToBranch(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MERGE (output:DesignSpace {spaceID: {outputSpaceID}}) "
			+ "ON CREATE SET output.idIndex = 0 "
			+ "WITH output "
			+ "MATCH (:DesignSpace {spaceID: {inputSpaceID}})-[:CONTAINS]->(bi:Branch {branchID: {inputBranchID}}) "
			+ "CREATE (output)-[:CONTAINS]->(bo:Branch {branchID: {outputBranchID}, idIndex: bi.idIndex}) "
			
			+ "WITH output, bi, bo "
			+ "MATCH (bi)-[:LATEST]->(ci:Commit)<-[:CONTAINS]-(bi) "
			+ "OPTIONAL MATCH (output)-[:CONTAINS]->(b:Branch)-[:CONTAINS]->(:Commit {copyIndex: ID(ci)}) "
			+ "FOREACH(ignoreMe IN CASE WHEN b IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (bo)-[:LATEST]->(co:Commit {commitID: ci.commitID, copyIndex: ID(ci)})<-[:CONTAINS]-(bo), (b)-[:CONTAINS]->(co)) "
			+ "FOREACH(ignoreMe IN CASE WHEN b IS NULL THEN [1] ELSE [] END | "
			+ "CREATE (bo)-[:LATEST]->(co:Commit {commitID: ci.commitID, copyIndex: ID(ci)})<-[:CONTAINS]-(bo)) "
			
			+ "WITH bi, bo "
			+ "MATCH (bi)-[:CONTAINS]->(ci:Commit)-[:CONTAINS]->(si:Snapshot) "
			+ "OPTIONAL MATCH (output)-[:CONTAINS]->(b:Branch)-[:CONTAINS]->(:Commit {copyIndex: ID(ci)}) "
			+ "OPTIONAL MATCH (ci)-[:SUCCEEDS]->(d:Commit)<-[:CONTAINS]-(bi) "
			+ "FOREACH(ignoreMe IN CASE WHEN b IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (bo)-[:CONTAINS]->(co:Commit {commitID: ci.commitID, copyIndex: ID(ci)}), (b)-[:CONTAINS]->(co)) "
			
			+ "CREATE UNIQUE (bo)-[:CONTAINS]->(co:Commit {commitID: ci.commitID, copyIndex: ID(ci)})-[:CONTAINS]->(so:Snapshot {idIndex: si.idIndex}) "
			
			+ "FOREACH(ignoreMe IN CASE WHEN d IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (co)-[:SUCCEEDS]->(:Commit {commitID: d.commitID})<-[:CONTAINS]-(bo)) "
			
			+ "WITH si, so "
			+ "MATCH (si)-[:CONTAINS]->(n:Node) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID, nodeType: n.nodeType})) "
			+ "WITH si, so, n as m "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(si) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(so)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(so))")
	void copyBranch(@Param("inputSpaceID") String inputSpaceID, @Param("inputBranchID") String inputBranchID, @Param("outputSpaceID") String outputSpaceID, @Param("outputBranchID") String outputBranchID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(bi:Branch {branchID: {inputBranchID}})-[:LATEST]->(ci:Commit)-[:CONTAINS]->(si:Snapshot)-[:CONTAINS]->(n:Node), (bi)-[:CONTAINS]->(ci) "
			+ "OPTIONAL MATCH (bi)-[:CONTAINS]->(c:Commit) "
			+ "WHERE NOT (bi)-[:LATEST]->(c:Commit) "
			+ "WITH target, bi, ci, c, si, collect(n) as nodes "
			+ "MERGE (target)-[:CONTAINS]->(bo:Branch {branchID: {outputBranchID}})-[:LATEST]->(co:Commit)-[:CONTAINS]->(so:Snapshot) "
			+ "ON CREATE SET bo.idIndex = bi.idIndex, so.idIndex = size(nodes) "
			+ "ON MATCH SET so.idIndex = so.idIndex + size(nodes) "
			+ "FOREACH(ignoreMe IN CASE WHEN bo.idIndex <= bi.idIndex THEN [1] ELSE [] END | "
			+ "SET bo.idIndex = bi.idIndex + 1, co.commitID = 'c' + bi.idIndex) "
			+ "CREATE UNIQUE (ci)<-[:CONTAINS]-(bo)-[:CONTAINS]->(co)-[:SUCCEEDS]->(ci) "
			+ "FOREACH(ignoreMe IN CASE WHEN c IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE (bo)-[:CONTAINS]->(c)) "
			+ "WITH si, nodes, so "
			+ "UNWIND range(0, size(nodes) - 1) as nodeIndex "
			+ "WITH si, nodeIndex, nodes[nodeIndex] as n, so "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (so)-[:CONTAINS]->(:Node {nodeID: 'n' + (so.idIndex - nodeIndex - 1), copyIndex: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (so)-[:CONTAINS]->(:Node {nodeID: 'n' + (so.idIndex - nodeIndex - 1), copyIndex: ID(n), nodeType: n.nodeType})) "
			+ "WITH si, n as m, so "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(si) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(so)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(so))")
	void copyBranch(@Param("targetSpaceID") String targetSpaceID, @Param("inputBranchID") String inputBranchID, @Param("outputBranchID") String outputBranchID);
	
	@Query("MATCH (c1:Commit)<-[l1:LATEST]-(b1:Branch {branchID: {targetBranchID1}})<-[:CONTAINS]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b2:Branch {branchID: {targetBranchID2}})-[:LATEST]->(c2:Commit), (b1)-[:CONTAINS]->(c1), (c1)<-[:CONTAINS]-(b2)-[:CONTAINS]->(c2) "
			+ "DELETE l1 "
			+ "CREATE (b1)-[:LATEST]->(c2) "
			+ "WITH b1, b2 "
			+ "OPTIONAL MATCH (b2)-[:CONTAINS]->(c:Commit) "
			+ "WHERE NOT (b1)-[:CONTAINS]->(c:Commit) "
			+ "FOREACH(ignoreMe IN CASE WHEN c IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE (b1)-[:CONTAINS]->(c))")
	void fastForwardBranch(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID1") String targetBranchID1, @Param("targetBranchID2") String targetBranchID2);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeType as tailType, e.componentRoles as componentRoles, "
			+ "n.nodeID as headID, n.nodeType as headType")
	List<Map<String, Object>> mapDesignSpace(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (l:Commit)<-[:LATEST]-(b:Branch)<-[:CONTAINS]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(h:Branch) "
			+ "OPTIONAL MATCH (b)-[:CONTAINS]->(c:Commit)-[:SUCCEEDS]->(d:Commit)<-[:CONTAINS]-(b) "
			+ "RETURN target.spaceID as spaceID, h.branchID as headBranchID, l.commitID as latestCommitID, ID(l) as latestCopyIndex, b.branchID as branchID, "
			+ "c.commitID as tailID, ID(c) as tailCopyIndex, d.commitID as headID, ID(d) as headCopyIndex")
	List<Map<String, Object>> mapBranches(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (n:Node)<-[:CONTAINS]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch)-[:CONTAINS]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(sn:Node) "
			+ "DETACH DELETE target "
			+ "DETACH DELETE n "
			+ "DETACH DELETE b "
			+ "DETACH DELETE c "
			+ "DETACH DELETE s "
			+ "DETACH DELETE sn")
	void deleteDesignSpace(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "DETACH DELETE n")
	void deleteNodes(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch {branchID: {targetBranchID}}) "
			+ "DETACH DELETE b")
	void deleteBranch(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);
	
	@Query("CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: 0})-[:CONTAINS]->(b:Branch {branchID: {outputSpaceID}, idIndex: 1})-[:CONTAINS]->(c:Commit {commitID: 'c0'})-[:CONTAINS]->(s:Snapshot {idIndex: 0}) "
			+ "CREATE (output)-[:SELECTS]->(b)-[:LATEST]->(c)")
	void createDesignSpace(@Param("outputSpaceID") String outputSpaceID);
	
	@Query("MATCH (target:DesignSpace)-[:SELECTS]->(hb:Branch)-[:CONTAINS]->(lc:Commit)<-[:LATEST]-(hb:Branch)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "OPTIONAL MATCH (hb)-[:CONTAINS]->(c:Commit) "
			+ "WHERE NOT (hb)-[:LATEST]->(c:Commit) "
			+ "CREATE (target)-[:CONTAINS]->(b:Branch {branchID: {outputBranchID}, idIndex: hb.idIndex}) "
			+ "CREATE (lc)<-[:LATEST]-(b)-[:CONTAINS]->(lc) "
			+ "FOREACH(ignoreMe IN CASE WHEN c IS NOT NULL THEN [1] ELSE [] END | "
			+ "CREATE (b)-[:CONTAINS]->(c))")
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
			+ "CREATE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyIndex: ID(n)})) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(n.nodeType) THEN [1] ELSE [] END | "
			+ "CREATE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyIndex: ID(n), nodeType: n.nodeType})) "
			+ "WITH input, n as m, output "
			+ "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(input) "
			+ "FOREACH(ignoreMe IN CASE WHEN NOT has(e.componentIDs) AND NOT has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(output)) "
			+ "FOREACH(ignoreMe IN CASE WHEN has(e.componentIDs) AND has(e.componentRoles) THEN [1] ELSE [] END | "
			+ "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(output))")
	void copyDesignSpace(@Param("inputSpaceID") String inputSpaceID, @Param("outputSpaceID") String outputSpaceID);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID1}})-[:CONTAINS]->(n1:Node {nodeID: {targetNodeID}}), (:DesignSpace {spaceID: {targetSpaceID2}})-[:CONTAINS]->(n2:Node {copyIndex: ID(n1)}) "
			+ "RETURN n2")
	Set<Node> findNodeCopy(@Param("targetSpaceID1") String targetSpaceID1, @Param("targetNodeID") String targetNodeID, @Param("targetSpaceID2") String targetSpaceID2);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b1:Branch {branchID: {targetBranchID1}})-[:LATEST]->(c1:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n1:Node {nodeID: {targetNodeID}}), (b1)-[:CONTAINS]->(c1), (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b2:Branch {branchID: {targetBranchID2}})-[:LATEST]->(c2:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n2:Node {copyIndex: ID(n1)}), (b2)-[:CONTAINS]->(c2) "
			+ "RETURN n2")
	Set<Node> findNodeCopy(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID1") String targetBranchID1, @Param("targetNodeID") String targetNodeID, @Param("targetBranchID2") String targetBranchID2);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
			+ "REMOVE n.copyIndex")
	void deleteNodeCopyIndices(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node), (b)-[:CONTAINS]->(c) "
			+ "REMOVE n.copyIndex")
	void deleteNodeCopyIndices(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch {branchID: {targetBranchID}})-[:CONTAINS]->(c:Commit) "
			+ "REMOVE c.copyIndex")
	void deleteCommitCopyIndices(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeType: {nodeType}}) "
			+ "RETURN n")
	Set<Node> getNodesByType(@Param("targetSpaceID") String targetSpaceID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node {nodeType: {nodeType}}), (b)-[:CONTAINS]->(c)  "
			+ "RETURN n")
	Set<Node> getNodesByType(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch) "
			+ "RETURN b.branchID as branchID")
	List<Map<String, Object>> getBranchIDs(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (b:Branch)<-[:CONTAINS]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(b:Branch) "
			+ "RETURN b.branchID as headBranchID")
	List<Map<String, Object>> getHeadBranchID(@Param("targetSpaceID") String targetSpaceID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch {branchID: {targetBranchID}}) "
			+ "CREATE (target)-[:SELECTS]->(b)")
	void selectHeadBranch(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "CREATE (target)-[:CONTAINS]->(:Node {nodeID: {outputNodeID}})")
	void createNode(@Param("targetSpaceID") String targetSpaceID, @Param("outputNodeID") String outputNodeID);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
			+ "CREATE (target)-[:CONTAINS]->(:Node {nodeID: {outputNodeID}, nodeType: {nodeType}})")
	void createTypedNode(@Param("targetSpaceID") String targetSpaceID, @Param("outputNodeID") String outputNodeID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot), (b)-[:CONTAINS]->(c) "
			+ "CREATE (s)-[:CONTAINS]->(:Node {nodeID: {outputNodeID}, nodeType: {nodeType}})")
	void createTypedNode(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID, @Param("outputNodeID") String outputNodeID, @Param("nodeType") String nodeType);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}}) "
			+ "REMOVE n.nodeType")
	void deleteNodeType(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID);
	
	@Query("MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}}), (b)-[:CONTAINS]->(c) "
			+ "REMOVE n.nodeType")
	void deleteNodeType(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID, @Param("targetNodeID") String targetNodeID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {targetTailID}})-[e:PRECEDES]->(n:Node {nodeID: {targetHeadID}})<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN e")
	Set<Edge> findEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "RETURN e")
	Set<Edge> getOutgoingEdges(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(b:Branch)-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(s:Snapshot)<-[:CONTAINS]-(c:Commit)<-[:LATEST]-(b:Branch)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID}, b.branchID = {targetBranchID} "
			+ "RETURN e")
	Set<Edge> getOutgoingEdges(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID, @Param("targetNodeID") String targetNodeID);
	
	@Query("MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}) "
			+ "CREATE (tail)-[:PRECEDES]->(head)")
	void createEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID);
	
	@Query("MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(s:Snapshot)<-[:CONTAINS]-(c:Commit)<-[:LATEST]-(b:Branch)<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch)-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}), (b)-[:CONTAINS]->(c) "
			+ "WHERE b.branchID = {targetBranchID} "
			+ "CREATE (tail)-[:PRECEDES]->(head)")
	void createEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID);
	
	@Query("MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}) "
			+ "CREATE (tail)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(head)")
	void createComponentEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID, 
			@Param("componentIDs") ArrayList<String> componentIDs, @Param("componentRoles") ArrayList<String> componentRoles);  
	
	@Query("MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(s:Snapshot)<-[:CONTAINS]-(c:Commit)<-[:LATEST]-(b:Branch)<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(b:Branch)-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}), (b)-[:CONTAINS]->(c) "
			+ "WHERE b.branchID = {targetBranchID} "
			+ "CREATE (tail)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(head)")
	void createComponentEdge(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID, @Param("targetTailID") String targetTailID, @Param("targetHeadID") String targetHeadID, 
			@Param("componentIDs") ArrayList<String> componentIDs, @Param("componentRoles") ArrayList<String> componentRoles);  
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID} "
			+ "DELETE e")
	void removeOutgoingEdges(@Param("targetSpaceID") String targetSpaceID, @Param("targetNodeID") String targetNodeID);
	
	@Query("MATCH (target:DesignSpace)-[:CONTAINS]->(b:Branch)-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(:Node)<-[:CONTAINS]-(s:Snapshot)<-[:CONTAINS]-(c:Commit)<-[:LATEST]-(b:Branch)<-[:CONTAINS]-(target:DesignSpace) "
			+ "WHERE target.spaceID = {targetSpaceID}, b.branchID = {targetBranchID} "
			+ "DELETE e")
	void removeOutgoingEdges(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID, @Param("targetNodeID") String targetNodeID);
	
}
