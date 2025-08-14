package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Branch;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author james roberts
 * @since 08.13.2025
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface BranchRepository extends Neo4jRepository<Branch, Long> {
    @Query(
        "MATCH (target:DesignSpace {spaceID: $targetSpaceID})-[:ARCHIVES]->(b:Branch {branchID: $targetBranchID}) " +
        "DETACH DELETE b")
    void deleteBranch(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (:DesignSpace {spaceID: $targetSpaceID})-[:ARCHIVES]->(b:Branch {branchID: $targetBranchID}) " +
        "RETURN b")
    Set<Branch> findBranch(@Param("targetSpaceID") String targetSpaceID, @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: $targetSpaceID})-[:ARCHIVES]->(b:Branch) " +
        "RETURN b.branchID as branchID")
    Set<String> getBranchIDs(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: $targetSpaceID})-[:SELECTS]->(hb:Branch) " +
        "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) " +
        "WHERE NOT (lc.mergeID IS NOT NULL) " +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(c:Commit)-[:SUCCEEDS]->(d:Commit)<-[:CONTAINS]-(b) " +
        "WHERE NOT (c.mergeID IS NOT NULL) AND NOT (d.mergeID IS NOT NULL) " +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, " +
        "c.commitID as tailID, ID(c) as tailCopyIndex, d.commitID as headID, ID(d) as headCopyIndex " +
        "UNION " +
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: $targetSpaceID})-[:SELECTS]->(hb:Branch) " +
        "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) " +
        "WHERE (lc.mergeID IS NOT NULL) " +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(cm:Commit)-[:SUCCEEDS]->(dm:Commit)<-[:CONTAINS]-(b) " +
        "WHERE (cm.mergeID IS NOT NULL) AND (dm.mergeID IS NOT NULL) " +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID + lc.mergeID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, " +
        "cm.commitID + cm.mergeID as tailID, ID(cm) as tailCopyIndex, dm.commitID + dm.mergeID as headID, ID(dm) as headCopyIndex " +
        "UNION " +
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: $targetSpaceID})-[:SELECTS]->(hb:Branch) " +
        "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) " +
        "WHERE NOT (lc.mergeID IS NOT NULL) " +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(ch:Commit)-[:SUCCEEDS]->(dh:Commit)<-[:CONTAINS]-(b) " +
        "WHERE NOT (ch.mergeID IS NOT NULL) AND (dh.mergeID IS NOT NULL) " +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, " +
        "ch.commitID as tailID, ID(ch) as tailCopyIndex, dh.commitID + dh.mergeID as headID, ID(dh) as headCopyIndex")
    List<Map<String, Object>> mapBranches(@Param("targetSpaceID") String targetSpaceID);
}
