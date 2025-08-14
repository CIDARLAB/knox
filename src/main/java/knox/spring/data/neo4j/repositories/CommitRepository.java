package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Commit;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author james roberts
 * @since 08.13.2025
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface CommitRepository extends Neo4jRepository<Commit, Long> {}
