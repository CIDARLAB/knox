package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.RuleEvaluation;

import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author james roberts
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface RuleEvaluationRepository extends Neo4jRepository<RuleEvaluation, Long> {

    @Query("MATCH (target:RuleEvaluation {evaluationName: $targetEvaluationName}) RETURN ID(target) as graphID")
    Set<Integer> getRuleEvaluationGraphID(@Param("targetEvaluationName") String targetEvaluationName);

    @Query(
        "MATCH (target:RuleEvaluation {evaluationName: $targetEvaluationName}) " +
        "DETACH DELETE target")
    void deleteRuleEvaluation(@Param("targetEvaluationName") String targetEvaluationName);

}
