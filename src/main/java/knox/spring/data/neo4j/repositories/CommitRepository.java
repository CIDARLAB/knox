package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Commit;

// import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface CommitRepository
    extends PagingAndSortingRepository<Commit, Long> {}
