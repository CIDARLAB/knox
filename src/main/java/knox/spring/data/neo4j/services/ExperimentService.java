package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.Experiment;
import knox.spring.data.neo4j.domain.DesignGroup;
import knox.spring.data.neo4j.domain.PartLibrary;
import knox.spring.data.neo4j.domain.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import knox.spring.data.neo4j.repositories.ExperimentRepository;
import knox.spring.data.neo4j.repositories.PartLibraryRepository;
import knox.spring.data.neo4j.repositories.PartRepository;
import knox.spring.data.neo4j.repositories.InteractionRepository;
import knox.spring.data.neo4j.repositories.JobRepository;

@Service
public class ExperimentService {
    final DesignSpaceService designSpaceService;

    final ExperimentRepository experimentRepository;
    
    final PartLibraryRepository partLibraryRepository;

    final PartRepository partRepository;

    final InteractionRepository interactionRepository;

    final JobRepository jobRepository;

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentService.class);

    public ExperimentService(
            DesignSpaceService designSpaceService,
            ExperimentRepository experimentRepository,
            PartLibraryRepository partLibraryRepository,
            PartRepository partRepository,
            InteractionRepository interactionRepository,
            JobRepository jobRepository
    ) {
        this.designSpaceService = designSpaceService;
        this.experimentRepository = experimentRepository;
        this.partLibraryRepository = partLibraryRepository;
        this.partRepository = partRepository;
        this.interactionRepository = interactionRepository;
        this.jobRepository = jobRepository;
    }

    public void createExperiment(
            String experimentName, 
            String experimentDescription, 
            String designsGroupID, 
            String rulesGroupID, 
            String rulesToEvalGroupID, 
            String ruleEvaluationName,
            String partLibraryName) {

        PartLibrary partLibrary = loadPartLibrary(partLibraryName);
        DesignGroup designsGroup = (designsGroupID == null || designsGroupID.isEmpty()) ? null : designSpaceService.loadDesignGroup(designsGroupID);
        DesignGroup rulesGroup = (rulesGroupID == null || rulesGroupID.isEmpty()) ? null : designSpaceService.loadDesignGroup(rulesGroupID);
        DesignGroup rulesToEvalGroup = (rulesToEvalGroupID == null || rulesToEvalGroupID.isEmpty()) ? null : designSpaceService.loadDesignGroup(rulesToEvalGroupID);

        if (isExperimentNameUsed(experimentName)) {
            throw new IllegalArgumentException("Experiment name already used: " + experimentName);
        }

        Experiment experiment = new Experiment(
            experimentName, 
            experimentDescription, 
            designsGroup, 
            rulesGroup, 
            rulesToEvalGroup,
            ruleEvaluationName,
            partLibrary
        );

        saveExperiment(experiment);
    }

    public void buildPartLibraryFromCSV(String partLibraryName, List<InputStream> inputCSVStreams) {
        List<BufferedReader> partLibraryReaders = new ArrayList<>();
        List<BufferedReader> partInteractionReaders = new ArrayList<>();
        List<String> partDataLabels = new ArrayList<>();
        List<String> interactionDataLabels = new ArrayList<>();

        for (InputStream inputCSVStream : inputCSVStreams) {
            try {
                String csvLine;
                BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputCSVStream));
                
                if ((csvLine = csvReader.readLine()) != null) {
                    ArrayList<String> csvArray = DesignSpaceService.csvToArrayList(csvLine);
                    
                    if (csvArray.size() > 0) {
                        if (csvArray.get(0).equals("id") && csvArray.get(1).equals("role")
                                && csvArray.get(2).equals("sequence") && csvArray.get(3).equals("description")) {
                            LOG.info("Valid part library CSV header detected.");
                            partLibraryReaders.add(csvReader);

                            for (int i = 4; i < csvArray.size(); i++) {
                                partDataLabels.add(csvArray.get(i));
                            }

                        } else if (csvArray.get(0).equals("from") && csvArray.get(1).equals("to")) {
                            LOG.info("Valid part interaction CSV header detected.");
                            partInteractionReaders.add(csvReader);

                            for (int i = 2; i < csvArray.size(); i++) {
                                interactionDataLabels.add(csvArray.get(i));
                            }

                        } else {
                            LOG.warn("Invalid CSV header detected: " + csvLine);
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error("Error reading CSV: " + e.getMessage());
            }
        }

        // Process Part Library CSV files
        List<String> componentIDs = new ArrayList<>();
        List<String> componentRoles = new ArrayList<>();
        List<String> componentSequences = new ArrayList<>();
        List<String> componentDescriptions = new ArrayList<>();
        List<List<Double>> componentPartData = new ArrayList<>();
        for (BufferedReader csvReader : partLibraryReaders) {
            String csvLine;
            try {
                while ((csvLine = csvReader.readLine()) != null) {
                    ArrayList<String> csvArray = new ArrayList<String>();
                    String[] splitData = csvLine.split("\\s*,\\s*");
                    for (int i = 0; i < splitData.length; i++) {
                        csvArray.add(splitData[i].trim());
                    }

                    while (csvArray.size() < 4) {
                        csvArray.add("missing");
                    }
                    
                    if (csvArray.size() > 0) {
                        componentIDs.add(csvArray.get(0).equals("") ? "missing" : csvArray.get(0));

                        // TODO: convert role to approprite sequence ontology
                        componentRoles.add(csvArray.get(1).equals("") ? "missing" : csvArray.get(1));

                        componentSequences.add(csvArray.get(2).equals("") ? "missing" : csvArray.get(2));
                        componentDescriptions.add(csvArray.get(3).equals("") ? "missing" : csvArray.get(3));
                    }

                    List<Double> partData = new ArrayList<>();
                    for (int i = 4; i < csvArray.size(); i++) {
                        try {
                            partData.add(Double.parseDouble(csvArray.get(i)));
                        } catch (NumberFormatException e) {
                            LOG.warn("Non-numeric part data: " + csvLine);
                            partData.add(Double.NaN);
                        }
                    }
                    componentPartData.add(partData);
                }
            } catch (IOException e) {
                LOG.error("Error reading part library CSV: " + e.getMessage());
            }
        }

        // Process Interaction Data CSV files
        HashMap<String, Map<String, List<Double>>> partToPartInteractionMap = new HashMap<>();
        for (BufferedReader csvReader : partInteractionReaders) {
            String csvLine;
            try {
                while ((csvLine = csvReader.readLine()) != null) {
                    ArrayList<String> csvArray = new ArrayList<String>();
                    String[] splitData = csvLine.split("\\s*,\\s*");
                    for (int i = 0; i < splitData.length; i++) {
                        csvArray.add(splitData[i].trim());
                    }

                    String fromPartID = csvArray.get(0);
                    String toPartID = csvArray.get(1);
                    List<Double> interactionData = new ArrayList<>();
                    for (int i = 2; i < csvArray.size(); i++) {
                        try {
                            interactionData.add(Double.parseDouble(csvArray.get(i)));
                        } catch (NumberFormatException e) {
                            LOG.warn("Skipping non-numeric interaction data: " + csvLine);
                            continue;
                        }
                    }

                    // Handle Missing from/to part IDs or interactions by skipping the interaction
                    if (fromPartID.equals("") || toPartID.equals("")) {
                        LOG.warn("Skipping interaction with missing from/to part IDs: " + csvLine);
                        continue;
                    } else if (!componentIDs.contains(fromPartID) || !componentIDs.contains(toPartID)) {
                        LOG.warn("Skipping interaction with unknown from/to part IDs: " + csvLine);
                        continue;
                    } else if (interactionData.size() != interactionDataLabels.size()) {
                        LOG.warn("Skipping interaction with missing interaction data: " + csvLine);
                        continue;
                    }

                    partToPartInteractionMap.computeIfAbsent(fromPartID, k -> new HashMap<>()).put(toPartID, interactionData);
                }

            } catch (IOException e) {
                LOG.error("Error reading interaction CSV: " + e.getMessage());
            }
        }

        // Create PartLibrary object
        PartLibrary partLibraryObj = new PartLibrary(
            partLibraryName,
            componentIDs,
            componentRoles,
            componentSequences,
            componentDescriptions,
            componentPartData,
            partDataLabels,
            interactionDataLabels
        );

        // Add interactions to PartLibrary object
        for (String fromPartID : partToPartInteractionMap.keySet()) {
            for (String toPartID : partToPartInteractionMap.get(fromPartID).keySet()) {
                List<Double> interactionData = partToPartInteractionMap.get(fromPartID).get(toPartID);
                partLibraryObj.addInteraction(fromPartID, toPartID, interactionData);
            }
        }

        // Save PartLibrary object to database
        savePartLibrary(partLibraryObj);
    }

    public Map<String, Object> getPartLibraryInfo(String partLibraryName) {
        LOG.info("Loading part library info for: {}", partLibraryName);
		PartLibrary partLibrary = loadPartLibrary(partLibraryName);
		Map<String, Object> partLibraryInfo = partLibrary.getPartLibraryInfo();
		return partLibraryInfo;
	}

    public Map<String, Object> d3GraphPartLibrary(String partLibraryName) {
        PartLibrary partLibrary = loadPartLibrary(partLibraryName);
        return partLibrary.toD3();
    }

    public Map<String, Map<String, List<String>>> listPartLibraryCategories(String partLibraryName) {
        PartLibrary partLibrary = loadPartLibrary(partLibraryName);
        return partLibrary.partLibraryToCategories();
    }

    public List<String> listExperiments() {
		return experimentRepository.listExperiments();
	}

    public List<String> listPartLibraries() {
		return partLibraryRepository.listPartLibraries();
	}

	public Map<String, Object> getExperimentInfo(String experimentName) {
        System.out.println("Loading experiment info for: " + experimentName);
		Experiment experiment = loadExperiment(experimentName);
		Map<String, Object> experimentInfo = experiment.getExperimentInfo();
        // System.out.println("Experiment info: " + experimentInfo);
		return experimentInfo;
	}

	public void addJobToExperiment(String experimentName, Job job) {
		Experiment experiment = loadExperiment(experimentName);
		experiment.addJob(job); 
		experimentRepository.save(experiment);
	}

    public void updateJobStatusByID(String jobID, String status) {
        Job job = loadJob(jobID);
        job.setStatus(status);
        jobRepository.save(job);
    }

    public void updateJobMlflowRunIDByID(String jobID, String mlflowRunID) {
        Job job = loadJob(jobID);
        job.setMlflowRunID(mlflowRunID);
        jobRepository.save(job);
    }

    public void updateJobErrorMessageByID(String jobID, String errorMessage) {
        Job job = loadJob(jobID);
        job.setErrorMessage(errorMessage);
        jobRepository.save(job);
    }

    public Experiment loadExperiment(String experimentName) {
        Long graphId = getExperimentGraphID(experimentName);
        Experiment targetExperiment = null;
        
        if (graphId != null) {
            targetExperiment = experimentRepository.findById(graphId).orElse(null);
        }

        if (targetExperiment == null) {
            throw new IllegalArgumentException("Experiment not found: " + experimentName);
        }

        return targetExperiment;
    }

    private Long getExperimentGraphID(String experimentName) {
        Set<Integer> graphIDs = experimentRepository.getExperimentGraphID(experimentName);

        if (graphIDs.size() > 0) {
			return (graphIDs.iterator().next()).longValue();
		} else {
			return null;
		}
    }

    public Job loadJob(String jobID) {
        Job job = jobRepository.findByJobID(jobID);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobID);
        }
        return job;
    }

    public PartLibrary loadPartLibrary(String partLibraryName) {
        Long graphId = getPartLibraryGraphID(partLibraryName);
        PartLibrary partLibrary = null;

        if (graphId != null) {
            partLibrary = partLibraryRepository.findById(graphId).orElse(null);
        }

        if (partLibrary == null) {
            throw new IllegalArgumentException("PartLibrary not found: " + partLibraryName);
        }

        return partLibrary;
	}

    private Long getPartLibraryGraphID(String partLibraryName) {
        Set<Integer> graphIDs = partLibraryRepository.getPartLibraryGraphID(partLibraryName);

        if (graphIDs.size() > 0) {
			return (graphIDs.iterator().next()).longValue();
		} else {
			return null;
		}
    }

    public void saveExperiment(Experiment experiment) {
		// TODO: check name uniqueness/alphanumeric

		System.out.println("\nSaving Experiment: " + experiment.getExperimentName());
		experimentRepository.save(experiment);
	}

    public void savePartLibrary(PartLibrary partLibrary) {
		// TODO: check name uniqueness/alphanumeric

		System.out.println("\nSaving PartLibrary: " + partLibrary.getPartLibraryName());
		partLibraryRepository.save(partLibrary);
	}

    public void deleteExperiment(String experimentName) {
        experimentRepository.deleteExperiment(experimentName);
    }

    public void deletePartLibrary(String partLibraryName) {
        if (partLibraryRepository.usedInExperiment(partLibraryName)) {
            LOG.warn("Cannot delete PartLibrary '{}' as it is used in an active Experiment.", partLibraryName);
            throw new IllegalArgumentException("Cannot delete PartLibrary '" + partLibraryName + "' as it is used in an active Experiment.");
        }
        partLibraryRepository.deletePartLibrary(partLibraryName);
    }

    public void deleteJob(String jobID) {
        jobRepository.deleteByJobID(jobID);
    }

    public boolean isExperimentNameUsed(String experimentName) {
        return experimentRepository.isExperimentNameUsed(experimentName);
    }
    
}
