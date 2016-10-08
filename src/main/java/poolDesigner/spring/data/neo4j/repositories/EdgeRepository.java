package poolDesigner.spring.data.neo4j.repositories;

import poolDesigner.spring.data.neo4j.domain.Edge;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@RepositoryRestResource(collectionResourceRel = "poolDesigner", path = "poolDesigner")
public interface EdgeRepository extends GraphRepository<Edge> {
	
}
