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
import knox.spring.data.neo4j.domain.PartLibrary;
import knox.spring.data.neo4j.domain.Part;
import knox.spring.data.neo4j.domain.Interaction;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.RuleEvaluation;
import knox.spring.data.neo4j.domain.dto.DesignSpaceLinearDAGRepresentation;

public class ExperimentExport {

    private Experiment experiment;

    private RuleEvaluation ruleEvaluation;

    private List<String> ruleEvalFeatureNames;

    private Map<String, Part> compIDMap;

    private PartLibrary partLibrary;

    private int featureDim;

    private int nodeDim;

    private int edgeDim;

    public ExperimentExport() {}

    public ExperimentExport(Experiment experiment) {
        this.experiment = experiment;
        this.ruleEvaluation = null;
        this.ruleEvalFeatureNames = new ArrayList<>();
        this.compIDMap = experiment.getPartLibrary().getCompIDMap();
        this.partLibrary = experiment.getPartLibrary();
        this.featureDim = 0;
        this.nodeDim = 0;
        this.edgeDim = 0;
    }

    public ExperimentExport(Experiment experiment, RuleEvaluation ruleEvaluation) {
        this.experiment = experiment;
        this.ruleEvaluation = ruleEvaluation;
        this.ruleEvalFeatureNames = new ArrayList<>();
        this.compIDMap = experiment.getPartLibrary().getCompIDMap();
        this.partLibrary = experiment.getPartLibrary();
        this.featureDim = 0;
        this.nodeDim = 0;
        this.edgeDim = 0;
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

    public Map<String, Object> getRuleEvaluationDatapoints(List<NodeSpace> nodeSpaces, List<NodeSpace> ruleSpaces, boolean includeTarget) {
        Map<String, Object> datapoint = new HashMap<>();

        RuleEvaluation tempRuleEval = new RuleEvaluation(new ArrayList<>(ruleSpaces), new ArrayList<>(nodeSpaces));
        tempRuleEval.runEvaluationParallel();

        double[][] xDatapoints = tempRuleEval.getFeatures();

        datapoint.put("samples", new ArrayList<>(List.of(xDatapoints)));

        if (includeTarget) {
            List<Double> yDatapoints = tempRuleEval.getDesignScores();
            datapoint.put("targets", new ArrayList<>(yDatapoints));
        }
        
        return datapoint;
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

        // Use fixed thread pool to limit concurrent DB connections
        int size = designRepresentations.size();
		int numThreads = Math.min(size, Runtime.getRuntime().availableProcessors() * 2);
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		try {
			// Create futures for all load operations
			List<CompletableFuture<Object>> futures = new ArrayList<>(size);
			
			for (DesignSpaceLinearDAGRepresentation designRepresentation : designRepresentations) {
				CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
					return sequenceDatapoint(designRepresentation, padding, true);
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

    public Map<String, Object> sequenceDatapoint(DesignSpaceLinearDAGRepresentation design, boolean padding, boolean includeTarget) {
        Map<String, Object> datapoint = new HashMap<>();
        datapoint.put("token_ids", getTokenIDs(design, padding));

        //datapoint.put("sequence", getDesignSequence(design));

        //datapoint.put("features", new ArrayList<>()); // Design-level features

        if (includeTarget) {
            datapoint.put("y", getTargetMetricValue(design));
        }

        return datapoint;
    }

    // Used for GNN models
    public Map<String, List<Object>> getGNNDatapoints(
            List<DesignSpaceLinearDAGRepresentation> designRepresentations,
            double trainRatio,
            double valRatio,
            double testRatio, 
            long seed) {

        // Use fixed thread pool to limit concurrent DB connections
        int size = designRepresentations.size();
		int numThreads = Math.min(size, Runtime.getRuntime().availableProcessors() * 2);
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		try {
			// Create futures for all load operations
			List<CompletableFuture<Object>> futures = new ArrayList<>(size);
			
			for (DesignSpaceLinearDAGRepresentation designRepresentation : designRepresentations) {
				CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
					return gnnDatapoint(designRepresentation, true);
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

    public Map<String, Object> gnnDatapoint(DesignSpaceLinearDAGRepresentation design, boolean includeTarget) {
        Map<String, Object> datapoint = new HashMap<>();
        datapoint.put("node_labels", getTokenIDs(design, false).get(0));
        datapoint.put("node_features", buildNodeFeatures(design));
        //datapoint.put("node_sequence", getNodeSequences(design));

        List<List<Double>> edgeAttr = new ArrayList<>();
        List<Integer> edgeLabels = new ArrayList<>();
        datapoint.put("edge_index", buildEdgeIndex(design, edgeAttr, edgeLabels));
        datapoint.put("edge_attr", edgeAttr);
        datapoint.put("edge_labels", edgeLabels);
        
        //datapoint.put("features", new ArrayList<>()); // Design-level features
        
        //datapoint.put("sequence", getDesignSequence(design));

        if (includeTarget) {
            datapoint.put("y", getTargetMetricValue(design));
        }

        return datapoint;
    }

    private List<Double> getTargetMetricValue(DesignSpaceLinearDAGRepresentation design) {
        return Collections.singletonList(design.getWeights().get(0));
    }

    private List<List<Integer>> getTokenIDs(DesignSpaceLinearDAGRepresentation design, boolean padding) {
        List<List<Integer>> sequences = Collections.singletonList(designPartsToIndexList(design));
        if (padding) {
            padSequences(sequences);
        }
        return sequences;
    }

    private List<Integer> designPartsToIndexList(DesignSpaceLinearDAGRepresentation design) {
        List<Integer> tokenIDs = new ArrayList<>();
        for (String componentID : design.getCompIDs()) {
            Integer tokenID = compIDMap.containsKey(componentID) ? compIDMap.get(componentID).getIndex() : null;

            if (tokenID != null) {
                tokenIDs.add(tokenID);

            } else {
                throw new IllegalArgumentException("Component ID not found in PartLibrary: " + componentID);
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

    private List<List<Number>> buildNodeFeatures(DesignSpaceLinearDAGRepresentation design) {
        List<List<Number>> nodeFeatures = new ArrayList<>();
        List<String> uniqueRoles = partLibrary.getUniqueComponentRoles();
        Map<String, Integer> roleIndex = new HashMap<>();

        for (int i = 0; i < uniqueRoles.size(); i++) {
            roleIndex.put(uniqueRoles.get(i), i);
        }

        for (int i = 0; i < design.getCompIDs().size(); i++) {
            Part part = compIDMap.get(design.getCompIDs().get(i));

            // node role
            String role = part.getComponentRole();
            Integer rolePos = roleIndex.get(role);
            if (rolePos == null) {
                throw new IllegalArgumentException("Unknown component role: " + role);
            }

            List<Number> features = new ArrayList<>(Collections.nCopies(uniqueRoles.size(), 0));
            features.set(rolePos, 1);

            // orientation
            String orientation = design.getOrientation().get(i);
            if ("INLINE".equals(orientation)) {
                features.add(1);
            } else if ("REVERSE_COMPLEMENT".equals(orientation)) {
                features.add(-1);
            } else {
                features.add(0);
            }

            // Individual part Data (from part library)
            List<Double> partData = part.getPartData();
            if (partData != null) {
                features.addAll(partData);
            }

            nodeFeatures.add(features);
        }

        this.nodeDim = nodeFeatures.isEmpty() ? 0 : nodeFeatures.get(0).size();

        return nodeFeatures;
    }

    private List<List<Integer>> buildEdgeIndex(DesignSpaceLinearDAGRepresentation design, List<List<Double>> edgeAttr, List<Integer> edgeLabels) {
        List<Integer> src = new ArrayList<>();
        List<Integer> dst = new ArrayList<>();

        Integer edgeFeatureSize = null;

        // Build edges for Interactions
        for (int i = 0; i < design.getCompIDs().size() - 1; i++) {
            
            for (Interaction interaction : partLibrary.getPartByComponentID(design.getCompIDs().get(i)).getInteractions()) {
                String targetCompID = interaction.getTargetPart().getComponentID();
                int targetIndex = design.getCompIDs().indexOf(targetCompID);

                if (targetIndex != -1) {
                    // Update Edge Index
                    src.add(i);
                    dst.add(targetIndex);

                    // Update Edge Labels
                    edgeLabels.add(1); // 1 for interaction

                    // Update Edge Attributes
                    if (interaction.getInteractionData() != null) {
                        List<Double> interactionData = interaction.getInteractionData();
                        if (edgeFeatureSize == null) {
                            edgeFeatureSize = interactionData.size();
                        } else if (interactionData.size() != edgeFeatureSize) {
                            throw new IllegalArgumentException("Inconsistent edge feature sizes: expected " + edgeFeatureSize + ", got " + interactionData.size());
                        }
                        edgeAttr.add(interactionData);
                    } else {
                        // If no interaction data, add a zero vector of the same size as other edges
                        if (edgeFeatureSize == null) {
                            throw new IllegalArgumentException("Edge feature size is not defined yet.");
                        }
                        List<Double> zeroVector = new ArrayList<>(Collections.nCopies(edgeFeatureSize, 0.0));
                        edgeAttr.add(zeroVector);
                    }
                }
            }
        }

        // Build edges for Structure
        for (int i = 0; i < design.getCompIDs().size() - 1; i++) {

            // Update Edge Index - bidirectional edges for structure
            src.add(i);
            dst.add(i + 1);
            src.add(i + 1);
            dst.add(i);

            // Update Edge Labels
            edgeLabels.add(0); // 0 for structure
            edgeLabels.add(0); // 0 for structure

            // Update Edge Attributes
            if (edgeFeatureSize != null) {
                List<Double> zeroVector = new ArrayList<>(Collections.nCopies(edgeFeatureSize, 0.0));
                edgeAttr.add(zeroVector);
                edgeAttr.add(zeroVector);
            }

        }

        List<List<Integer>> edgeIndex = new ArrayList<>();
        edgeIndex.add(src);
        edgeIndex.add(dst);

        this.edgeDim = edgeAttr.isEmpty() ? 0 : edgeAttr.get(0).size();

        if (edgeAttr.size() != edgeIndex.get(0).size() || edgeLabels.size() != edgeIndex.get(0).size()) {
            throw new IllegalStateException("Edge attributes, labels, and index sizes do not match.");
        }

        return edgeIndex;
    }

    private String getDesignSequence(DesignSpaceLinearDAGRepresentation design) {
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

    private List<String> getNodeSequences(DesignSpaceLinearDAGRepresentation design) {
        List<String> nodeSequences = new ArrayList<>();

        for (String compID : design.getCompIDs()) {
            if (compIDMap.containsKey(compID)) {
                String seq = compIDMap.get(compID).getSequence();
                
                if (seq == null || seq.equals("missing")) {
                    return null;
                }

                nodeSequences.add(seq);

            } else {
                return null;
            }
        }

        return nodeSequences;
    }

    private List<Double> getDesignFeatures(DesignSpaceLinearDAGRepresentation design) {
        List<Double> features = new ArrayList<>();

        // TODO: Add design-level features here, if any. For now, we will just return an empty list.

        this.featureDim = features.size();

        return features;
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
        if (test.size() > 0) split.put("test", test);

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

    public int getFeatureDim() {
        System.out.println("Feature Dim: " + featureDim);
        return featureDim;
    }

    public int getNodeDim() {
        System.out.println("Node Dim: " + nodeDim);
        return nodeDim;
    }

    public int getEdgeDim() {
        System.out.println("Edge Dim: " + edgeDim);
        return edgeDim;
    }

}
