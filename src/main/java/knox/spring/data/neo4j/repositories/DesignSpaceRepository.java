package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.dto.DesignSpaceEdgeDTO;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author james roberts
 * @since 08.13.2025
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface DesignSpaceRepository extends Neo4jRepository<DesignSpace, Long> {

    @Query(
        "CREATE (output:DesignSpace {spaceID: $outputSpaceID, idIndex: 0, mergeIndex: 0})-[:ARCHIVES]->(b:Branch {branchID: $outputSpaceID, idIndex: 0}) " +
        "CREATE (output)-[:SELECTS]->(b)")
    void createDesignSpace(@Param("outputSpaceID") String outputSpaceID);

    @Query(
        "CREATE (output:DesignSpace {spaceID: $outputSpaceID, idIndex: 2, mergeIndex: 0})-[:ARCHIVES]->(b:Branch {branchID: $outputSpaceID, idIndex: 0}) " +
        "CREATE (output)-[:SELECTS]->(b) " +
        "CREATE (output)-[:CONTAINS]->(m:Node {nodeID: 'n0', nodeType: 'start'}) " +
        "CREATE (output)-[:CONTAINS]->(n:Node {nodeID: 'n1', nodeType: 'accept'}) " +
        "CREATE (m)-[:PRECEDES {componentIDs: $componentIDs, componentRoles: $componentRoles}]->(n)")
    void createDesignSpace(@Param("outputSpaceID") String outputSpaceID,
                           @Param("componentIDs") ArrayList<String> componentIDs,
                           @Param("componentRoles") ArrayList<String> componentRoles);

    @Query(
        "MATCH (target:DesignSpace {spaceID: $targetSpaceID}) " +
        "OPTIONAL MATCH (target)-[:CONTAINS]->(n:Node) " +
        "OPTIONAL MATCH (target)-[:ARCHIVES]->(b:Branch)-[:CONTAINS]->(c:Commit)-[:CONTAINS]->(s:Snapshot) " +
        "OPTIONAL MATCH (s)-[:CONTAINS]->(sn:Node) " +
        "DETACH DELETE target, n, b, c, s, sn")
    void deleteDesignSpace(@Param("targetSpaceID") String targetSpaceID);

    DesignSpace findBySpaceID(@Param("spaceID") String spaceID);

    @Query("MATCH (target:DesignSpace) WHERE target.spaceID = $targetSpaceID RETURN count(target)")
    Integer countBySpaceID(@Param("targetSpaceID") String targetSpaceID);

    @Query("MATCH (target:DesignSpace {spaceID: $targetSpaceID}) RETURN ID(target) as graphID")
    Set<Integer> getDesignSpaceGraphID(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) " +
        "WHERE target.spaceID = $targetSpaceID " +
        "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeTypes as tailTypes, e.componentRoles as componentRoles, " +
        "e.componentIDs as componentIDs, e.weight as weight, e.orientation as orientation, n.nodeID as headID, n.nodeTypes as headTypes")
    List<DesignSpaceEdgeDTO> mapDesignSpace(@Param("targetSpaceID") String targetSpaceID);

    @Query("MATCH (n:DesignSpace) RETURN n.spaceID")
    List<String> listDesignSpaces();

    @Query("MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID SET n.spaceID = $newSpaceID")
    void renameDesignSpace(@Param("targetSpaceID") String targetSpaceID, @Param("newSpaceID") String newSpaceID);

    @Query("MATCH (n:DesignSpace) RETURN count(n)")
    Integer getNumberOfDesignSpaces();

    @Query("MATCH (n:DesignSpace) WHERE n.groupID = $group RETURN n.spaceID")
    List<String> listDesignSpaces(@Param("group") String group);

    @Query("MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID SET n.groupID = $group")
    void setGroupID(@Param("targetSpaceID") String targetSpaceID, @Param("group") String group);

    @Query("MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID SET n.groupID = 'none'")
    void removeGroupID(@Param("targetSpaceID") String targetSpaceID);

    @Query("MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID RETURN n.groupID")
    String getGroupID(@Param("targetSpaceID") String targetSpaceID);

    @Query("MATCH (n:DesignSpace) WHERE n.groupID = $group RETURN count(n)")
    Integer getGroupIDSize(@Param("group") String group);

    @Query("MATCH (n:DesignSpace) WHERE n.groupID IS NOT NULL RETURN DISTINCT n.groupID")
    List<String> getUniqueGroupIDs();

    @Query("MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID SET n.designGroupIndex = $designGroupIndex")
    void setDesignGroupIndex(@Param("targetSpaceID") String targetSpaceID, @Param("designGroupIndex") int designGroupIndex);

    @Query("MATCH (n:DesignSpace) WHERE n.groupID = $group RETURN n.spaceID, n.designGroupIndex ORDER BY n.designGroupIndex")
    List<Map<String, Object>> getDesignsFromGroup(@Param("group") String group);
}
