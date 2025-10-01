package knox.spring.data.neo4j.sample;

import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DesignSampler {
	private static final Logger LOG = LoggerFactory.getLogger(DesignSampler.class);
	
	private NodeSpace space;
	
	private List<Node> startNodes;
	
	public DesignSampler(NodeSpace space) {
		this.space = space;
		
		startNodes = new LinkedList<Node>(space.getStartNodes());
	}

    /*
		This method will randomly sample from the graph.

		Arguments:
			- int numSamples: the number of samples that the user wants returned

		Returns:
			- Set<List<String>>: The paths that are generated. Each List<String> represents an ordering of
								 the specific component ids.
	 */
	

	public Set<List<String>> sample(int numberOfDesigns, int minLength, int maxLength, boolean isWeighted, boolean positiveOnly, boolean isSampleSpace) {

		Set<List<String>> designs = new HashSet<List<String>>();
		
		Random rand = new Random();
		int tries = 0;

		double avgWeight = 0;
		double stdev = 0;
		if (isWeighted && !positiveOnly && !isSampleSpace) {
			// Get average weight of edges and standard deviation.
			avgWeight = Double.parseDouble(space.getAvgScoreofAllNonBlankEdges());
			stdev = space.getStdev();
		}

		System.out.println("Begin Sampling");
		System.out.println("Number of Designs: " + String.valueOf(numberOfDesigns));
		System.out.println("Min Length: " + String.valueOf(minLength));
		System.out.println("Max Length: " + String.valueOf(maxLength));
		System.out.println("Is Weighted: " + isWeighted);
		System.out.println("Positive Only: " + positiveOnly);
		System.out.println("Is Sample Space: " + isSampleSpace + "\n");

		while ((designs.size() < numberOfDesigns) && (tries < numberOfDesigns*2)) {
			tries++;
			List<String> design = new LinkedList<String>();

			Double probability = 1.0;
			
			Node node = startNodes.get(rand.nextInt(startNodes.size()));
			
            Edge edge = new Edge();

            while (node.hasEdges() && (!node.isAcceptNode() || rand.nextInt(2) == 1) && (design.size() < maxLength || maxLength == 0)) {

				if (isWeighted && !isSampleSpace) {
					// weighted sampling
					ArrayList<String> parts = new ArrayList<>();
					ArrayList<Edge> edges = new ArrayList<>();
					ArrayList<Double> weights = new ArrayList<>();
					ArrayList<Double> weightsStandardScaled = new ArrayList<>();
					ArrayList<Double> weightsScaled = new ArrayList<>();
					double totalWeights = 0.0;
					
					if (node.getEdges().size() > 1 || node.getEdges().iterator().next().getComponentIDs().size() > 1) {

						// Add up the total weights and populate lists above for a given node
						for (Edge e: node.getEdges()) {

							int i = 0;
							for (Double weight : e.getWeight()) {

								if ((weight > 0) || (!positiveOnly)){

									// Blank Egdes Naming
									if (e.isBlank()) {
										parts.add("blank");
									} else {
										parts.add(e.getComponentIDs().get(i));
									}

									totalWeights += weight;
									edges.add(e);
									weights.add(weight);
								}

								i++;
							}
						}

						// if not positive only, perform transformation to [0, 1]
						if (!positiveOnly) {
							double to_min = 0;
							double to_max = 1;
							double from_min = -3;
							double from_max = 3;

							// Standard Scaled
							for (double x : weights) {
								weightsStandardScaled.add((x - avgWeight) / stdev);
							}

							totalWeights = 0.0;
							for (double x : weightsStandardScaled) {
								double y = (x - from_min) / (from_max - from_min) * (to_max - to_min) + to_min;

								weightsScaled.add(y);
								totalWeights += y;
							}

							// Update weights
							weights = weightsScaled;
						}
						

						// random number
						double rWeight = rand.nextDouble();

						// part selection
						int partIndex = 0;
						double currentWeight = (weights.get(partIndex) / totalWeights);
						while (rWeight > currentWeight) {
							partIndex++;
							currentWeight += (weights.get(partIndex) / totalWeights); 
						}

						// Update Probability
						probability = probability * (weights.get(partIndex) / totalWeights);
				

						// Selected Edge
						edge = edges.get(partIndex);

						// Add Part to Design
						if (edge.hasComponentIDs()) {
							design.add(parts.get(partIndex));
						}
					} else {
						// Node only has one outgoing edge
						edge = node.getEdges().iterator().next();

						if (edge.hasComponentIDs()) {
							design.add(edge.getComponentIDs().get(0));
						}
					}

				} else if (isSampleSpace) {
					// sample space sampling
					ArrayList<Edge> edges = new ArrayList<>();
					ArrayList<String> parts = new ArrayList<>();
					ArrayList<Double> weights = new ArrayList<>();
					
					for (Edge e : node.getEdges()) {
						
						int i = 0;
						for (Double w : e.getWeight()) {
							edges.add(e);
							
							// Blank Egdes Naming
							if (e.isBlank()) {
								parts.add("blank");
							} else {
								parts.add(e.getComponentIDs().get(i));
							}

							weights.add(w);
							i++;
						}
					}

					// random number
					double rWeight = rand.nextDouble();

					// part selection
					int partIndex = 0;
					double currentWeight = weights.get(partIndex);
					while (rWeight > currentWeight) {
						partIndex++;
						currentWeight += weights.get(partIndex); 
					}

					// Update Probability
					probability = probability * weights.get(partIndex);

					// Selected Edge
					edge = edges.get(partIndex);

					// Add Part to Design
					if (edge.hasComponentIDs()) {
						design.add(parts.get(partIndex));
					}

				} else {
					// unweighted Sampling
					int item = new Random().nextInt(node.getEdges().size());
					int i = 0;
					for (Edge e : node.getEdges()) {
						if (i == item) {
							edge = e;
							break;
						} else {
							i++;
						}

					}
				
					if (edge.hasComponentIDs()) {
						String part = edge.getComponentIDs().get(rand.nextInt(edge.getComponentIDs().size()));

						if (edge.getOrientation() != null && edge.getOrientation().getValue().equals("reverseComplement")) {
							part = part + "_REVERSE";
						}
						
						design.add(part);
					}
				}
				
				node = edge.getHead();
			}

			boolean includeDesign = false;
			if (design.size() >= minLength) {
				includeDesign = true;
			}

			// Add probability to the end of the design
			if (isWeighted && !isSampleSpace) {
				design.add(String.valueOf(probability));
				//design.add(String.valueOf(avgWeight));
				//design.add(String.valueOf(stdev));
			} else if (isSampleSpace) {
				design.add(String.valueOf(probability));
			}
			
			// add design to designs
			if (includeDesign) {
				designs.add(design);
			}
		}

		System.out.println(designs);

		System.out.println("Done Sampling");
		
		return designs;
	}

	/*
		This method will enumerate the graph: providing all possible paths from the start of the graph
		to the end of the graph.

		Arguments:
			- enumerateType [EnumerateType]: Either will perform a breadth-first-depth or a depth-first-search
			- numDesigns [int]: The number of requested designs. 5 is the default.
			- maxLength [int]: The maximum length of a design in components. If zero or less, method will not 
							   enumerate cycles

		Returns:
			- Set<List<String>>: The paths that are generated. Each List<String> represents an ordering of
								 the specific component ids.
	 */
	public Collection<List<Map<String, Object>>> enumerate(int numDesigns, int minLength, 
			int maxLength, int maxCycles, Boolean allowDuplicates, Boolean isSampleSpace, EnumerateType type) {
		if (type == EnumerateType.BFS) {
			return bfsEnumerate(numDesigns, minLength, maxLength, maxCycles, allowDuplicates, isSampleSpace);
		} else {
			return dfsEnumerate(numDesigns, minLength, maxLength, maxCycles, allowDuplicates, isSampleSpace);
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
//	private Set<List<String>> dfsEnumerate(int numberOfDesigns) {
//		Set<List<String>> allDesigns = new HashSet<>();
//		int currentNumberOfDesigns = 0;
//
//		for (Node start : startNodes) {
//			Set<List<String>> designs = new HashSet<>();
//			designs.add(new ArrayList<>());
//			Set<List<String>> generatedDesigns = dfsEnumerateRecursive(start, designs);
//			LOG.info("generated designs size {}", generatedDesigns.size());
//			LOG.info("Node start {}", start.getNodeID());
//
//			if (generatedDesigns.size() + currentNumberOfDesigns < numberOfDesigns) {
//				allDesigns.addAll(generatedDesigns);
//				currentNumberOfDesigns += generatedDesigns.size();
//			} else {
//				int neededDesigns = numberOfDesigns - currentNumberOfDesigns;
//				Iterator<List<String>> generatedDesignsIterator = generatedDesigns.iterator();
//				for (int i = 0; i < neededDesigns; i++) {
//					allDesigns.add(generatedDesignsIterator.next());
//				}
//
//				return allDesigns;
//			}
//
//		}
//
//		return allDesigns;
//	}

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
//	private Set<List<String>> dfsEnumerateRecursive(Node node, Set<List<String>> designs) {
//		if (!node.hasEdges() || node.isAcceptNode()) {
//			LOG.info("node done {}", node.getNodeID());
//			return designs;
//		}
//
//		Set<List<String>> allVisitedDesigns = new HashSet<>();
//		LOG.info("node id {}", node.getNodeID());
//
//		for (Edge edge : node.getEdges()) {
//			Set<List<String>> visitedDesigns = new HashSet<>();
//
//			if (edge.hasComponentRoles()) {
//				for (String componentRole : edge.getComponentRoles()) {
//					LOG.info("component role {}", componentRole);
//
//					for (List<String> design : designs) {
//						List<String> copiedDesign = new ArrayList<>(design);
//						copiedDesign.add(componentRole);
//						visitedDesigns.add(copiedDesign);
//					}
//				}
//			} else {
//				visitedDesigns = designs;
//			}
//
//			allVisitedDesigns.addAll(dfsEnumerateRecursive(edge.getHead(), visitedDesigns));
//			LOG.info("visited designs size {}", allVisitedDesigns.size());
//		}
//
//		return allVisitedDesigns;
//	}
	
	private List<List<Map<String, Object>>> multiplyDesignsSampleSpace(List<List<Map<String, Object>>> designs,
			Edge edge) {
		List<List<Map<String, Object>>> comboDesigns = new LinkedList<List<Map<String, Object>>>();
		
		if (!edge.hasComponentIDs()) {
			Map<String, Object> comp = new HashMap<String, Object>();
			//comp.put("id", null);
			if (!edge.getWeight().isEmpty()){
				comp.put("weight", edge.getWeight().get(0));
			}
			comp.put("isBlank", "true");
			if (!designs.isEmpty()) {
				for (List<Map<String, Object>> design : designs) {
					List<Map<String, Object>> comboDesign = new ArrayList<Map<String, Object>>(design);

					comboDesign.add(comp);

					comboDesigns.add(comboDesign);
				}
			} else {
				List<Map<String, Object>> comboDesign = new ArrayList<Map<String, Object>>();

				comboDesign.add(comp);

				comboDesigns.add(comboDesign);
			}
		} else {

			HashMap<String, Double> componentIDstoWeights = edge.componentIDtoWeight();
			HashMap<String, String> componentIDstoRoles = edge.componentIDtoRole();

			for (String compID : edge.getComponentIDs()) {
				Map<String, Object> comp = new HashMap<String, Object>();

				comp.put("id", compID);

				comp.put("roles", componentIDstoRoles.get(compID));

				comp.put("weight", componentIDstoWeights.get(compID));

				comp.put("isBlank", "false");

//				comp.put("orientation", edge.getOrientation());
				comp.put("orientation", edge.getOrientation().getValue()); //does this need to be string?

				if (!designs.isEmpty()) {
					for (List<Map<String, Object>> design : designs) {
						List<Map<String, Object>> comboDesign = new ArrayList<Map<String, Object>>(design);

						comboDesign.add(comp);

						comboDesigns.add(comboDesign);
					}
				} else {
					List<Map<String, Object>> comboDesign = new ArrayList<Map<String, Object>>();

					comboDesign.add(comp);

					comboDesigns.add(comboDesign);
				}
			}
		}
		
		return comboDesigns;
	}

	private List<List<Map<String, Object>>> multiplyDesigns(List<List<Map<String, Object>>> designs, Edge edge, int numDesigns) {
		List<List<Map<String, Object>>> comboDesigns = new LinkedList<List<Map<String, Object>>>();
		int designCount = 0;
		
		if (!edge.hasComponentIDs()) {
			for (List<Map<String, Object>> design : designs) {
				List<Map<String, Object>> comboDesign = new ArrayList<Map<String, Object>>(design);
				designCount++;

				comboDesigns.add(comboDesign);
				
				if (numDesigns > 0 && designCount >= numDesigns) {
					return comboDesigns;
				}
			}
		} else {

			HashMap<String, Double> componentIDstoWeights = edge.componentIDtoWeight();
			HashMap<String, String> componentIDstoRoles = edge.componentIDtoRole();

			for (String compID : edge.getComponentIDs()) {
				Map<String, Object> comp = new HashMap<String, Object>();
				comp.put("id", compID);
				comp.put("roles", componentIDstoRoles.get(compID));
				comp.put("weight", componentIDstoWeights.get(compID));
				comp.put("isBlank", "false");
				comp.put("orientation", edge.getOrientation().getValue());

				if (!designs.isEmpty()) {
					for (List<Map<String, Object>> design : designs) {
						List<Map<String, Object>> comboDesign = new ArrayList<Map<String, Object>>(design);
						designCount++;

						comboDesign.add(comp);

						comboDesigns.add(comboDesign);

						if (numDesigns > 0 && designCount >= numDesigns) {
							return comboDesigns;
						}
					}
				} else {
					List<Map<String, Object>> comboDesign = new ArrayList<Map<String, Object>>();
					designCount++;

					comboDesign.add(comp);

					comboDesigns.add(comboDesign);
					if (numDesigns > 0 && designCount >= numDesigns) {
						return comboDesigns;
					}
				}
			}
		}
		
		return comboDesigns;
	}
	
	/* 
	public HashSet<List<Map<String, Object>>> bfsEnumerateSet(int numDesigns, int minLength, int maxLength, boolean isSampleSpace) {
		HashSet<List<Map<String, Object>>> allDesigns = new HashSet<List<Map<String, Object>>>();
	
		for (Node startNode : startNodes) {
			List<List<Map<String, Object>>> designs = new LinkedList<List<Map<String, Object>>>();
			
			Set<Node> localNodes = new HashSet<Node>();
			
			localNodes.add(startNode);
			
			Stack<Set<Node>> localNodeStack = new Stack<Set<Node>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				localNodeStack.push(new HashSet<Node>(localNodes));
			}
			
			Stack<Edge> edgeStack = new Stack<Edge>();
			
			if (startNode.hasEdges()) {
				for (Edge edge : startNode.getEdges()) {
					edgeStack.push(edge);
				}
			}
			
			Stack<List<List<Map<String, Object>>>> designStack = new Stack<List<List<Map<String, Object>>>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				designStack.push(designs);
			}
			
			while (!edgeStack.isEmpty()) {
				Edge edge = edgeStack.pop();
				
				if (isSampleSpace) {
					designs = multiplyDesignsSampleSpace(designs, edge);
				} else {
					designs = multiplyDesigns(designs, edge, numDesigns);
				}
				
				if (!designs.isEmpty() && maxLength > 0 && designs.get(0).size() > maxLength) {
					if (!designStack.isEmpty()) {
						localNodes = localNodeStack.pop();
						
						designs = designStack.pop();
					}
				} else { 
					if (edge.getHead().isAcceptNode()) {
						List<List<Map<String, Object>>> atLeastMinDesigns = filterUnderMinDesigns(designs,
								minLength);
						
						if (numDesigns < 1 || allDesigns.size() + atLeastMinDesigns.size() < numDesigns) {
							allDesigns.addAll(atLeastMinDesigns);
						} else {
							int diffDesignCount = numDesigns - allDesigns.size();

							Iterator<List<Map<String, Object>>> designerator = atLeastMinDesigns.iterator();

							for (int i = 0; i < diffDesignCount; i++) {
								allDesigns.add(designerator.next());
							}

							return allDesigns;
						}
					} 

					if (edge.getHead().hasEdges() 
							&& (!localNodes.contains(edge.getHead()) || numDesigns > 0 
									|| maxLength > 0)) {
						localNodes.add(edge.getHead());
						
						for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
							localNodeStack.push(new HashSet<Node>(localNodes));
						}
						
						for (Edge headEdge : edge.getHead().getEdges()) {
							edgeStack.push(headEdge);
						}

						for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
							designStack.push(designs);
						}
					} else if (!designStack.isEmpty()) {
						localNodes = localNodeStack.pop();
						
						designs = designStack.pop();
					}
				}
			}
		}
		
		return allDesigns;
	}
	
	private HashSet<List<Map<String, Object>>> dfsEnumerateSet(int numDesigns, int minLength, int maxLength, Boolean isSampleSpace) {
		HashSet<List<Map<String, Object>>> allDesigns = new HashSet<List<Map<String, Object>>>();
	
		for (Node startNode : startNodes) {
			List<List<Map<String, Object>>> designs = new LinkedList<List<Map<String, Object>>>();
			
			Set<Node> localNodes = new HashSet<Node>();
			
			localNodes.add(startNode);
			
			Stack<Set<Node>> localNodeStack = new Stack<Set<Node>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				localNodeStack.push(new HashSet<Node>(localNodes));
			}
			
			Stack<Edge> edgeStack = new Stack<Edge>();
			
			if (startNode.hasEdges()) {
				for (Edge edge : startNode.getEdges()) {
					edgeStack.push(edge);
				}
			}
			
			Stack<List<List<Map<String, Object>>>> designStack = new Stack<List<List<Map<String, Object>>>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				designStack.push(designs);
			}
			
			while (!edgeStack.isEmpty()) {
				Edge edge = edgeStack.pop();
				
				if (isSampleSpace) {
					designs = multiplyDesignsSampleSpace(designs, edge);
				} else {
					designs = multiplyDesigns(designs, edge, numDesigns);
				}
				
				if (!designs.isEmpty() && maxLength > 0 && designs.get(0).size() > maxLength) {
					if (!designStack.isEmpty()) {
						localNodes = localNodeStack.pop();
						
						designs = designStack.pop();
					}
				} else { 
					if (edge.getHead().isAcceptNode()) {
						List<List<Map<String, Object>>> atLeastMinDesigns = filterUnderMinDesigns(designs,
								minLength);
						
						if (numDesigns < 1 || allDesigns.size() + atLeastMinDesigns.size() < numDesigns) {
							allDesigns.addAll(atLeastMinDesigns);
						} else {
							int diffDesignCount = numDesigns - allDesigns.size();

							Iterator<List<Map<String, Object>>> designerator = atLeastMinDesigns.iterator();

							for (int i = 0; i < diffDesignCount; i++) {
								allDesigns.add(designerator.next());
							}

							return allDesigns;
						}
					} 

					if (edge.getHead().hasEdges() 
							&& (!localNodes.contains(edge.getHead()) || numDesigns > 0 
									|| maxLength > 0)) {
						localNodes.add(edge.getHead());
						
						for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
							localNodeStack.push(new HashSet<Node>(localNodes));
						}
						
						for (Edge headEdge : edge.getHead().getEdges()) {
							edgeStack.push(headEdge);
						}

						for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
							designStack.push(designs);
						}
					} else if (!designStack.isEmpty()) {
						localNodes = localNodeStack.pop();
						
						designs = designStack.pop();
					}
				}
			}
		}
		
		return allDesigns;
	}

	private List<List<Map<String, Object>>> bfsEnumerates(int numDesigns, int minLength, int maxLength, Boolean isSampleSpace) {
		List<List<Map<String, Object>>> allDesigns = new ArrayList<List<Map<String, Object>>>();
	
		for (Node startNode : startNodes) {
			List<List<Map<String, Object>>> designs = new LinkedList<List<Map<String, Object>>>();
			
			Set<Node> localNodes = new HashSet<Node>();
			
			localNodes.add(startNode);
			
			Stack<Set<Node>> localNodeStack = new Stack<Set<Node>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				localNodeStack.push(new HashSet<Node>(localNodes));
			}
			
			Stack<Edge> edgeStack = new Stack<Edge>();
			
			if (startNode.hasEdges()) {
				for (Edge edge : startNode.getEdges()) {
					edgeStack.push(edge);
				}
			}
			
			Stack<List<List<Map<String, Object>>>> designStack = new Stack<List<List<Map<String, Object>>>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				designStack.push(designs);
			}
			
			while (!edgeStack.isEmpty()) {
				Edge edge = edgeStack.pop();
				
				if (isSampleSpace) {
					designs = multiplyDesignsSampleSpace(designs, edge);
				} else {
					designs = multiplyDesigns(designs, edge, numDesigns);
				}

				
				if (!designs.isEmpty() && maxLength > 0 && designs.get(0).size() > maxLength) {
					if (!designStack.isEmpty()) {
						localNodes = localNodeStack.pop();
						
						designs = designStack.pop();
					}
				} else { 
					if (edge.getHead().isAcceptNode()) {
						List<List<Map<String, Object>>> atLeastMinDesigns = filterUnderMinDesigns(designs,
								minLength);
						
						if (numDesigns < 1 || allDesigns.size() + atLeastMinDesigns.size() < numDesigns) {
							allDesigns.addAll(atLeastMinDesigns);
						} else {
							int diffDesignCount = numDesigns - allDesigns.size();

							Iterator<List<Map<String, Object>>> designerator = atLeastMinDesigns.iterator();

							for (int i = 0; i < diffDesignCount; i++) {
								allDesigns.add(designerator.next());
							}

							return allDesigns;
						}
					} 

					if (edge.getHead().hasEdges() 
							&& (!localNodes.contains(edge.getHead()) || numDesigns > 0 
									|| maxLength > 0)) {
						localNodes.add(edge.getHead());
						
						for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
							localNodeStack.push(new HashSet<Node>(localNodes));
						}
						
						for (Edge headEdge : edge.getHead().getEdges()) {
							edgeStack.push(headEdge);
						}

						for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
							designStack.push(designs);
						}
					} else if (!designStack.isEmpty()) {
						localNodes = localNodeStack.pop();
						
						designs = designStack.pop();
					}
				}
			}
		}
		
		return allDesigns;
	}
	*/
	private Collection<List<Map<String, Object>>> bfsEnumerate(int numDesigns, int minLength, int maxLength, int maxCycles, 
			Boolean allowDuplicates, Boolean isSampleSpace) {

		Collection<List<Map<String, Object>>> allDesigns = allowDuplicates ? new ArrayList<>() : new LinkedHashSet<>();
		int maxVisitsPerNode = maxCycles + 1;

		for (Node startNode : startNodes) {
			Queue<BFSState> queue = new LinkedList<>();
			Map<Node, Integer> initialVisitCounts = new HashMap<>();
			initialVisitCounts.put(startNode, 1);

			// Start with an empty design
			queue.add(new BFSState(startNode, new ArrayList<>(), initialVisitCounts));

			while (!queue.isEmpty() && (numDesigns < 1 || allDesigns.size() < numDesigns)) {
				BFSState state = queue.poll();
				Node node = state.node;
				List<List<Map<String, Object>>> currentDesigns = state.currentDesigns;
				Map<Node, Integer> visitCounts = state.visitCounts;

			// If accept node, add all designs that meet minLength
				if (node.isAcceptNode()) {
					for (List<Map<String, Object>> design : currentDesigns) {
						if (design.size() >= minLength) {
							if (allowDuplicates || !allDesigns.contains(design)) {
							allDesigns.add(new ArrayList<>(design));
							if (numDesigns > 0 && allDesigns.size() >= numDesigns) {
								break;
							}
							}
						}
					}
				}

				for (Edge edge : node.getEdges()) {
					Node nextNode = edge.getHead();
					if (nextNode != null) {
						int visits = visitCounts.getOrDefault(nextNode, 0);
						if (visits < maxVisitsPerNode) {
							List<List<Map<String, Object>>> nextDesigns;
							if (isSampleSpace) {
								nextDesigns = multiplyDesignsSampleSpace(currentDesigns, edge);
							} else {
								nextDesigns = multiplyDesigns(currentDesigns, edge, numDesigns);
							}
							// Prune designs that exceed maxLength
							List<List<Map<String, Object>>> prunedDesigns = new ArrayList<>();
							for (List<Map<String, Object>> d : nextDesigns) {
								if (maxLength <= 0 || d.size() <= maxLength) {
									prunedDesigns.add(d);
								}
							}
						Map<Node, Integer> nextVisitCounts = new HashMap<>(visitCounts);
						nextVisitCounts.put(nextNode, visits + 1);
						queue.add(new BFSState(nextNode, prunedDesigns, nextVisitCounts));
						}
					}
				}
			}
		}
		return allDesigns;
	}

	private Collection<List<Map<String, Object>>> dfsEnumerate(int numDesigns, int minLength, int maxLength, int maxCycles, 
			Boolean allowDuplicates, Boolean isSampleSpace) {

		Collection<List<Map<String, Object>>> allDesigns = allowDuplicates ? new ArrayList<>() : new LinkedHashSet<>();
		Map<Node, Integer> visitCounts = new HashMap<>();

		int maxVisitsPerNode = maxCycles + 1;

		for (Node startNode : startNodes) {
			dfsHelper(startNode, new ArrayList<>(), allDesigns, numDesigns, minLength, maxLength, 
					allowDuplicates, isSampleSpace, visitCounts, maxVisitsPerNode);
			
			if (allDesigns.size() >= numDesigns && numDesigns > 0) break;
		}
		return allDesigns;
	}

	private void dfsHelper(Node node, List<List<Map<String, Object>>> currentDesigns,
                       Collection<List<Map<String, Object>>> allDesigns,
                       int numDesigns, int minLength, int maxLength, Boolean allowDuplicates, Boolean isSampleSpace,
                       Map<Node, Integer> visitCounts, int maxVisitsPerNode) {

		if (allDesigns.size() >= numDesigns && numDesigns > 0) return;

		int visits = visitCounts.getOrDefault(node, 0);
		if (visits >= maxVisitsPerNode) return; // limit reached

		visitCounts.put(node, visits + 1);

		// If accept node, add all designs that meet minLength
		if (node.isAcceptNode()) {
			for (List<Map<String, Object>> design : currentDesigns) {
				if (design.size() >= minLength) {
					if (allowDuplicates || !allDesigns.contains(design)) {
						allDesigns.add(new ArrayList<>(design));
						if (allDesigns.size() >= numDesigns && numDesigns > 0) {
							visitCounts.put(node, visits); // backtrack
							return;
						}
					}
				}
			}
		}

		for (Edge edge : node.getEdges()) {
			if (edge.getHead() != null) {
				List<List<Map<String, Object>>> nextDesigns;
				if (isSampleSpace) {
					nextDesigns = multiplyDesignsSampleSpace(currentDesigns, edge);
				} else {
					nextDesigns = multiplyDesigns(currentDesigns, edge, numDesigns);
				}
				// Prune designs that exceed maxLength
				List<List<Map<String, Object>>> prunedDesigns = new ArrayList<>();
				for (List<Map<String, Object>> d : nextDesigns) {
					if (maxLength <= 0 || d.size() <= maxLength) {
						prunedDesigns.add(d);
					}
				}
				dfsHelper(edge.getHead(), prunedDesigns, allDesigns, numDesigns, minLength, maxLength, 
						allowDuplicates, isSampleSpace, visitCounts, maxVisitsPerNode);
			}
		}

		// backtrack
		if (visits + 1 == 1) {
			visitCounts.remove(node);
		} else {
			visitCounts.put(node, visits);
		}
	}

	public Collection<List<Map<String, Object>>> processEnumerate(Collection<List<Map<String, Object>>> samplerOutput, 
			boolean isWeighted, boolean isSampleSpace, boolean printDesigns) {

		if (isWeighted && !isSampleSpace) {
			int i = 0;
			for (List<Map<String,Object>> design : samplerOutput) {
				double total = 0.0;
				double length = design.size();
				for (Map<String,Object> element : design) {
					if (element.get("isBlank") == "true") {
						length = length - 1.0;
					} else {
						total = total + (double) element.get("weight");
					}
				}
				double averageWeight = total / length;

				for (Map<String,Object> element : design) {
					element.put("average_weight", averageWeight);
				}

				i++;
			}
		} else if (isSampleSpace) {
			int i = 0;
			for (List<Map<String,Object>> design : samplerOutput) {
				double probability = 1.0;
				
				for (Map<String,Object> element : design) {
					probability = probability * (double) element.get("weight");
				}

				for (Map<String,Object> element : design) {
					element.put("probability", probability);
				}

				i++;
			}
		}

		// Print Designs
		if (printDesigns) {
			int i = 0;
			for (List<Map<String,Object>> design : samplerOutput) {
				i++;
				List<Object> design_parts = new ArrayList<>();
				for (Map<String,Object> element : design) {
					design_parts.add(element.get("id"));
				}
				System.out.println();
				System.out.println(i);
				System.out.println(design_parts);
				
			}
		}
        
        return samplerOutput;
	}

	private List<List<Map<String, Object>>> filterUnderMinDesigns(List<List<Map<String, Object>>> designs,
			int minLength) {
		List<List<Map<String, Object>>> atLeastMinDesigns = new LinkedList<List<Map<String, Object>>>();
		
		for (List<Map<String, Object>> design : designs) {
			if (design.size() >= minLength) {
				atLeastMinDesigns.add(design);
			}
		}
		
		return atLeastMinDesigns;
	}

    /*
        This method is responsible for creating an adjacency matrix from a given graph.
        It does so by looping over each of the nodes in a design space and assigning them
        a row/column number and storing this information in a map with the nodeID as the
        key and the row/column number as the value.

        After that has been completed, the method then iterates through each of the connecting
        nodes that every node has. If a connection exists, the values for the row and column
        numbers for each of the nodes will be found in the graph and the adjacency matrix
        will be updated to 1 for that specific connection.

        Because this is a directed graph, we understand a connection at cell [i][j] as an
        edge existing from i to j, and not necessarily vice versa.

        This takes O(N + KN) where N is the number of nodes and K is the number of outgoing
        edges each node has (this number will vary for each node).

        Arguments:
            - nodeIdsToRowNumber [Map<String, Integer>]: The map that will store the nodeIds to
            row numbers

        Returns:
            - double[][]: The adjacency matrix. 0 represents no connection and 1 represents
            a connection.
     */
    private double[][] createAdjacencyMatrix(Map<Integer,String> rowtoNodeIds) {
        Set<Node> allNodes = space.getNodes();
        double[][] adjacencyMatrix = new double[allNodes.size()][allNodes.size()];
        int counter = 0;

        Map<String, Integer> nodeIdsToRowNumber = new HashMap<>();

        for (Node node : allNodes) {
            nodeIdsToRowNumber.put(node.getNodeID(), counter);
            rowtoNodeIds.put(counter, node.getNodeID());
            counter += 1;
        }


        for (Node node : allNodes) {
            int rowNumber = nodeIdsToRowNumber.get(node.getNodeID());
            for (Edge edge : node.getEdges()) {
                Node connectingNode = edge.getHead();
                int columnNumber = nodeIdsToRowNumber.get(connectingNode.getNodeID());
                adjacencyMatrix[rowNumber][columnNumber] = 1.0;
            }
        }


        return adjacencyMatrix;
    }

    /*
    This method implements the Markov Clustering algorithm in order to discover
    highly connected regions of the design space.

    Arguments:
        - None

    Returns:
        - A Set of List<String>: Each List has a string of NodeIds belonging to one cluster.
    */
    public Set<List<String>> partition() {
        Map<Integer,String> rowtoNodeIds = new HashMap<>();
        double[][] graphadj = createAdjacencyMatrix(rowtoNodeIds);

        graphadj = normByCol(graphadj);

        double[][] oldAdj;
        double newVal;
        int numRows = graphadj.length;
        int numCols = graphadj[0].length;
        int inflation_power = 2;

        do {

            oldAdj = graphadj;

            // Expansion and Inflation
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    if (graphadj[i][j] == 0)
                        continue;

                    newVal = Math.pow(graphadj[i][j] * graphadj[i][j], inflation_power);
                    graphadj[i][j] =  newVal;
                }
            }

            graphadj = normByCol(graphadj);


        } while(isChanged(graphadj,oldAdj));


        // Analyze graphadj to discover the set of clusters
        Set<List<String>> clusters = new HashSet<>();
        for (double[] row: graphadj) {
            List<String> cluster = new ArrayList<>();
            for (int i = 0; i < numCols; i++) {
                if (row[i] >= 0.01) {
                    String nodeID = rowtoNodeIds.get(i);
                    cluster.add(nodeID);
                }
            }
            clusters.add(cluster);
        }



        return clusters;
    }

    private boolean isChanged(double[][] old, double[][] mod) {
        int numRows = old.length;
        int numCols = old[0].length;

        boolean change = true;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if (Math.abs(old[i][j] - mod[i][j]) <= 0.001)
                    change = false;
            }
        }
        return change;
    }

    private double[][] normByCol(double[][] graphadj) {
        int numCols = graphadj[0].length;
        int sumCols[] = new int[numCols];

        // Get sums of the columns
        for (double [] row: graphadj) {
            for (int i = 0; i < numCols; i++) {
                if (row[i] == 1.0)
                    sumCols[i]++;
            }
        }

        // Divide elements in column by sum of column
        for (double[] row: graphadj) {
            for (int i = 0; i < numCols; i++) {
                if (row[i] == 1) {
                    row[i] = row[i]/sumCols[i];
                }
            }
        }
        return graphadj;
    }
    
    public enum EnumerateType { BFS, DFS }

	private static class BFSState {
		Node node;
		List<List<Map<String, Object>>> currentDesigns;
		Map<Node, Integer> visitCounts;

		BFSState(Node node, List<List<Map<String, Object>>> currentDesigns, Map<Node, Integer> visitCounts) {
			this.node = node;
			this.currentDesigns = currentDesigns;
			this.visitCounts = visitCounts;
		}
	}

}
