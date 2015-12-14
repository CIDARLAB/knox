package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Node;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 24.07.12
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface DesignSpaceRepository extends GraphRepository<Node> {
	
//	Node findByTitle(@Param("title") String title);
//	
//	@Query("MATCH (m:Movie) WHERE m.title =~ ('(?i).*'+{title}+'.*') RETURN m")
//    Collection<Node> findByTitleContaining(@Param("title") String title);

    @Query("MATCH (m:Node)-[:PRECEDES]->(n:Node) RETURN m.displayID as tailID, n.displayID as headID")
    List<Map<String,Object>> graph(@Param("limit") int limit);
    
}

