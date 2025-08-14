package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Node;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author james roberts
 * @since 08.13.2025
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface NodeRepository extends Neo4jRepository<Node, Long> {}
