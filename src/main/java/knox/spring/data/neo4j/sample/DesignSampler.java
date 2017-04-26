package knox.spring.data.neo4j.sample;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.EnumerateType;
import knox.spring.data.neo4j.domain.Node;

import org.sbolstandard.core2.Collection;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.SBOLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

public class DesignSampler {
	private static final Logger LOG = LoggerFactory.getLogger(DesignSampler.class);
	private DesignSpace space;
	private List<Node> starts;
	
	public DesignSampler(DesignSpace space) {
		this.space = space;
		starts = new LinkedList<>(space.getStartNodes());
	}
	
	public Set<List<String>> sample(int numSamples) {

		Set<List<String>> samples = new HashSet<List<String>>();
		Random rand = new Random();

		for (int i = 0; i < numSamples; i++) {

			List<String> sample = new LinkedList<String>();
			Node node = starts.get(rand.nextInt(starts.size()));
            Edge edge = new Edge();

            while (node.hasEdges() && (!node.isAcceptNode() || rand.nextInt(2) == 1)) {
				Iterator<Edge> edgerator = node.getEdges().iterator();

				// Get the number of edges w no probability
				int numZero = 0; 
				for (Edge e : node.getEdges()) {
					if (e.getProbability() == 0.0)
						numZero += 1;
				}

				// Add up the total weights
				double totalWeights = 0.0;
				for (Edge e: node.getEdges()) {
					if (e.getProbability() == 0.0)
						e.setProbability(1.0/numZero);

					totalWeights += e.getProbability();
				}

				// Choose edge based on weight
				double rWeight = rand.nextDouble() * totalWeights;

				double countWeights = 0.0;
				for (Edge e: node.getEdges()) {
					countWeights += e.getProbability();
					if (countWeights >= rWeight) {
						edge = e;
						break;
					}
				}

				
				if (edge.hasComponentRoles()) {
					sample.add(edge.getComponentRoles().get(rand.nextInt(edge.getComponentRoles().size())));
				}
				
				node = edge.getHead();
			}
			
			samples.add(sample);
		}
		
		return samples;
	}

	/*
		This method will enumerate the graph: providing all possible paths from the start of the graph
		to the end of the graph.

		Arguments:
			- enumerateType [EnumerateType]: Either will perform a breadth-first-depth or a depth-first-search
			- requestedDesigns [int]: The number of requested designs. 5 is the default.

		Returns:
			- Set<List<String>>: The paths that are generated. Each List<String> represents an ordering of
								 the specific component ids.
	 */
	public Set<List<String>> enumerate(EnumerateType enumerateType, int requestedDesigns) {
		if (enumerateType == EnumerateType.BFS) {
			return bfsEnumerate(requestedDesigns);
		} else {
			return dfsEnumerate(requestedDesigns);
		}
	}

	/*
		This method will enumerate the graph using a DFS method. If the number of designs requested is
		less than the total possible number of designs, it will only return the first x designs requested,
		where x is the number of requested designs. If this is the case, the designs generated each time
		will be different as the edges are represented as a set, thereby giving a random ordering
		each time they are looped over.

		Arguments:
			- requestedDesigns [int]: The number of requested designs. 5 is the default.

		Returns:
			- Set<List<String>>: The paths that are generated. Each List<String> represents an ordering of
								 the specific component ids.

		Notes:
			- This algorithm is based on a permutation algorithm (obviously we have some differences
			  due to adjacency limitations). Therefore, if we can assume our adjacency matrix is
			  not not perfect (meaning that every node is connected to every other node), then our runtime
			  is limited by n!. However, because each edge can have numerous component IDs, this will
			  actually increase the runtime of our algorithm. Because this is a recursive algorithm, we
			  also need to account for the callstack for every time the dfsEnumerateRecursive is
			  called on itself. Presumably, this would simply by O(N) in additional runtime for each path.

			  This algorithm is of course of high complexity, but it is worth noting that this is NP-hard,
			  and therefore, it should suffice as generally there are no efficient solutions available
			  without a heuristic.

		Memory:
			- The memory for this algorithm is similarly high. Luckily, Java garbage collection will remove
			  unnecessary sets and lists that are created simply for aid of the final algorithm. On each iteration
			  of the dfsEnumerateRecursive, we must create a new Set for each iteration to ensure that each
			  copy represents specific information for a single n-1 permutation.

	 */
	private Set<List<String>> dfsEnumerate(int numberOfDesigns) {
		Set<List<String>> allDesigns = new HashSet<>();
		int currentNumberOfDesigns = 0;

		for (Node start : starts) {
			Set<List<String>> designs = new HashSet<>();
			designs.add(new ArrayList<>());
			Set<List<String>> generatedDesigns = dfsEnumerateRecursive(start, designs);
			LOG.warn("generated designs size {}", generatedDesigns.size());

			if (generatedDesigns.size() + currentNumberOfDesigns < numberOfDesigns) {
				allDesigns.addAll(generatedDesigns);
				currentNumberOfDesigns += generatedDesigns.size();
			} else {
				int neededDesigns = numberOfDesigns - currentNumberOfDesigns;
				Iterator<List<String>> generatedDesignsIterator = generatedDesigns.iterator();
				for (int i = 0; i < neededDesigns; i++) {
					allDesigns.add(generatedDesignsIterator.next());
				}

				return allDesigns;
			}

		}

		return allDesigns;
	}

