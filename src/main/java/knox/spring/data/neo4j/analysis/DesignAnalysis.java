package knox.spring.data.neo4j.analysis;

import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.sample.DesignSampler;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.util.*;

public class DesignAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(DesignAnalysis.class);

    private NodeSpace space;

    private double bestPathScore;

    public DesignAnalysis(NodeSpace space) {
        this.space = space;

        this.bestPathScore = 0;
    }

    public List<List<Map<String, Object>>> getBestPath() {		
		List<List<Map<String, Object>>> bestPaths = new LinkedList<List<Map<String, Object>>>();		
		List<Map<String, Object>> bestPath = new ArrayList<Map<String, Object>>();
		
		Node startNode = this.space.getStartNode();
		Set<Node> acceptNodes = this.space.getAcceptNodes();
		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = this.space.mapNodeIDsToIncomingEdges();
		this.space.clearNodeWeights();

		double startNodeWeight = 0.0;
		startNode.setWeight(startNodeWeight);

		// Check nodes have edges
		if (startNode.getEdges() == null) {
			return bestPaths;
		}

		// Update each Node's weight
		double bestPathScore = 0.0;
		for (Node node : acceptNodes) {
			getBestNodeWeight_TotalScore();

			bestPathScore = node.getWeight();
		}
		this.bestPathScore = bestPathScore;

		
		List<Node> sortedBestNodes = sortBestNodes();
		

		// Find Edge to each Node
		for (Node node: sortedBestNodes) {
			Edge bestEdge = getBestEdge(node, nodeIDToIncomingEdges);
			List<String> componentIDs = bestEdge.getComponentIDs();
			
			// Select Component ID with highest Weight
			if (!bestEdge.isBlank()) {
				String compID = bestEdge.getComponentIDs().get(bestEdge.getWeight().indexOf(bestEdge.getMaxWeight()));

				Map<String, Object> comp = new HashMap<String, Object>();

				comp.put("id", compID);
				comp.put("pathScore", bestPathScore);

				bestPath.add(comp);
			}
		}

		bestPaths.add(bestPath);

		return bestPaths;
    }
    public void getBestNodeWeight_TotalScore() {
		List<Node> topologicalSort = topologicalSort(this.space);

		for (Node node: topologicalSort) {

			Set<Edge> edges = node.getEdges();
			for (Edge edge: edges) {
				if (edge.getTailID() == node.getNodeID()) {
					
					if (edge.isBlank()) {
						if (edge.getHead().hasWeight()) {
							if (edge.getTail().getWeight() > edge.getHead().getWeight()) {
								edge.getHead().setWeight(edge.getTail().getWeight());
							}
						} else {
							edge.getHead().setWeight(edge.getTail().getWeight());
						}


					} else {
						if (edge.getHead().hasWeight()) {
							if ((edge.getTail().getWeight() + edge.getMaxWeight()) > edge.getHead().getWeight()) {
								edge.getHead().setWeight(edge.getTail().getWeight() + edge.getMaxWeight());
							}
						} else {
							edge.getHead().setWeight(edge.getTail().getWeight() + edge.getMaxWeight());
						}
					}

					if (!this.space.getNodes().contains(edge.getHead())) {
						this.space.getNodes().add(edge.getHead());
					}
				}
			}

			
		}
	}

    public List<Node> topologicalSort(NodeSpace nodeSpace) {
		int numNodes = nodeSpace.getNumNodes();
		Node startNode = nodeSpace.getStartNode();

		List<Node> ordering = new ArrayList<Node>();
		List<Node> queue = new ArrayList<Node>();

		queue.add(startNode);

		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = nodeSpace.mapNodeIDsToIncomingEdges();
		Map<String, Integer> nodeDegrees = new HashMap<String, Integer>();
		for (Node node: nodeSpace.getNodes()) {
			nodeDegrees.put(node.getNodeID(), node.getIncomingEdges(nodeIDToIncomingEdges).size());
		}

		while (ordering.size() < numNodes) {
			Node node = queue.get(0);
			ordering.add(node);
			queue.remove(0);

			for (Edge edge: node.getEdges()) {
				if (edge.getTailID() == node.getNodeID()) {
					nodeDegrees.merge(edge.getHeadID(), -1, Integer::sum);

					if (nodeDegrees.get(edge.getHeadID()) == 0) {
						queue.add(edge.getHead());
					}

				}
			}
		}

		return ordering;
	}

    public List<Node> sortBestNodes() {
		List<Node> sortedNodes = new ArrayList<Node>();
		
		Set<Node> acceptNodes = this.space.getAcceptNodes();

		Node node = acceptNodes.iterator().next();

		while(!node.isStartNode()) {
			sortedNodes.add(0, node);

			node = prevBestNode(node);
		}

		return sortedNodes;
	}

    public Node prevBestNode(Node node) {
		Node prevBestNode = new Node();
		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = this.space.mapNodeIDsToIncomingEdges();

		Set<Edge> edges = node.getIncomingEdges(nodeIDToIncomingEdges);

		for (Edge edge: edges) {
			if (edge.getHeadID() == node.getNodeID()) {

				if (edge.isBlank()) {
					if (edge.getTail().getWeight() == node.getWeight()) {
						prevBestNode = edge.getTail();
					}
				} else {
					if (edge.getTail().getWeight() + edge.getMaxWeight() == node.getWeight()) {
						prevBestNode = edge.getTail();
					}
				}
				
			}
		}

		return prevBestNode;
		
	}

    public Edge getBestEdge(Node node, HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
		Set<Edge> incomingEdges = node.getIncomingEdges(nodeIDToIncomingEdges);
		Edge bestEdge = new Edge();

		for (Edge edge: incomingEdges) {
			if (edge.isBlank()) {
				if (edge.getTail().getWeight() == node.getWeight()) {
					bestEdge = edge;
					break;
				}
			} else {
				if (edge.getTail().getWeight() + edge.getMaxWeight() == node.getWeight()) {
					bestEdge = edge;
					break;
				}
			}
		}

		return bestEdge;
	}

    public double getBestPathScore() {
		getBestPath();

		return this.bestPathScore;
	}

	public Map<String, Map<String, Object>> partAnalytics() {
		Map<String, Map<String, Object>> partAnalytics = new HashMap<String, Map<String, Object>>();

		Set<String> nextCompIDsSeen = new HashSet<>();

		for (Edge thisEdge : this.space.getEdges()) {

			int i = 0;
			for (String compID : thisEdge.getComponentIDs()) {
				Map<String, Object> data = new HashMap<String,Object>();
				
				if (!partAnalytics.containsKey(compID)) {
					data.put("frequency", 1.0);
					data.put("totalScore", thisEdge.getWeight().get(i));
					data.put("averageScore", thisEdge.getWeight().get(i));
					data.put("lowScore", thisEdge.getWeight().get(i));
					data.put("highScore", thisEdge.getWeight().get(i));
					data.put("type", thisEdge.getComponentRoles().get(i));

					List<Double> weights = new ArrayList<Double>();
					weights.add(thisEdge.getWeight().get(i));
					data.put("weights", weights);
					
					// Unique Weights Count
					data.put("UniqueWeightCount", 1);

					// Next Part Data
					Set<String> nextPartIDs = new HashSet<String>();
					for (Edge nextEdge : thisEdge.getHead().getEdges()) {
						nextPartIDs.addAll(nextEdge.getComponentIDs());
					}

					for (String nextPartID : nextPartIDs) {
						List<Double> nextWeights = new ArrayList<Double>();
						nextWeights.add(thisEdge.getWeight().get(i));
						data.put(nextPartID, nextWeights);
					}

					nextCompIDsSeen.addAll(nextPartIDs);

					partAnalytics.put(compID, data);
				
				} else {

					// update frequency
					data.put("frequency", Double.valueOf((Double) partAnalytics.get(compID).get("frequency")) + 1.0);

					// update totalScore
					data.put("totalScore", Double.valueOf((Double) partAnalytics.get(compID).get("totalScore")) + thisEdge.getWeight().get(i));

					// update averageScore
					data.put("averageScore", Double.valueOf((Double) partAnalytics.get(compID).get("totalScore")) / Double.valueOf((Double) partAnalytics.get(compID).get("frequency")));

					// update lowScore
					if (thisEdge.getWeight().get(i) < Double.valueOf((Double) partAnalytics.get(compID).get("lowScore"))) {
						data.put("lowScore", thisEdge.getWeight().get(i));
					} else {
						data.put("lowScore", partAnalytics.get(compID).get("lowScore"));
					}

					// update highScore
					if (thisEdge.getWeight().get(i) > Double.valueOf((Double) partAnalytics.get(compID).get("highScore"))) {
						data.put("highScore", thisEdge.getWeight().get(i));
					} else {
						data.put("highScore", partAnalytics.get(compID).get("highScore"));
					}

					// update part type
					data.put("type", thisEdge.getComponentRoles().get(i));

					// update weights
					List<Double> weights = (List<Double>) partAnalytics.get(compID).get("weights");
					weights.add(thisEdge.getWeight().get(i));
					data.put("weights", weights);

					// update unique weights count
					List<Double> uniqueWeights = new ArrayList<>();
					for (Double weight : weights) {
						if (!uniqueWeights.contains(weight)) {
							uniqueWeights.add(weight);
						}
					}
					data.put("uniqueWeightCount", uniqueWeights.size());

					// update Next Part Data
					Set<String> nextPartIDs = new HashSet<String>();
					for (Edge nextEdge : thisEdge.getHead().getEdges()) {
						nextPartIDs.addAll(nextEdge.getComponentIDs());
					}

					for (String nextPartID : nextPartIDs) {

						if (!(partAnalytics.get(compID).containsKey(nextPartID))){
							List<Double> nextPartWeights = new ArrayList<>();
							nextPartWeights.add(thisEdge.getWeight().get(i));
							data.put(nextPartID, nextPartWeights);
						} else {
							List<Double> nextPartWeights = (List<Double>) partAnalytics.get(compID).get(nextPartID);
							nextPartWeights.add(thisEdge.getWeight().get(i));
							data.put(nextPartID, nextPartWeights);
						}
					}

					// Include Next Part Data of already seen parts
					for (String compIDSeen : nextCompIDsSeen) {
						if ((partAnalytics.get(compID).containsKey(compIDSeen)) && (!(nextPartIDs.contains(compIDSeen)))){
							
							List<Double> compPartWeights = (List<Double>) partAnalytics.get(compID).get(compIDSeen);
							data.put(compIDSeen, compPartWeights);

						} 
					}

					nextCompIDsSeen.addAll(nextPartIDs);

					// update partAnalytics
					partAnalytics.replace(compID, data);
				}

				i++;
			}
		}

		return partAnalytics;
	}

	public double pairwiseAccuracy(double[] predicted, double[] actual) {
		int correctPairs = 0;
		int totalPairs = 0;

		for (int i = 0; i < predicted.length; i++) {
			for (int j = i + 1; j < predicted.length; j++) {
				if ((predicted[i] > predicted[j] && actual[i] > actual[j]) || 
					(predicted[i] < predicted[j] && actual[i] < actual[j])) {
					correctPairs++;
				}
				totalPairs++;
			}
		}
		return (double) correctPairs / totalPairs;
	}

	public double precisionAtK(ArrayList<String> predicted, ArrayList<String> actual, double k, Boolean isTop) {
		int groupSize = predicted.size();
		ArrayList<String> kPredicted = new ArrayList<>();
		ArrayList<String> kActual = new ArrayList<>();

		// Split Predicted and Actual to Top or Bottom 1/k
		if (isTop) {
			int index = (int) Math.ceil(groupSize / k);
			kPredicted = new ArrayList<>(predicted.subList(groupSize - index, groupSize));
			kActual = new ArrayList<>(actual.subList(groupSize - index, groupSize));
		} else {
			int index = (int) Math.ceil(groupSize / k);
			kPredicted = new ArrayList<>(predicted.subList(0, index));
			kActual = new ArrayList<>(actual.subList(0, index));
		}

		// Calculate Precision at K
		int trueCount = 0;
		int totalCount = 0;
		for (String id : kPredicted) {
			if (kActual.contains(id)) trueCount++;
			totalCount++;
		}

		return (double) trueCount / totalCount;
	}

	public double kendallTau(double[] predicted, double[] actual) {
		KendallsCorrelation kendall = new KendallsCorrelation();
        double tau = kendall.correlation(predicted, actual);
		return tau;
	}

	public double spearmansCorrelation(double[] predicted, double[] actual) {
		SpearmansCorrelation spearman = new SpearmansCorrelation();
        double rho = spearman.correlation(predicted, actual);
		return rho;
	}

}
