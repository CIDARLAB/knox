package knox.spring.data.neo4j.domain;

import java.util.*;

import knox.spring.data.neo4j.domain.DesignGroup;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class Experiment {
    
    @Id
    @GeneratedValue
    private Long id;

    @Property
    private String experimentName;

    @Property
    private String experimentDescription;

    @Relationship(type = "HAS_DESIGNS_GROUP")
    private DesignGroup designsGroup;

    @Relationship(type = "HAS_RULES_GROUP")
    private DesignGroup rulesGroup;

    @Relationship(type = "HAS_RULES_TO_EVAL_GROUP")
    private DesignGroup rulesToEvalGroup;

    @Property
    private String ruleEvaluationName;

    @Relationship(type = "HAS_JOB")
    private Set<Job> jobs;

    @Relationship(type = "HAS_PART_LIBRARY")
    private PartLibrary partLibrary;

    public Experiment() {}

    public Experiment(String experimentName, String experimentDescription, DesignGroup designsGroup, DesignGroup rulesGroup, DesignGroup rulesToEvalGroup, String ruleEvaluationName, PartLibrary partLibrary) {
        this.experimentName = experimentName;
        this.experimentDescription = experimentDescription;
        this.designsGroup = designsGroup;
        this.rulesGroup = rulesGroup;
        this.rulesToEvalGroup = rulesToEvalGroup;
        this.ruleEvaluationName = ruleEvaluationName;
        this.jobs = new HashSet<>();
        this.partLibrary = partLibrary;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public String getExperimentDescription() {
        return experimentDescription;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public void setExperimentDescription(String experimentDescription) {
        this.experimentDescription = experimentDescription;
    }

    public DesignGroup getDesignsGroup() {
        return designsGroup;
    }

    public void setDesignsGroup(DesignGroup designsGroup) {
        this.designsGroup = designsGroup;
    }

    public DesignGroup getRulesGroup() {
        return rulesGroup;
    }

    public void setRulesGroup(DesignGroup rulesGroup) {
        this.rulesGroup = rulesGroup;
    }

    public DesignGroup getRulesToEvalGroup() {
        return rulesToEvalGroup;
    }

    public void setRulesToEvalGroup(DesignGroup rulesToEvalGroup) {
        this.rulesToEvalGroup = rulesToEvalGroup;
    }

    public String getRuleEvaluationName() {
        return ruleEvaluationName;
    }

    public void setRuleEvaluationName(String ruleEvaluationName) {
        this.ruleEvaluationName = ruleEvaluationName;
    }

    public Set<Job> getJobs() {
        return jobs;
    }

    public void setJobs(Set<Job> jobs) {
        this.jobs = jobs;
    }

    public PartLibrary getPartLibrary() {
        return partLibrary;
    }

    public void setPartLibrary(PartLibrary partLibrary) {
        this.partLibrary = partLibrary;
    }

    public void addJob(Job job) {
        if (this.jobs == null) {
            this.jobs = new HashSet<>();
        }
        this.jobs.add(job);
    }

    public Map<String, Object> getExperimentInfo() {
        Map<String, Object> experimentInfo = new HashMap<>();
		experimentInfo.put("experimentName", experimentName);
		experimentInfo.put("experimentDescription", experimentDescription);
		experimentInfo.put("designsGroupID", designsGroup != null ? designsGroup.getGroupID() : null);
		experimentInfo.put("rulesGroupID", rulesGroup != null ? rulesGroup.getGroupID() : null);
		experimentInfo.put("rulesToEvalGroupID", rulesToEvalGroup != null ? rulesToEvalGroup.getGroupID() : null);

        if (partLibrary != null) {
            // Part Information
            List<String> componentIDs = new ArrayList<>();
            List<String> componentRoles = new ArrayList<>();
            List<String> sequences = new ArrayList<>();
            List<String> descriptions = new ArrayList<>();
            List<List<Double>> partData = new ArrayList<>();
            partLibrary.partInformation(componentIDs, componentRoles, sequences, descriptions, partData);

            experimentInfo.put("partLibraryName", partLibrary.getPartLibraryName());
            experimentInfo.put("componentIDs", componentIDs);
            experimentInfo.put("componentRoles", componentRoles);
            experimentInfo.put("componentSequences", sequences);
            experimentInfo.put("componentDescriptions", descriptions);
            experimentInfo.put("partData", partData);
            experimentInfo.put("partDataLabels", partLibrary.getPartDataLabels());

            // Interaction Information
            List<String> sourceComponentIDs = new ArrayList<>();
            List<String> targetComponentIDs = new ArrayList<>();
            List<List<Double>> interactionData = new ArrayList<>();
            partLibrary.interactionInformation(sourceComponentIDs, targetComponentIDs, interactionData);

            experimentInfo.put("sourceComponentIDs", sourceComponentIDs);
            experimentInfo.put("targetComponentIDs", targetComponentIDs);
            experimentInfo.put("interactionData", interactionData);
            experimentInfo.put("interactionDataLabels", partLibrary.getInteractionDataLabels());
        } else {
            experimentInfo.put("partLibraryName", null);
            experimentInfo.put("componentIDs", new ArrayList<String>());
            experimentInfo.put("componentRoles", new ArrayList<String>());
            experimentInfo.put("componentSequences", new ArrayList<String>());
            experimentInfo.put("componentDescriptions", new ArrayList<String>());
            experimentInfo.put("partData", new ArrayList<List<Double>>());
        }
        
        List<Map<String, String>> jobsInfo = new ArrayList<>();
        if (jobs != null) {
            for (Job job : jobs) {
                jobsInfo.add(job.getJobInfo());
            }
        }
		experimentInfo.put("jobs", jobsInfo);

        return experimentInfo;
    }

    public int getVocabSize() {
        if (partLibrary == null) {
            return 0;
        }
        return partLibrary.getPartIndex();
    }

}
