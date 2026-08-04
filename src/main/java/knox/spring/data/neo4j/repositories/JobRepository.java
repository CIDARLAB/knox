package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Job;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface JobRepository extends Neo4jRepository<Job, Long> {

    @Query("MATCH (n:Job {jobID: $jobID}) RETURN n")
    Job findByJobID(@Param("jobID") String jobID);

    @Query("MATCH (n:Job {jobID: $jobID}) DETACH DELETE n")
    void deleteByJobID(@Param("jobID") String jobID);

}
