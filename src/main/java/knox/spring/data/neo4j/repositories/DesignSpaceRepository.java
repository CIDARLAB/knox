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

    @Query(
        "MATCH (dg:DesignGroup {groupID: $groupID})-[r:CONTAINS]->(target:DesignSpace) " +
        "OPTIONAL MATCH (target)-[:CONTAINS]->(n:Node) " +
        "OPTIONAL MATCH (target)-[:ARCHIVES]->(b:Branch)-[:CONTAINS]->(c:Commit)-[:CONTAINS]->(s:Snapshot) " +
        "OPTIONAL MATCH (s)-[:CONTAINS]->(sn:Node) " +
        "DETACH DELETE dg, target, n, b, c, s, sn"
    )
    void deleteSpacesInGroup(@Param("groupID") String groupID);

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

    @Query("MATCH (n:DesignSpace) RETURN n.spaceID ORDER BY n.spaceID")
    List<String> listDesignSpaces();

    @Query("MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID SET n.spaceID = $newSpaceID")
    void renameDesignSpace(@Param("targetSpaceID") String targetSpaceID, @Param("newSpaceID") String newSpaceID);

    @Query("MATCH (n:DesignSpace) RETURN count(n)")
    Integer getNumberOfDesignSpaces();

    @Query("MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID RETURN n.goldbar")
    String getGoldbarBySpaceID(@Param("targetSpaceID") String targetSpaceID);






    @Query(
        "MATCH (n:DesignSpace) WHERE n.spaceID = $targetSpaceID " +
        "MATCH (n)-[:CONTAINS]->(c:ContextSpace) " +
        "RETURN c.spaceID")
    String getContextSpaceID(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (space:DesignSpace {spaceID: $spaceID})-[:CONTAINS]->(start:Node) " +
        "WHERE ('start' IN coalesce(start.nodeTypes, [])) " +

        "MATCH p = (start)-[:PRECEDES*1..]->(accept:Node) " +
        "WHERE ('accept' IN coalesce(accept.nodeTypes, [])) " +

        "AND ALL(n IN nodes(p)[1..-1] " +
            "WHERE size([(n)-[:PRECEDES]->() | 1]) = 1 " +
            "AND size([(m)-[:PRECEDES]->(n) | 1]) = 1) " +
        "AND ALL(r IN relationships(p) " +
            "WHERE size(r.componentIDs) = 1 AND size(r.componentRoles) = 1) " +

        "RETURN " +
        "[r IN relationships(p) | r.componentIDs[0]] as compIDs, " +
        "[r IN relationships(p) | r.componentRoles[0]] as compRoles, " +
        "[r IN relationships(p) | toString(r.orientation)] as orientation, " +
        "[r IN relationships(p) | r.weight[0]] as weights"
    )
    DesignSpaceLinearDAGRepresentation getLinearDAGRepresentation(@Param("spaceID") String spaceID);
}
