package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Branch;
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
    @Query(
        "CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: 0, mergeIndex: 0})-[:ARCHIVES]->(b:Branch {branchID: {outputSpaceID}, idIndex: 0}) "
        + "CREATE (output)-[:SELECTS]->(b)")
    void createDesignSpace(@Param("outputSpaceID") String outputSpaceID);

    @Query(
        "CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: 2, mergeIndex: 0})-[:ARCHIVES]->(b:Branch {branchID: {outputSpaceID}, idIndex: 0}) "
        + "CREATE (output)-[:SELECTS]->(b) "
        +
        "CREATE (output)-[:CONTAINS]->(m:Node {nodeID: 'n0', nodeType: 'start'}) "
        +
        "CREATE (output)-[:CONTAINS]->(n:Node {nodeID: 'n1', nodeType: 'accept'}) "
        +
        "CREATE (m)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(n)")
    void createDesignSpace(@Param("outputSpaceID") String outputSpaceID, 
    		@Param("componentIDs") ArrayList<String> componentIDs, 
    		@Param("componentRoles") ArrayList<String> componentRoles);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}}) "
        + "DETACH DELETE b")
    void deleteBranch(@Param("targetSpaceID") String targetSpaceID, 
    		@Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
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

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}}) "
        + "RETURN b")
    Set<Branch> findBranch(@Param("targetSpaceID") String targetSpaceID,
                           @Param("targetBranchID") String targetBranchID);

    DesignSpace findBySpaceID(@Param("spaceID") String spaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch) "
        + "RETURN b.branchID as branchID")
    Set<String> getBranchIDs(@Param("targetSpaceID") String targetSpaceID);

    @Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
           + "RETURN ID(target) as graphID")
    Set<Integer> getDesignSpaceGraphID(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
        + "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) "
        + "WHERE NOT exists(lc.mergeID) "
        +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(c:Commit)-[:SUCCEEDS]->(d:Commit)<-[:CONTAINS]-(b) "
        + "WHERE NOT exists(c.mergeID) AND NOT exists(d.mergeID) "
        +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, "
        +
        "c.commitID as tailID, ID(c) as tailCopyIndex, d.commitID as headID, ID(d) as headCopyIndex "
        + "UNION "
        +
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
        + "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) "
        + "WHERE exists(lc.mergeID) "
        +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(cm:Commit)-[:SUCCEEDS]->(dm:Commit)<-[:CONTAINS]-(b) "
        + "WHERE exists(cm.mergeID) AND exists(dm.mergeID) "
        +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID + lc.mergeID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, "
        +
        "cm.commitID + cm.mergeID as tailID, ID(cm) as tailCopyIndex, dm.commitID + dm.mergeID as headID, ID(dm) as headCopyIndex "
        + "UNION "
        +
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
        + "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) "
        + "WHERE NOT exists(lc.mergeID) "
        +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(ch:Commit)-[:SUCCEEDS]->(dh:Commit)<-[:CONTAINS]-(b) "
        + "WHERE NOT exists(ch.mergeID) AND exists(dh.mergeID) "
        +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, "
        +
        "ch.commitID as tailID, ID(ch) as tailCopyIndex, dh.commitID + dh.mergeID as headID, ID(dh) as headCopyIndex")
    List<Map<String, Object>> mapBranches(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
        + "WHERE target.spaceID = {targetSpaceID} "
        +
        "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeTypes as tailTypes, e.componentRoles as componentRoles, "
        + "e.componentIDs as componentIDs, e.weight as weight, e.orientation as orientation, n.nodeID as headID, n.nodeTypes as headTypes")
    List<Map<String, Object>> mapDesignSpace(@Param("targetSpaceID") String targetSpaceID);

    @Query("MATCH (n:DesignSpace) RETURN n.spaceID;")
    List<String> listDesignSpaces();
}
