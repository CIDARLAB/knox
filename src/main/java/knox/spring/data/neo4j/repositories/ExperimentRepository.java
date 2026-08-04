package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Experiment;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface ExperimentRepository extends Neo4jRepository<Experiment, Long> {

    @Query("MATCH (e:Experiment {experimentName: $experimentName}) RETURN ID(e) as graphId")
    Set<Integer> getExperimentGraphID(@Param("experimentName") String experimentName);

    @Query(
        "MATCH (target:Experiment {experimentName: $experimentName}) " +
        "OPTIONAL MATCH (target)-[:HAS_JOB]->(j:Job) " +
        "DETACH DELETE target, j"
    )
    void deleteExperiment(@Param("experimentName") String experimentName);

    @Query("MATCH (n:Experiment {experimentName: $experimentName}) RETURN n")
    Experiment findByExperimentName(@Param("experimentName") String experimentName);

    @Query("MATCH (n:Experiment) RETURN n.experimentName")
    List<String> listExperiments();

    @Query("MATCH (e:Experiment {experimentName: $experimentName})-[:HAS_PART_LIBRARY]->(p:PartLibrary) RETURN p.partLibraryName")
    String getPartLibraryNameFromExperiment(@Param("experimentName") String experimentName);

    @Query("MATCH (n:Experiment {experimentName: $experimentName}) RETURN COUNT(n) > 0")
    boolean isExperimentNameUsed(@Param("experimentName") String experimentName);
}
