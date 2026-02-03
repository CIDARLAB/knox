package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.ContextSpace;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author james roberts
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface ContextSpaceRepository extends Neo4jRepository<ContextSpace, Long> {

    @Query(
        "MATCH (target:ContextSpace {spaceID: $targetSpaceID}) " +
        "OPTIONAL MATCH (target)-[:INCLUDES]->(n:Component) " +
        "DETACH DELETE target, n")
    void deleteContextSpace(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:ContextSpace {spaceID: $targetSpaceID}) " + 
        "OPTIONAL MATCH (target)-[:INCLUDES]->(n:Component) " +
        "WHERE NOT ( ()-[:CONTAINS]->(target) ) " + 
        "DETACH DELETE target, n") 
    void deleteContextSpaceIfOrphan(@Param("targetSpaceID") String targetSpaceID);

}