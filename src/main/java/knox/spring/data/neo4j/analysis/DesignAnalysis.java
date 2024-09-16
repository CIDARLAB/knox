package knox.spring.data.neo4j.analysis;

import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.sample.DesignSampler;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public Map<String, Map<String, Double>> partAnalytics() {
		Map<String, Map<String, Double>> partAnalytics = new HashMap<String, Map<String, Double>>();

		for (Edge e : this.space.getEdges()) {

			int i = 0;
			for (String compID : e.getComponentIDs()) {
				Map<String, Double> data = new HashMap<String,Double>();
				
				if (!partAnalytics.containsKey(compID)) {
					data.put("frequency", 1.0);
					data.put("totalScore", e.getWeight().get(i));
					data.put("averageScore", e.getWeight().get(i));
					data.put("lowScore", e.getWeight().get(i));
					data.put("highScore", e.getWeight().get(i));
					partAnalytics.put(compID, data);
				
				} else {

					// update frequency
					data.put("frequency", partAnalytics.get(compID).get("frequency") + 1.0);

					// update totalScore
					data.put("totalScore", partAnalytics.get(compID).get("totalScore") + e.getWeight().get(i));

					// update averageScore
					data.put("averageScore", partAnalytics.get(compID).get("totalScore") / partAnalytics.get(compID).get("frequency"));

					// update lowScore
					if (e.getWeight().get(i) < partAnalytics.get(compID).get("lowScore")) {
						data.put("lowScore", e.getWeight().get(i));
					} else {
						data.put("lowScore", partAnalytics.get(compID).get("lowScore"));
					}

					// update highScore
					if (e.getWeight().get(i) > partAnalytics.get(compID).get("highScore")) {
						data.put("highScore", e.getWeight().get(i));
					} else {
						data.put("highScore", partAnalytics.get(compID).get("highScore"));
					}

					partAnalytics.replace(compID, data);
				}

				i++;
			}
		}

		return partAnalytics;
	}
}