	/*
		This method is a helper method for a DFS algorithm. It is recursive and will call
		itself repeatedly until a node is either at the end (being an accept node), or a node has no
		outgoing edges. This algorithm follows a tail recursion approach.

		Arguments:
			- node [Node] The current node.
			- designs [Set<List<String>>]: All of the designs that have been generated up until the current node.

		Returns:
			- Set<List<String>>: The paths that are generated at a single from a single node to the end.

	*/
	private Set<List<String>> dfsEnumerateRecursive(Node node, Set<List<String>> designs) {
		if (!node.hasEdges() || node.isAcceptNode()) {
			LOG.warn("node done {}", node.getNodeID());
			return designs;
		}

		Set<List<String>> allVisitedDesigns = new HashSet<>();
		LOG.warn("node id {}", node.getNodeID());

		for (Edge edge : node.getEdges()) {
			Set<List<String>> visitedDesigns = new HashSet<>();

			for (String componentRole : edge.getComponentRoles()) {
				LOG.warn("component role {}", componentRole);

				for (List<String> design : designs) {
					List<String> copiedDesign = new ArrayList<>(design);
					copiedDesign.add(componentRole);
					visitedDesigns.add(copiedDesign);
				}
			}

			allVisitedDesigns.addAll(dfsEnumerateRecursive(edge.getHead(), visitedDesigns));
			LOG.warn("visited designs size {}", allVisitedDesigns.size());
		}

		return allVisitedDesigns;
	}


	private Set<List<String>> bfsEnumerate(int numberOfDesigns) {
		Set<List<String>> allDesigns = new HashSet<>();
		int currentNumberOfDesigns = 0;
		
		for (Node start : starts) {
			Set<List<String>> designs = new HashSet<>();
			Stack<Edge> edgeStack = new Stack<>();
			Stack<Set<List<String>>> designStack = new Stack<>();
			
			if (start.hasEdges()) {
				for (Edge edge : start.getEdges()) {
					edgeStack.push(edge);
				}
			}
			
			for (int i = 0; i < start.getNumEdges() - 1; i++) {
				designStack.push(designs);
			}
			
			while (!edgeStack.isEmpty()) {
				Edge edge = edgeStack.pop();
				if (edge.hasComponentRoles()) {
					Set<List<String>> comboDesigns = new HashSet<>();
					for (String compRole : edge.getComponentRoles()) {
						if (designs.size() > 0) {
							for (List<String> design : designs) {
								List<String> comboDesign = new LinkedList<>(design);
								comboDesign.add(compRole);
								comboDesigns.add(comboDesign);
							}
						} else {
							List<String> comboDesign = new LinkedList<String>();
							comboDesign.add(compRole);
							comboDesigns.add(comboDesign);
						}
					}
					designs = comboDesigns;
				}
				
				Node head = edge.getHead();
				if (!head.hasEdges() || head.isAcceptNode()) {
					if (designs.size() + currentNumberOfDesigns < numberOfDesigns) {
						allDesigns.addAll(designs);
						currentNumberOfDesigns += designs.size();
					} else {
						int neededDesigns = numberOfDesigns - currentNumberOfDesigns;
						Iterator<List<String>> generatedDesignsIterator = designs.iterator();
						for (int i = 0; i < neededDesigns; i++) {
							allDesigns.add(generatedDesignsIterator.next());
						}
						return allDesigns;
					}
					if (!designStack.isEmpty()) {
						designs = designStack.pop();
					}

				} else {
					for (Edge headEdge : head.getEdges()) {
						edgeStack.push(headEdge);
					}
					for (int i = 0; i < head.getNumEdges() - 1; i++) {
						designStack.push(designs);
					}
				}
			}
		}
		
		return allDesigns;
	}

	public Set<List<String>> partition() {
		Set<List<String>> partitions = new HashSet<>();



		return partitions;
	}
}
