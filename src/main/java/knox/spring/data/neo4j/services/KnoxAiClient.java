package knox.spring.data.neo4j.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import knox.spring.data.neo4j.analysis.ExperimentExport;
import knox.spring.data.neo4j.domain.Job;
import knox.spring.data.neo4j.domain.Experiment;
import knox.spring.data.neo4j.domain.RuleEvaluation;
import knox.spring.data.neo4j.domain.dto.DesignSpaceLinearDAGRepresentation;

@Service
public class KnoxAiClient {

    private final RestTemplate restTemplate;
    final DesignSpaceService designSpaceService;
    final ExperimentService experimentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${knox.ai.url}")
    private String knoxAiUrl; // KnoxAI API URL

    // Predict request and response records
    public record PredictResponse(List<Object> predictions) {}
    public record PredictRequest(String run_id, List<Object> samples) {}


    // Train request and response records
    public record TrainResponse(String run_id, Object shap_values, Object ebm_importances) {}
    public record TrainRequestTree(  // EBM, RF, XGBOOST
        Map<String, Object> data, 
        List<String> feature_names,
        Map<String, Object> config,
        String task,
        String experiment_name,
        String run_name,
        Boolean interpret_shap) {}
    public record TrainRequestNN(   // MLP, GNN, Transformer
        Map<String, Object> data, 
        Map<String, Object> rule_matrix, 
        Integer vocab_size,
        Map<String, Object> config,
        String task,
        String experiment_name,
        String run_name) {}


    // Evaluate requrest and response records
    public record EvaluateResponse(Map<String, Double> metrics) {}
    public record EvaluateRequest(
        String run_id, 
        List<Object> x_test, 
        List<Object> y_test, 
        List<String> feature_names
    ) {}


    // Tune request and response records
    public record TuneResponse(
        Map<String, Object> best_params,
        Double best_value,
        String metric,
        Integer trials
    ) {}
    public record TuneRequestTree(  // EBM, RF, XGBOOST
        Map<String, Object> data,
        List<String> feature_names,
        String task,
        Map<String, Object> config,
        Integer n_trials,
        String experiment_name
    ) {}
    public record TuneRequestNN(   // MLP, GNN, Transformer
        Map<String, List<Map<String, Object>>> data,
        Integer vocab_size,
        Map<String, Object> config,
        String task,
        Integer n_trials,
        String experiment_name
    ) {}


    // Config Response 
    public record ConfigResponse(Map<String, Object> config) {}


    public KnoxAiClient(RestTemplate restTemplate, DesignSpaceService designSpaceService, ExperimentService experimentService) {
        this.restTemplate = restTemplate;
        this.designSpaceService = designSpaceService;
        this.experimentService = experimentService;
    }

    public PredictResponse predict(
            String model,
            String run_id,
            List<Object> samples
    ) {
        PredictRequest req = new PredictRequest(run_id, samples);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<PredictRequest> entity = new HttpEntity<>(req, headers);

            ResponseEntity<PredictResponse> response = restTemplate.exchange(
                    knoxAiUrl +  "/" + model + "/predict",
                    HttpMethod.POST,
                    entity,
                    PredictResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI predict endpoint", e);
        }
    }

    public TrainResponse trainTree(
            String model,
            Map<String, Object> data,
            List<String> feature_names,
            Map<String, Object> config,
            String task,
            String experiment_name,
            String run_name,
            Boolean interpret_shap
    ) {
        TrainRequestTree req = new TrainRequestTree(
                data, feature_names, config, task, experiment_name, run_name, interpret_shap
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<TrainRequestTree> entity = new HttpEntity<>(req, headers);

            ResponseEntity<TrainResponse> response = restTemplate.exchange(
                    knoxAiUrl +  "/" + model + "/train",
                    HttpMethod.POST,
                    entity,
                    TrainResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI train endpoint", e);
        }
    }

    public TrainResponse trainNN(
            String model,
            Map<String, Object> data,
            Map<String, Object> rule_matrix,
            Integer vocab_size,
            Map<String, Object> config,
            String task,
            String experiment_name,
            String run_name
    ) {
        TrainRequestNN req = new TrainRequestNN(
                data, rule_matrix, vocab_size, config, task, experiment_name, run_name
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<TrainRequestNN> entity = new HttpEntity<>(req, headers);

            ResponseEntity<TrainResponse> response = restTemplate.exchange(
                    knoxAiUrl +  "/" + model + "/train",
                    HttpMethod.POST,
                    entity,
                    TrainResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI train endpoint", e);
        }
    }

    public EvaluateResponse evaluate(
            String model,
            String run_id,
            List<Object> x_test,
            List<Object> y_test,
            List<String> feature_names
    ) {
        EvaluateRequest req = new EvaluateRequest(run_id, x_test, y_test, feature_names);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<EvaluateRequest> entity = new HttpEntity<>(req, headers);

            ResponseEntity<EvaluateResponse> response = restTemplate.exchange(
                    knoxAiUrl +  "/" + model + "/evaluate",
                    HttpMethod.POST,
                    entity,
                    EvaluateResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI evaluate endpoint", e);
        }
    }

    public void deleteRunID(String run_id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    knoxAiUrl +  "/runs/" + run_id,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI delete run endpoint", e);
        }
    }

    public void deleteExperiment(String experiment_id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    knoxAiUrl +  "/experiments/" + experiment_id,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI delete experiment endpoint", e);
        }
    }

