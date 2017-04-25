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
import java.lang.*;

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

			LOG.warn(String.valueOf(generatedDesigns.size()));

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
			String error = node.isAcceptNode() ? "accept node" : "no edges";
			LOG.warn(error);
			return designs;
		}

		Set<List<String>> allVisitedDesigns = new HashSet<>();

		for (Edge edge : node.getEdges()) {
			Set<List<String>> visitedDesigns = new HashSet<>();

			for (String componentId : edge.getComponentIDs()) {
				LOG.warn("component id {}", componentId);

				for (List<String> design : designs) {
					List<String> copiedDesign = new ArrayList<>(design);
					copiedDesign.add(componentId);
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

	public Set<List<String>> partition() {
		Set<List<String>> partitions = new HashSet<>();

        ArrayList<ArrayList<Double>> graphadj = new ArrayList<>();
        // Create adj matrix

        // Normalize adjacency matrix
        int numCols = graphadj.get(0).size();
        int sumCols[] = new int[numCols];

        // Get sums of the columns
        sumCols = calcSumCols(graphadj);

        // Divide elements in column by sum of column
        double newVal;
        for (ArrayList<Double> row: graphadj) {
            for (int i = 0; i < numCols; i++) {
                if (row.get(i) == 1) {
                    newVal = row.get(i)/sumCols[i];
                    row.set(i, newVal);
                }
            }
        }

        ArrayList<ArrayList<Double>> oldAdj = graphadj;
        int numRows = oldAdj.size();
        numCols = oldAdj.get(0).size();
        int inflation_power = 2;

        do {

            oldAdj = graphadj;

            // Expansion and Inflation
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    if (graphadj.get(i).get(j) == 0)
                        continue;

                    newVal = Math.pow(graphadj.get(i).get(j) * graphadj.get(i).get(j), inflation_power);
                    graphadj.get(i).set(j, newVal);
                }
            }

            // Get sums of colums
            sumCols = calcSumCols(graphadj);

            // Divide elements in column by sum of column
            for (ArrayList<Double> row: graphadj) {
                for (int i = 0; i < numCols; i++) {
                    if (row.get(i) == 1) {
                        newVal = row.get(i) / sumCols[i];
                        row.set(i,newVal);
                    }
                }
            }


        } while(isChanged(graphadj,oldAdj));


        // Analyze graphadj to discover the set of clusters
        Set<List<Integer>> clusters = new HashSet<>();
        for (ArrayList<Double> row: graphadj) {
            List<Integer> cluster = new ArrayList<Integer>();
            for (int i = 0; i < numCols; i++) {
                if (row.get(i) >= 0.01) {
                    cluster.add(i);
                }
            }
            clusters.add(cluster);
        }



		return partitions;
	}

	private boolean isChanged(ArrayList<ArrayList<Double>> old, ArrayList<ArrayList<Double>> mod) {
            int numRows = old.size();
            int numCols = old.get(0).size();

            boolean change = true;
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                   if (Math.abs(old.get(i).get(j) - mod.get(i).get(j)) <= 0.001)
                       change = false;
                }
            }
            return change;
    }

    private int[] calcSumCols(ArrayList<ArrayList<Double>> graphadj) {
        int numCols = graphadj.get(0).size();
        int sumCols[] = new int[numCols];

        // Get sums of the columns
        for (ArrayList<Double> row: graphadj) {
            for (int i = 0; i < numCols; i++) {
                if (row.get(i) == 1.0)
                    sumCols[i]++;
            }
        }
        return sumCols;
    }


}
