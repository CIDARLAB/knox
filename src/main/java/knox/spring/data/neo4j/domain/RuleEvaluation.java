package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.annotation.Transient;
import knox.spring.data.neo4j.operations.ANDOperator;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@org.springframework.data.neo4j.core.schema.Node
public class RuleEvaluation {
    @Id
    @GeneratedValue
    private Long id;

    @Property
    private String evaluationName;

    @Property
    private ArrayList<String> ruleSpaceIDs;

    @Property
    private ArrayList<String> designSpaceIDs;

    @Property
    private ArrayList<Integer> designLabels;

    @Property
    private ArrayList<Double> designScores;

    @Property
    private ArrayList<Integer> flattenedRuleEvaluations;

    @Property
    private String labelingMethod;

    @Transient
    private ArrayList<NodeSpace> ruleSpaces;

    @Transient
    private ArrayList<NodeSpace> designSpaces;

    public RuleEvaluation() {}

    public RuleEvaluation(String evaluationName, ArrayList<String> ruleSpaceIDs, ArrayList<String> designSpaceIDs, ArrayList<Integer> designLabels, ArrayList<Double> designScores, 
            ArrayList<Integer> flattenedRuleEvaluations, String labelingMethod) {
        
        this.evaluationName = evaluationName;
        this.ruleSpaceIDs = ruleSpaceIDs;
        this.designSpaceIDs = designSpaceIDs;
        this.designLabels = designLabels;
        this.designScores = designScores;
        this.flattenedRuleEvaluations = flattenedRuleEvaluations;
        this.labelingMethod = labelingMethod;
    }

    public RuleEvaluation(String evaluationName, ArrayList<String> ruleSpaceIDs, ArrayList<String> designSpaceIDs, ArrayList<Integer> designLabels, ArrayList<Double> designScores,
            ArrayList<NodeSpace> ruleSpaces, ArrayList<NodeSpace> designSpaces, String labelingMethod) {

        this.evaluationName = evaluationName;
        this.ruleSpaceIDs = ruleSpaceIDs;
        this.designSpaceIDs = designSpaceIDs;
        this.ruleSpaces = ruleSpaces;
        this.designSpaces = designSpaces;
        this.designLabels = designLabels;
        this.designScores = designScores;
        this.flattenedRuleEvaluations = new ArrayList<>();
        this.labelingMethod = labelingMethod;

        if (this.designLabels.isEmpty() && !this.designScores.isEmpty()) {
            if (labelingMethod.equals("median")) {
                populateLabelsByMedian();
            } else if (labelingMethod.equals("sign")) {
                populateLabelsBySign();
            }
        }

        if (!this.designLabels.isEmpty()) {
            sortDesignSpacesByLabels();
        }
    }

    public Map<String, Map<String, Object>> getEvaluationResults() {
        System.out.println("Getting Evaluation Results...");
        if (flattenedRuleEvaluations.isEmpty()) {
            runEvaluation();
        }
        return evaluateRuleSpace();
    }

    private void runEvaluation() {
        System.out.println("Running Rule Evaluation...");
        int ruleIndex = 0;
        for (NodeSpace ruleSpace : ruleSpaces) {
            ArrayList<Integer> designResults = new ArrayList<>();

            for (NodeSpace designSpace : designSpaces) {
                NodeSpace outSpace = new NodeSpace();
                ArrayList<NodeSpace> inputSpaces = new ArrayList<>();
                inputSpaces.add(designSpace);
                inputSpaces.add(ruleSpace);
                ANDOperator.apply(inputSpaces, outSpace, 1, true, new HashSet<String>(), new ArrayList<String>());
                
                if (outSpace.isEmpty()) {
                    designResults.add(1); // 1 indicates eliminated
                } else {
                    designResults.add(0); // 0 indicates not eliminated (kept)
                }
            }

            flattenedRuleEvaluations.addAll(designResults);

            System.out.println("Rule Space ID: " + ruleSpaceIDs.get(ruleIndex));
            System.out.println("\n--------------------------------------------------\n");
            ruleIndex++;
        }
    }

