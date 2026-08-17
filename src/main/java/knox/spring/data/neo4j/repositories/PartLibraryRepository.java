package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.PartLibrary;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface PartLibraryRepository extends Neo4jRepository<PartLibrary, Long> {

    @Query("MATCH (p:PartLibrary {partLibraryName: $partLibraryName}) RETURN ID(p) as graphId")
    Set<Integer> getPartLibraryGraphID(@Param("partLibraryName") String partLibraryName);

    @Query("MATCH (n:PartLibrary {partLibraryName: $partLibraryName}) DETACH DELETE n")
    void deleteByPartLibraryName(@Param("partLibraryName") String partLibraryName);

    @Query("MATCH (n:PartLibrary {partLibraryName: $partLibraryName}) WHERE NOT (n)<-[:CONTAINS]-(:SomeOtherNode) DETACH DELETE n")
    void deleteIfOrphaned(@Param("partLibraryName") String partLibraryName);

    @Query("MATCH (n:PartLibrary {partLibraryName: $partLibraryName}) RETURN n")
    PartLibrary findByPartLibraryName(@Param("partLibraryName") String partLibraryName);

    @Query("MATCH (n:PartLibrary) RETURN n.partLibraryName")
    List<String> listPartLibraries();
}
