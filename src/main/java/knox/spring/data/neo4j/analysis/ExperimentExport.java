package knox.spring.data.neo4j.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import knox.spring.data.neo4j.domain.Experiment;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.RuleEvaluation;
import knox.spring.data.neo4j.domain.dto.DesignSpaceLinearDAGRepresentation;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;

public class ExperimentExport {

    private Experiment experiment;

    private RuleEvaluation ruleEvaluation;

    private List<String> ruleEvalFeatureNames;

    private Map<String, Map<String, Object>> compIDMap;

    public ExperimentExport() {}

    public ExperimentExport(Experiment experiment) {
        this.experiment = experiment;
        this.ruleEvaluation = null;
        this.ruleEvalFeatureNames = new ArrayList<>();
        this.compIDMap = null;
    }

    public ExperimentExport(Experiment experiment, RuleEvaluation ruleEvaluation) {
        this.experiment = experiment;
        this.ruleEvaluation = ruleEvaluation;
        this.ruleEvalFeatureNames = new ArrayList<>();
        this.compIDMap = null;
    }

    // Used for Tree Based Models
    public Map<String, List<Object>> getRuleEvaluationDatapoints(
            double trainRatio,
            double valRatio,
            double testRatio, 
            long seed) {

        Map<String, List<Object>> datapoints = new HashMap<>();

        // TODO: Specify Rule Types to use

        ArrayList<String> ruleSpaceIDs = ruleEvaluation.getRuleSpaceIDs();
        ruleEvalFeatureNames = new ArrayList<>(ruleSpaceIDs);

        // Initialize the features matrix
        double[][] xDatapoints = ruleEvaluation.getFeatures();

        // TODO: Handle Highly Correlated Rules

        // get the target metric values for each design
        List<Double> yDatapoints = ruleEvaluation.getDesignScores();

        // Combine x and y datapoints into a single list for splitting
        List<List<Double>> datapointsCombined = new ArrayList<>();
        for (int i = 0; i < xDatapoints.length; i++) {
            List<Double> combined = new ArrayList<>();
            for (int j = 0; j < xDatapoints[i].length; j++) {
                combined.add(xDatapoints[i][j]);
            }
            combined.add(yDatapoints.get(i));
            datapointsCombined.add(combined);
        }

        // Split and Shuffle
        Map<String, List<Object>> split = getTrainValTestSplit(new ArrayList<>(datapointsCombined), trainRatio, valRatio, testRatio, seed);

        // Populate the final datapoints map with the split data
        for (String key : split.keySet()) {   //train, val, test
            List<List<Double>> features = new ArrayList<>();
            List<Double> targets = new ArrayList<>();

            for (Object obj : split.get(key)) {
                List<Double> combined = (List<Double>) obj;
                features.add(combined.subList(0, combined.size() - 1));
                targets.add(combined.get(combined.size() - 1));
            }

            datapoints.put("x_" + key, new ArrayList<>(features));
            datapoints.put("y_" + key, new ArrayList<>(targets));
        }

        return datapoints;
    }
    
    // Used for Transformer and MLP models
    public Map<String, List<Object>> getSequenceDatapoints(
            String model,
            List<DesignSpaceLinearDAGRepresentation> designRepresentations,
            double trainRatio,
            double valRatio,
            double testRatio, 
            long seed,
            boolean padding) {

        this.experiment.getPartLibrary().buildCompIDMap();
        this.compIDMap = experiment.getPartLibrary().getCompIDMap();

        // Use fixed thread pool to limit concurrent DB connections
        int size = designRepresentations.size();
		int numThreads = Math.min(size, Runtime.getRuntime().availableProcessors() * 2);
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		try {
			// Create futures for all load operations
			List<CompletableFuture<Object>> futures = new ArrayList<>(size);
			
			for (DesignSpaceLinearDAGRepresentation designRepresentation : designRepresentations) {
				CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
					Map<String, Object> datapoint = new HashMap<>();
                    datapoint.put("token_ids", getTokenIDs(designRepresentation, padding));

                    // TODO Change output to Map<String, Object>
                    //datapoint.put("sequence", Collections.singletonList(getDesignSequence(design)));

                    //datapoint.put("features", new ArrayList<>());
                    datapoint.put("y", Collections.singletonList(designRepresentation.getWeights().get(0)));

					return datapoint;
				}, executor);
				futures.add(future);
			}
			
