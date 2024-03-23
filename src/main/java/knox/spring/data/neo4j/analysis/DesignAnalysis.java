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
			
			
			for (String compID: componentIDs) {
				Map<String, Object> comp = new HashMap<String, Object>();

				comp.put("id", compID);

				bestPath.add(comp);
			}
		}

		bestPaths.add(bestPath);

		return bestPaths;
    }
    public void getBestNodeWeight_TotalScore() {
		List<Node> topologicalSort = topologicalSort();

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
							if ((edge.getTail().getWeight() + edge.getWeight()) > edge.getHead().getWeight()) {
								edge.getHead().setWeight(edge.getTail().getWeight() + edge.getWeight());
							}
						} else {
							edge.getHead().setWeight(edge.getTail().getWeight() + edge.getWeight());
						}
					}

					if (!this.space.getNodes().contains(edge.getHead())) {
						this.space.getNodes().add(edge.getHead());
					}
				}
			}

			
		}
	}

    public List<Node> topologicalSort() {
		int numNodes = this.space.getNumNodes();
		Node startNode = this.space.getStartNode();

		List<Node> ordering = new ArrayList<Node>();
		List<Node> queue = new ArrayList<Node>();

		queue.add(startNode);

		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = this.space.mapNodeIDsToIncomingEdges();
		Map<String, Integer> nodeDegrees = new HashMap<String, Integer>();
		for (Node node: this.space.getNodes()) {
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
					if (edge.getTail().getWeight() + edge.getWeight() == node.getWeight()) {
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
				if (edge.getTail().getWeight() + edge.getWeight() == node.getWeight()) {
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
}
