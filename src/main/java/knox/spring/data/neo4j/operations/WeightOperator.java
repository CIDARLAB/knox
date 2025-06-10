package knox.spring.data.neo4j.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import knox.spring.data.neo4j.analysis.DesignAnalysis;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.NodeSpace;

public class WeightOperator {
    
    public static void apply (NodeSpace weightedSpace, NodeSpace spaceToWeight, NodeSpace outputSpace, int tolerance, int weightTolerance) {

        System.out.println("\nStarting Weight Operation");
        System.out.println("tolerance: " + String.valueOf(tolerance));
		System.out.println("weight tolerance: " + String.valueOf(weightTolerance));

        DesignAnalysis designAnalysis = new DesignAnalysis(weightedSpace);
        Map<String, Map<String, Object>> partAnalytics = designAnalysis.partAnalytics();

        System.out.println("/nPart Analytics:");
        System.out.println(partAnalytics);
        System.out.println();

        for (Edge thisEdge : spaceToWeight.getEdges()) {

            int i = 0;
            ArrayList<Double> newEdgeWeights = new ArrayList<Double>();
            for (String compID : thisEdge.getComponentIDs()) {
                
                if (partAnalytics.containsKey(compID)) {
                    
                    // Get next PartIDs
                    Set<String> nextCompIDs = new HashSet<String>();
                    for (Edge nextEdge : thisEdge.getHead().getEdges()) {
						nextCompIDs.addAll(nextEdge.getComponentIDs());
					}

                    Boolean nextCompIDPresent = false;
                    for (String nextCompID : nextCompIDs) {
                        // Next Comp ID Present
                        if (partAnalytics.get(compID).containsKey(nextCompID)) {
                            int count = 0;
                            Double weightTotal = 0.0;
                            for (Double weight : (List<Double>) partAnalytics.get(compID).get(nextCompID)) {
                                weightTotal = weightTotal + weight;
                                count++;
                            }
                            weightTotal = weightTotal / count;
                            newEdgeWeights.add(weightTotal);
                            nextCompIDPresent = true;
                            break;
                        }
                    }

                    if (!nextCompIDPresent) {
                        newEdgeWeights.add((Double) partAnalytics.get(compID).get("averageScore"));
                    }

                } else {
                    newEdgeWeights.add(thisEdge.getWeight().get(i));
                }

                thisEdge.setWeight(newEdgeWeights);
                i++;
            }
        }

        outputSpace.copyNodeSpace(spaceToWeight);

        //outputSpace.shallowCopyNodeSpace(spaceToWeight);
    }
}
