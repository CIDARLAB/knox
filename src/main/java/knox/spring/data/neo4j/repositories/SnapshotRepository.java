package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Snapshot;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface SnapshotRepository extends PagingAndSortingRepository<Snapshot, Long> {
	
}
