package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Node;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Map;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface DesignSpaceRepository extends GraphRepository<Node> {
	
//	Node findByTitle(@Param("title") String title);
//	
//	@Query("MATCH (m:Movie) WHERE m.title =~ ('(?i).*'+{title}+'.*') RETURN m")
//    Collection<Node> findByTitleContaining(@Param("title") String title);

    @Query("MATCH (d:DesignSpace)-[c1:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[c2:CONTAINS]-(d:DesignSpace) WHERE d.displayID =~ ('(?i).*'+{designSpaceID}+'.*') RETURN m.displayID as tailID, m.nodeType as tailType, e.componentRole as componentRole, n.displayID as headID, n.nodeType as headType")
    List<Map<String,Object>> findDesignSpace(@Param("designSpaceID") String designSpaceID);
    
}

