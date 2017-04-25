package knox.spring.data.neo4j.sample;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.EnumerateType;
import knox.spring.data.neo4j.domain.Node;

import org.sbolstandard.core2.Collection;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.SBOLDocument;

import java.net.URI;
import java.util.*;

public class DesignSampler {
	
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
			
			while (node.hasEdges() && (!node.isAcceptNode() || rand.nextInt(2) == 1)) {
				Iterator<Edge> edgerator = node.getEdges().iterator();
				int k = rand.nextInt(node.getNumEdges());
				int j = 0;
				
				while (j < k) {
					edgerator.next();
					j++;
				}
				
				Edge edge = edgerator.next();
				
				if (edge.hasComponentIDs()) {
					sample.add(edge.getComponentID(rand.nextInt(edge.getNumComponentIDs())));
				}
				
				node = edge.getHead();
			}
			
			samples.add(sample);
		}
		
		return samples;
	}

	public Set<List<String>> enumerate(EnumerateType enumerateType, Integer requestedDesigns) {
		int numberOfDesigns = requestedDesigns != null ? requestedDesigns : Integer.MAX_VALUE;

		if (enumerateType == EnumerateType.BFS) {
			return bfsEnumerate(numberOfDesigns);
		} else {
			return dfsEnumerate(numberOfDesigns);
		}
	}

	private Set<List<String>> dfsEnumerate(int numberOfDesigns) {
		Set<List<String>> allDesigns = new HashSet<List<String>>();
		int currentNumberOfDesigns = 0;

		for (Node start : starts) {
			Set<List<String>> designs = new HashSet<>();
			Set<List<String>> generatedDesigns = dfsEnumerateRecursive(start, designs);

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

	private Set<List<String>> dfsEnumerateRecursive(Node node, Set<List<String>> designs) {
		if (!node.hasEdges() || node.isAcceptNode()) {
			return designs;
		}

		Set<List<String>> allVisitedDesigns = new HashSet<>();

		for (Edge edge : node.getEdges()) {
			Set<List<String>> visitedDesigns = new HashSet<>();

			for (String componentId : edge.getComponentIDs()) {
				for (List<String> design : designs) {
					List<String> copiedDesign = new ArrayList<>(design);
					design.add(componentId);
					visitedDesigns.add(copiedDesign);
				}
			}

			allVisitedDesigns.addAll(dfsEnumerateRecursive(edge.getHead(), visitedDesigns));
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

				if (edge.hasComponentIDs()) {
					Set<List<String>> comboDesigns = new HashSet<>();

					for (String compID : edge.getComponentIDs()) {
						if (designs.size() > 0) {

							for (List<String> design : designs) {
								List<String> comboDesign = new LinkedList<>(design);
								comboDesign.add(compID);
								comboDesigns.add(comboDesign);
							}

						} else {
							List<String> comboDesign = new LinkedList<String>();
							comboDesign.add(compID);
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

	public Set<List<String>> partition(int numberOfDesigns) {
		Set<List<String>> partitions = new HashSet<>();


		return partitions;
	}
}
