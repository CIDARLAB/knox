package knox.spring.data.neo4j.domain;

import java.util.*;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.data.neo4j.core.schema.Property;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class Job {
    
    @Id
    @GeneratedValue
    private Long id;

    @Property
    private String jobID;

    @Property
    private String experimentName;

    @Property
    private String runName;

    @Property
    private String model;

    @Property
    private String status;

    @Property
    private String mlflowRunID;

    @Property
    private String errorMessage;

    public Job() {}

    public Job(String jobID, String experimentName, String runName, String model, String status, String mlflowRunID) {
        this.jobID = jobID;
        this.experimentName = experimentName;
        this.runName = runName;
        this.model = model;
        this.status = status;
        this.mlflowRunID = mlflowRunID;
        this.errorMessage = null;
    }

    public String getJobID() {
        return jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMlflowRunID() {
        return mlflowRunID;
    }

    public void setMlflowRunID(String mlflowRunID) {
        this.mlflowRunID = mlflowRunID;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, String> getJobInfo() {
        Map<String, String> jobInfo = new HashMap<>();
        jobInfo.put("jobID", jobID);
        jobInfo.put("experimentName", experimentName);
        jobInfo.put("runName", runName);
        jobInfo.put("model", model);
        jobInfo.put("status", status);
        jobInfo.put("mlflowRunID", mlflowRunID);
        jobInfo.put("errorMessage", errorMessage);
        return jobInfo;
    }

}
