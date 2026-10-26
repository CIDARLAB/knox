package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Categories;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface CategoriesRepository extends Neo4jRepository<Categories, Long> {

}