    public ConfigResponse getConfig(String model) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ConfigResponse> response = restTemplate.exchange(
                    knoxAiUrl +  "/" + model + "/config",
                    HttpMethod.GET,
                    entity,
                    ConfigResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI get config endpoint", e);
        }
    }

    public void stopTune() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    knoxAiUrl +  "/stop_tuning",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to call KnoxAI stop tune endpoint", e);
        }
    }

    public void runTrainJob(
            String jobID,
            Job job,
            String model,
            String configJSON,
            String task,
            double trainRatio,
            double valRatio,
            double testRatio,
            long seed,
            Boolean buildSurrogateModel,
            Boolean interpretShap
    ) {
        try {
            experimentService.updateJobStatusByID(jobID, "COMPILING");

            TrainPayload payload = buildTrainPayload(
                model, 
                job.getExperimentName(), 
                configJSON, 
                trainRatio, 
                valRatio, 
                testRatio, 
                seed
            );

            TrainResponse result = null;
            if (isNeuralNetworkModel(model)) {
                result = trainNN(
                        model, payload.data, null, payload.vocabSize, payload.config, task, job.getExperimentName(), job.getRunName()
                );
            } else if (isTreeBasedModel(model)) {
                result = trainTree(
                        model, payload.data, payload.featureNames, payload.config, task, job.getExperimentName(), job.getRunName(), interpretShap
                );
            } else {
                throw new IllegalArgumentException("Unsupported model type: " + model);
            }

            experimentService.updateJobStatusByID(jobID, "SUBMITTED");
            experimentService.updateJobMlflowRunIDByID(jobID, (result == null) ? null : result.run_id());
        } catch (Exception e) {
            experimentService.updateJobStatusByID(jobID, "FAILED");
            experimentService.updateJobErrorMessageByID(jobID, e.getMessage());
        }
    }

    private TrainPayload buildTrainPayload(
            String model,
            String experimentName,
            String configJSON,
            double trainRatio,
            double valRatio,
            double testRatio,
            long seed
    ) {
        long startTime = System.nanoTime();

        Experiment experiment = experimentService.loadExperiment(experimentName);
        if (experiment == null) {
            throw new IllegalArgumentException("Experiment not found: " + experimentName);
        }

        RuleEvaluation ruleEvaluation = null;
        if (!(experiment.getRuleEvaluationName() == null) && !experiment.getRuleEvaluationName().isBlank()) {
            ruleEvaluation = designSpaceService.loadRuleEvaluation(experiment.getRuleEvaluationName());
        }

        ExperimentExport experimentExport = new ExperimentExport(experiment);

        Map<String, Object> config = new HashMap<>();
        if (configJSON != null && !configJSON.isBlank()) {
            try {
                config = objectMapper.readValue(configJSON, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid config JSON: " + e.getMessage(), e);
            }
        }
        config.put("seed", seed);
        config.put("out_dim", 1);

        Map<String, List<Object>> data = new HashMap<>();
        Map<String, List<Object>> ruleMatrix = new HashMap<>();

        if ("transformer".equals(model) || "mlp".equals(model)) {
            List<DesignSpaceLinearDAGRepresentation> linearDAGRepresentations = designSpaceService.getLinearDAGRepresentationsParallel(
                new ArrayList<>(designSpaceService.getSpaceIDsInDesignGroup(experiment.getDesignsGroup().getGroupID()))
            );
            
            boolean padding = true;
            if ("transformer".equals(model)) {
                padding = false;
            }

            data = experimentExport.getSequenceDatapoints(
                model,
                linearDAGRepresentations,
                trainRatio, valRatio, testRatio, seed, padding
            );

        } else if (isTreeBasedModel(model)) {
            experimentExport.setRuleEvaluation(ruleEvaluation);
            data = experimentExport.getRuleEvaluationDatapoints(trainRatio, valRatio, testRatio, seed);
        }

        DesignSpaceService.printTime(startTime, model + "_BUILD_TRAIN_PAYLOAD");

        return new TrainPayload(
            new HashMap<>(data), 
            config, 
            experiment.getVocabSize(), 
            experimentExport.getRuleEvalFeatureNames().size() > 0 ? experimentExport.getRuleEvalFeatureNames() : null
        );
    }

    private static class TrainPayload {
        final Map<String, Object> data;
        final Map<String, Object> config;
        final Integer vocabSize;
        final List<String> featureNames;

        TrainPayload(Map<String, Object> data, Map<String, Object> config, Integer vocabSize, List<String> featureNames) {
            this.data = data;
            this.config = config;
            this.vocabSize = vocabSize;
            this.featureNames = featureNames;
        }
    }

    private boolean isNeuralNetworkModel(String model) {
        return "gnn".equals(model) || "transformer".equals(model) || "mlp".equals(model);
    }

    private boolean isTreeBasedModel(String model) {
        return "random_forest".equals(model) || "xgboost".equals(model) || "ebm".equals(model);
    }

}