    private Map<String, Map<String, Object>> evaluateRuleSpace() {
        Map<String, Map<String, Object>> ruleResults = new HashMap<>();
        Map<String, Map<String, Object>> evaluationResults = new HashMap<>();
        Map<String, ArrayList<Object>> designToRule = new HashMap<>();
        ArrayList<ArrayList<Integer>> ruleEvaluations = chunkRuleEvaluations();

        if (!designLabels.isEmpty()) {
            designToRule.put("labels", designLabels.stream().map(label -> (Object) label).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        }

        if (!designScores.isEmpty()) {
            designToRule.put("scores", designScores.stream().map(score -> (Object) score).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        }

        designToRule.put("designIDs", designSpaceIDs.stream().map(id -> (Object) id).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        
        if (ruleEvaluations.isEmpty()) {
            return ruleResults;
        }

        int ruleIndex = 0;
        for (String ruleSpaceID : ruleSpaceIDs) {
            Map<String, Object> metrics = new HashMap<String, Object>();
            if (designLabels.isEmpty()) {
                // No labels to evaluate against
                evaluationResults.put(ruleSpaceID, metrics);
                designToRule.put(ruleSpaceID, ruleEvaluations.get(ruleIndex).stream().map(result -> (Object) result).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
                ruleIndex++;
                continue;
            }
            Integer totalGoodDesigns = designLabels.stream().filter(label -> label == 1).toArray().length;
            Integer totalPoorDesigns = designLabels.stream().filter(label -> label == 0).toArray().length;
            Integer totalDesignsElim = ruleEvaluations.get(ruleIndex).stream().filter(result -> result == 1).toArray().length;

            // Calculate confusion matrix
            int poorDesignIndex = ruleEvaluations.get(ruleIndex).size() - totalPoorDesigns;
            Integer goodDesignsElim = 0;
            Integer poorDesignsElim = 0;
            Integer goodRemaining = 0;
            Integer poorRemaining = 0;
            for (int i = 0; i < ruleEvaluations.get(ruleIndex).size(); i++) {

                if (i < poorDesignIndex) {
                    // Good Design
                    if (ruleEvaluations.get(ruleIndex).get(i) == 1) {
                        goodDesignsElim += 1;
                    } else {
                        goodRemaining += 1;
                    }
                } else {
                    // Poor Design
                    if (ruleEvaluations.get(ruleIndex).get(i) == 1) {
                        poorDesignsElim += 1;
                    } else {
                        poorRemaining += 1;
                    }
                }
            }

            // Populate metrics
            Integer numCorrect = goodRemaining + poorDesignsElim;
            Integer numIncorrect = poorRemaining + goodDesignsElim;
            metrics.put("numCorrect", numCorrect);
            metrics.put("numIncorrect", numIncorrect);
            metrics.put("goodDesignsElim", goodDesignsElim);
            metrics.put("poorDesignsElim", poorDesignsElim);
            metrics.put("scorePercent", 100 * (numCorrect - numIncorrect)/(totalGoodDesigns + totalPoorDesigns));
            metrics.put("goodnessPercent", 100 * goodRemaining / totalGoodDesigns);
            metrics.put("poornessPercent", 100 * poorRemaining / totalPoorDesigns);

            if ((poorDesignsElim + goodDesignsElim) == 0) {
                metrics.put("poorEliminationPercent", null);
            } else {
                metrics.put("poorEliminationPercent", 100 * poorDesignsElim / totalDesignsElim);
            }

            if (goodDesignsElim == 0 && poorDesignsElim > 0) {
                metrics.put("goodPerfection", true);
            } else {
                metrics.put("goodPerfection", false);
            }

            if (poorDesignsElim == 0 && goodDesignsElim > 0) {
                metrics.put("poorPerfection", true);
            } else {
                metrics.put("poorPerfection", false);
            }

            if (goodDesignsElim == 0 && poorDesignsElim == 0) {
                metrics.put("totalPerfection", true);
            } else {
                metrics.put("totalPerfection", false);
            }

            // Store results
            evaluationResults.put(ruleSpaceID, metrics);
            designToRule.put(ruleSpaceID, ruleEvaluations.get(ruleIndex).stream().map(result -> (Object) result).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
            ruleIndex++;
        }

        ruleResults.put("designToRule", new HashMap<>(designToRule));
		ruleResults.put("evaluationResults", new HashMap<>(evaluationResults));

		return ruleResults;
	}

    private ArrayList<ArrayList<Integer>> chunkRuleEvaluations() {
        ArrayList<ArrayList<Integer>> chunks = new ArrayList<>();
        int totalSize = flattenedRuleEvaluations.size();
        int chunkSize = designLabels.size();
        for (int i = 0; i < totalSize; i += chunkSize) {
            ArrayList<Integer> chunk = new ArrayList<>(flattenedRuleEvaluations.subList(i, Math.min(totalSize, i + chunkSize)));
            chunks.add(chunk);
        }
        return chunks;
    }

    public double[][] getFeatures() {
        ArrayList<ArrayList<Integer>> ruleEvaluations = chunkRuleEvaluations();
        return transpose(ruleEvaluations.stream().map(list -> list.stream().mapToDouble(Integer::doubleValue).toArray()).toArray(double[][]::new));
    }

    public static double[][] transpose(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        double[][] transposed = new double[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }

        return transposed;
    }

    public void sortDesignSpacesByLabels() {
        System.out.println("Sorting Design Spaces by Labels...");
        ArrayList<NodeSpace> sortedDesignSpaces = new ArrayList<>();
        ArrayList<String> sortedDesignSpaceIDs = new ArrayList<>();
        ArrayList<Integer> sortedLabels = new ArrayList<>();
        ArrayList<Double> sortedScores = new ArrayList<>();

        ArrayList<Integer> goodIndices = new ArrayList<>();
        ArrayList<Integer> poorIndices = new ArrayList<>();

        for (int i = 0; i < designLabels.size(); i++) {
            if (designLabels.get(i).equals(1)) {
                goodIndices.add(i);
            } else {
                poorIndices.add(i);
            }
        }

        for (int index : goodIndices) {
            sortedDesignSpaces.add(designSpaces.get(index));
            sortedDesignSpaceIDs.add(designSpaceIDs.get(index));
            sortedLabels.add(designLabels.get(index));
            sortedScores.add(designScores.get(index));
        }

        for (int index : poorIndices) {
            sortedDesignSpaces.add(designSpaces.get(index));
            sortedDesignSpaceIDs.add(designSpaceIDs.get(index));
            sortedLabels.add(designLabels.get(index));
            sortedScores.add(designScores.get(index));
        }

        this.designSpaces = sortedDesignSpaces;
        this.designSpaceIDs = sortedDesignSpaceIDs;
        this.designLabels = sortedLabels;
        this.designScores = sortedScores;
        System.out.println("Design Spaces Sorted.");
    }

    public void populateLabelsBySign() {
        this.designLabels = new ArrayList<>();
        for (Double score : designScores) {
            if (score > 0) {
                this.designLabels.add(1);
            } else {
                this.designLabels.add(0);
            }
        }
    }

    public void populateLabelsByMedian() {
        this.designLabels = new ArrayList<>();
        double medianScore = median(designScores);
        
        for (Double score : designScores) {
            if (score >= medianScore) {
                this.designLabels.add(1);
            } else {
                this.designLabels.add(0);
            }
        }
    }

    public static double median(List<Double> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List is empty");
        }

        List<Double> sorted = new ArrayList<>(list);
        Collections.sort(sorted);

        int n = sorted.size();
        int mid = n / 2;

        if (n % 2 == 1) {
            return sorted.get(mid);                 // odd size
        } else {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;  // even size
        }
    }

    public double[] getLabels() {
        return designLabels.stream().mapToDouble(Integer::doubleValue).toArray();
    }

    public double[] getScores() {
        return designScores.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public ArrayList<String> getRuleSpaceIDs() {
        return ruleSpaceIDs;
    }

    public String getEvaluationName() {
        return evaluationName;
    }

    public void setEvaluationName(String evaluationName) {
        this.evaluationName = evaluationName;
    }

}