			// Wait for all to complete and collect results
			List<Object> datapoints = new ArrayList<>(size);
			for (CompletableFuture<Object> future : futures) {
				datapoints.add(future.join()); // Throws exception if any failed
			}

            return getTrainValTestSplit(datapoints, trainRatio, valRatio, testRatio, seed);
			
		} finally {
			executor.shutdown();
		}

    }

    // Used for GNN models
    public Map<String, List<Object>> getGNNDatapoints(List<NodeSpace> nodeSpaces,
            double trainRatio,
            double valRatio,
            double testRatio, 
            long seed) {

        this.experiment.getPartLibrary().buildCompIDMap();
        this.compIDMap = experiment.getPartLibrary().getCompIDMap();

        List<Object> datapoints = new ArrayList<>();
        for (NodeSpace space : nodeSpaces) {
            Map<String, List<String>> design = getDesignRepresentation(space);

            Map<String, Object> datapoint = new HashMap<>();
            //datapoint.put("node_labels", getTokenIDs(design, false).get(0));
            datapoint.put("node_features", buildNodeFeatures(design));
            //datapoint.put("node_sequence", getNodeSequences(design));
            datapoint.put("edge_index", buildEdgeIndex(design));
            //datapoint.put("edge_attr", new ArrayList<>());
            //datapoint.put("edge_labels", new ArrayList<>());
            //datapoint.put("features", new ArrayList<>());
            
            // TODO Change output to Map<String, Object>
            //datapoint.put("sequence", Collections.singletonList(getDesignSequence(design)));

            datapoint.put("y", getTargetMetricValue(space));

            datapoints.add(datapoint);
        }

        return getTrainValTestSplit(datapoints, trainRatio, valRatio, testRatio, seed);
    }

    private List<List<Integer>> getTokenIDs(DesignSpaceLinearDAGRepresentation design, boolean padding) {
        List<List<Integer>> sequences = Collections.singletonList(designPartsToIndexList(design));
        if (padding) {
            padSequences(sequences);
        }
        return sequences;
    }

    private List<Double> getTargetMetricValue(NodeSpace nodeSpace) {
        return Collections.singletonList(nodeSpace.getAvgScoreofAllNonBlankEdges());
    }

    private List<Integer> designPartsToIndexList(DesignSpaceLinearDAGRepresentation design) {
        List<Integer> tokenIDs = new ArrayList<>();
        for (String componentID : design.getCompIDs()) {
            Integer tokenID = compIDMap.containsKey(componentID) ? (Integer) compIDMap.get(componentID).get("tokenID") : null;

            if (tokenID != null) {
                tokenIDs.add(tokenID);
            }
        }

        return tokenIDs;
    }

    private void padSequences(List<List<Integer>> sequences) {
        int paddingValue = 0;
        int maxLength = sequences.stream().mapToInt(List::size).max().orElse(0);

        for (List<Integer> sequence : sequences) {
            while (sequence.size() < maxLength) {
                sequence.add(paddingValue);
            }
        }
    }

    private List<List<Number>> buildNodeFeatures(Map<String, List<String>> design) {
        List<List<Number>> nodeFeatures = new ArrayList<>();
        List<String> uniqueRoles = experiment.getPartLibrary().getUniqueComponentRoles();
        Map<String, Integer> roleIndex = new HashMap<>();

        for (int i = 0; i < uniqueRoles.size(); i++) {
            roleIndex.put(uniqueRoles.get(i), i);
        }

        for (int i = 0; i < design.get("compIDs").size(); i++) {

            // node role
            String role = design.get("compRoles").get(i);
            Integer rolePos = roleIndex.get(role);
            if (rolePos == null) {
                throw new IllegalArgumentException("Unknown component role: " + role);
            }

            List<Number> features = new ArrayList<>(Collections.nCopies(uniqueRoles.size(), 0));
            features.set(rolePos, 1);

            // orientation
            String orientation = design.get("compOrientations").get(i);
            if ("inline".equals(orientation)) {
                features.add(1);
            } else if ("reverseComplement".equals(orientation)) {
                features.add(-1);
            } else {
                features.add(0);
            }

            // TODO: Individual part Data (from part library)

            nodeFeatures.add(features);
        }
        return nodeFeatures;
    }

    private List<List<Integer>> buildEdgeIndex(Map<String, List<String>> design) {
        List<Integer> src = new ArrayList<>();
        List<Integer> dst = new ArrayList<>();

        // Build edge index for a linear chain of components (bidirectional) - Structure
        for (int i = 0; i < design.get("compIDs").size() - 1; i++) {
            src.add(i);
            dst.add(i + 1);
            src.add(i + 1);
            dst.add(i);
        }

        List<List<Integer>> edgeIndex = new ArrayList<>();
        edgeIndex.add(src);
        edgeIndex.add(dst);

        return edgeIndex;
    }

    private String getDesignSequence(Map<String, List<String>> design) {
        String sequence = "";
        List<String> nodeSequences = getNodeSequences(design);

        if (nodeSequences == null) {
            return null;
        }

        for (String seq : nodeSequences) {
            sequence += seq;
        }
        return sequence;
    }

    private List<String> getNodeSequences(Map<String, List<String>> design) {
        List<String> nodeSequences = new ArrayList<>();

        for (String compID : design.get("compIDs")) {
            if (compIDMap.containsKey(compID)) {
                String seq = (String) compIDMap.get(compID).get("componentSequence");
                
                if (seq == null || seq.equals("missing")) {
                    return null;
                }

                nodeSequences.add((String) compIDMap.get(compID).get("componentSequence"));

            } else {
                return null;
            }
        }

        return nodeSequences;
    }

    private Map<String, List<String>> getDesignRepresentation(NodeSpace nodeSpace) {
        // NOTE: The design space should be linear (i.e., each design can be represented as a single sequence of components)
        // Linear Directed Acyclic Graph (DAG) assumption for the design space
        Map<String, List<String>> representation = nodeSpace.getLinearDAGRepresentation();
        if (representation.isEmpty()) {
            throw new IllegalArgumentException("NodeSpace is not a linear DAG");
        }
        return representation;
    }

    private Map<String, List<Object>> getTrainValTestSplit(
            List<Object> datapoints,
            double trainRatio,
            double valRatio,
            double testRatio,
            long seed) {

        double sum = trainRatio + valRatio + testRatio;
        if (Math.abs(sum - 1.0) > 1e-9) {
            throw new IllegalArgumentException("Ratios must sum to 1.0");
        }

        List<Object> shuffled = new ArrayList<>(datapoints);
        Collections.shuffle(shuffled, new Random(seed)); // deterministic randomness

        int n = shuffled.size();
        int trainSize = (int) Math.floor(n * trainRatio);
        int valSize = (int) Math.floor(n * valRatio);
        int testSize = n - trainSize - valSize; // absorb rounding remainder

        List<Object> train = new ArrayList<>(shuffled.subList(0, trainSize));
        List<Object> val = new ArrayList<>(shuffled.subList(trainSize, trainSize + valSize));
        List<Object> test = new ArrayList<>(shuffled.subList(trainSize + valSize, trainSize + valSize + testSize));

        Map<String, List<Object>> split = new HashMap<>();
        split.put("train", train);
        if (val.size() > 0) split.put("val", val);
        split.put("test", test);

        /*for (String key : split.keySet()) {
            System.out.println("Split " + key + " size: " + split.get(key).size());
        }*/

        return split;
    }

    public void setRuleEvaluation(RuleEvaluation ruleEvaluation) {
        this.ruleEvaluation = ruleEvaluation;
        this.ruleEvalFeatureNames = new ArrayList<>();
    }

    public List<String> getRuleEvalFeatureNames() {
        return ruleEvalFeatureNames;
    }

}
