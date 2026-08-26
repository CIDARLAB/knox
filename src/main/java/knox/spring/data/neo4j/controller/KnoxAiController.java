package knox.spring.data.neo4j.controller;

import knox.spring.data.neo4j.domain.Job;
import knox.spring.data.neo4j.services.DesignSpaceService;
import knox.spring.data.neo4j.services.KnoxAiClient;
import knox.spring.data.neo4j.services.ExperimentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class KnoxAiController {
    private final KnoxAiClient knoxAiClient; 
    final DesignSpaceService designSpaceService;
    final ExperimentService experimentService;

    // Bounded worker pool for training jobs.
    private final ExecutorService trainExecutor = Executors.newFixedThreadPool(1);
    
    public KnoxAiController(KnoxAiClient knoxAiClient, DesignSpaceService designSpaceService, ExperimentService experimentService) { 
        this.knoxAiClient = knoxAiClient; 
        this.designSpaceService = designSpaceService;
        this.experimentService = experimentService;
    } 

    // Endpoint to submit a job for a ML action
    @PostMapping("/job/submit")
    public ResponseEntity<Map<String, Object>> submitJob(
            @RequestParam(value = "action", required = true) String action,
            @RequestParam(value = "experimentName", required = true) String experimentName,
            @RequestParam(value = "model", required = true) String model,

            @RequestParam(value = "runName", required = false) String runName,

            @RequestParam(value = "config", required = false) String configJSON,
            @RequestParam(value = "task", required = false, defaultValue = "regression") String task,
            @RequestParam(value = "trainRatio", required = false, defaultValue = "0.8") double trainRatio,
            @RequestParam(value = "valRatio", required = false, defaultValue = "0.1") double valRatio,
            @RequestParam(value = "testRatio", required = false, defaultValue = "0.1") double testRatio,
            @RequestParam(value = "seed", required = false, defaultValue = "42") long seed,
            @RequestParam(value = "buildSurrogateModel", required = false, defaultValue = "false") Boolean buildSurrogateModel,
            @RequestParam(value = "interpretShap", required = false, defaultValue = "false") Boolean interpretShap,

            @RequestParam(value = "nTrials", required = false, defaultValue = "50") int nTrials,

            @RequestParam(value = "spaceIDs", required = false) List<String> spaceIDs,

            @RequestParam(value = "runID", required = false) String runID
    ) {
        String jobID = UUID.randomUUID().toString();
        Job job = new Job(jobID, experimentName, action, runName, model, "PENDING", runID);
        experimentService.addJobToExperiment(experimentName, job);

        if ("train".equals(action)){
            System.out.println("Submitting Train Job");
            CompletableFuture.runAsync(() -> knoxAiClient.runTrainJob(
                jobID, 
                job, 
                model, 
                configJSON, 
                task, 
                trainRatio, 
                valRatio, 
                testRatio, 
                seed, 
                buildSurrogateModel,
                interpretShap), trainExecutor
            );

        } else if ("predict".equals(action)) {
            System.out.println("Submitting Predict Job");
            CompletableFuture.runAsync(() -> knoxAiClient.runPredictJob(
                jobID, 
                job, 
                model, 
                runID, 
                spaceIDs), trainExecutor
            );

        } else if ("interpret".equals(action)) {
            System.out.println("Submitting Interpret Job");
            // TODO: Implement Interpret Job submission using knoxAiClient.runInterpretJob()

        } else if ("tune".equals(action)) {
            System.out.println("Submitting Tune Job");
            CompletableFuture.runAsync(() -> knoxAiClient.runTuneJob(
                jobID, 
                job, 
                model, 
                configJSON,
                task,
                trainRatio,
                valRatio,
                testRatio,
                seed,
                nTrials), trainExecutor
            );

        } else if ("generate".equals(action)) {
            System.out.println("Submitting Generate Job");
            // TODO: Implement Generate Job submission using knoxAiClient.runGenerateJob()

        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobID);
        response.put("status", "PENDING");
        response.put("experimentName", experimentName);
        response.put("runName", runName);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/job/stopTuning")
    public ResponseEntity<Map<String, Object>> stopTuningJob() {
        knoxAiClient.stopTune();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "STOPPED");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/job")
    public ResponseEntity<Map<String, Object>> deleteJob(
            @RequestParam(value = "jobID", required = true) String jobID
    ) {
        experimentService.deleteJob(jobID);
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobID);
        response.put("status", "DELETED");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/job")
    public ResponseEntity<Map<String, Object>> getJob(
            @RequestParam(value = "jobID", required = true) String jobID
    ) {
        Job job = experimentService.loadJob(jobID);
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobID);
        response.put("status", job.getStatus());
        response.put("experimentName", job.getExperimentName());
        response.put("runName", job.getRunName());
        response.put("mlflowRunID", job.getMlflowRunID());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
