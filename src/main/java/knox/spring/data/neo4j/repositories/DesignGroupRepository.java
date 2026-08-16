package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.DesignGroup;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface DesignGroupRepository extends Neo4jRepository<DesignGroup, Long> {

    @Query("MATCH (dg:DesignGroup {groupID: $groupID}) RETURN dg")
    DesignGroup findByDesignGroupID(@Param("groupID") String groupID);

    @Query("MATCH (dg:DesignGroup {groupID: $groupID}) DETACH DELETE dg")
    void deleteDesignGroup(@Param("groupID") String groupID);

    @Query(
        "MATCH (dg:DesignGroup {groupID: $groupID}) " +
        "OPTIONAL MATCH (dg)-[r:CONTAINS]->(target:DesignSpace) " +
        "OPTIONAL MATCH (target)-[:CONTAINS]->(n:Node) " +
        "OPTIONAL MATCH (target)-[:ARCHIVES]->(b:Branch)-[:CONTAINS]->(c:Commit)-[:CONTAINS]->(s:Snapshot) " +
        "OPTIONAL MATCH (s)-[:CONTAINS]->(sn:Node) " +
        "DETACH DELETE dg, target, n, b, c, s, sn"
    )
    void deleteSpacesInGroup(@Param("groupID") String groupID);

    @Query("MATCH (dg:DesignGroup) RETURN dg.groupID ORDER BY dg.groupID")
    List<String> listGroupIDs();

    @Query("MATCH (dg:DesignGroup {groupID: $groupID}) RETURN count(dg) > 0")
    boolean designGroupExists(@Param("groupID") String groupID);

    @Query(
        "MATCH (ds:DesignSpace {spaceID: $spaceID}) " +  // Finds DesignSpace

        "OPTIONAL MATCH (:DesignGroup)-[r:CONTAINS]->(ds:DesignSpace {spaceID: $spaceID}) " +  // Removes Existing DesignGroup Relation
        "DELETE r " +

        "MERGE (dg:DesignGroup {groupID: $groupID}) " +   // Creates new DesignGroup if not present
        "MERGE (dg)-[:CONTAINS]->(ds)"    // Adds new Relationship
    )
    void addSpaceToDesignGroup(@Param("groupID") String groupID, @Param("spaceID") String spaceID);

    @Query(
        "MATCH (dg:DesignGroup {groupID: $groupID})-[:CONTAINS]->(ds:DesignSpace) " +
        "RETURN ds.spaceID ORDER BY ds.spaceID"
    )
    List<String> getSpaceIDsInDesignGroup(@Param("groupID") String groupID);

    @Query(
        "MATCH (dg:DesignGroup {groupID: $groupID})-[:CONTAINS]->(ds:DesignSpace) " +
        "RETURN count(ds)"
    )
    Integer getSpaceCountInDesignGroup(@Param("groupID") String groupID);

    @Query(
        "MATCH (dg:DesignGroup)-[:CONTAINS]->(ds:DesignSpace {spaceID: $spaceID}) " +
        "RETURN dg.groupID"
    )
    String getDesignGroupIDForDesignSpace(@Param("spaceID") String spaceID);

}
