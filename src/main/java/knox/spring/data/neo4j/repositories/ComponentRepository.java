package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Component;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author james roberts
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface ComponentRepository extends Neo4jRepository<Component, Long> {
    
}
