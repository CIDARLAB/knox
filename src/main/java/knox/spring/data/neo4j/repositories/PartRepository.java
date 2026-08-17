package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Part;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
@RepositoryRestResource
public interface PartRepository extends Neo4jRepository<Part, Long> {}
