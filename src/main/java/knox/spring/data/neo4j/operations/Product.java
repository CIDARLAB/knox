package knox.spring.data.neo4j.operations;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Node.NodeType;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Product {
	
	private NodeSpace productSpace;
	
	private HashMap<String, Node> crossIDToProductNode;
	
	private HashMap<String, Set<Edge>> rowIDsToProductEdges;
	private HashMap<String, Set<Edge>> colIDsToProductEdges;
	
	private HashMap<String, Set<Node>> rowIDToProductNodes;
	private HashMap<String, Set<Node>> colIDToProductNodes;
	
	private HashMap<String, Node> rowIDToDiffNode;
	private HashMap<String, Node> colIDToDiffNode;
	
	private static final Logger LOG = LoggerFactory.getLogger(Product.class);
	
	public Product(NodeSpace productSpace) {
		this.productSpace = productSpace;
	}
	
    public NodeSpace getSpace() {
    	return productSpace;
    }
    
    public List<Set<Edge>> applyTensor(NodeSpace colSpace, int tolerance, int degree, Set<String> roles) {
    	return applyTensor(colSpace, tolerance, degree, roles, false);
    }
    
    public List<Set<Edge>> applyTensor(NodeSpace colSpace, int tolerance, int degree, Set<String> roles, 
    		boolean isStrongProduct) {
    	List<Set<Edge>> blankProductEdges = new LinkedList<Set<Edge>>();
    	blankProductEdges.add(new HashSet<Edge>());
    	blankProductEdges.add(new HashSet<Edge>());
    	
    	if (productSpace.hasNodes() && colSpace.hasNodes()) {
    		NodeSpace rowSpace = initializeProduct(colSpace);
			
    		crossNodes(rowSpace.getStartNodes(), colSpace.getStartNodes(), degree);
    		crossNodes(rowSpace.getAcceptNodes(), colSpace.getAcceptNodes(), degree);
			
			crossEdges(rowSpace.getEdges(), colSpace.getEdges(), tolerance, degree, roles);
			
			Set<Edge> blankRowEdges = rowSpace.getBlankEdges();
			Set<Edge> blankColEdges = colSpace.getBlankEdges();

			if (isStrongProduct) {
				crossBlankEdges(blankRowEdges, blankColEdges, blankProductEdges);
				
				productSpace.deleteUnconnectedNodes();
				
				updateIDToProductNodes(rowIDToProductNodes);
				updateIDToProductNodes(colIDToProductNodes);
			} else {
				insertBlankEdges(blankRowEdges, colSpace.getNodes(), rowIDToProductNodes, colIDToProductNodes, 
						blankProductEdges);
				insertBlankEdges(blankColEdges, rowSpace.getNodes(), colIDToProductNodes, rowIDToProductNodes, 
						blankProductEdges);
				
				multiCrossBlankEdges(blankRowEdges, blankColEdges, blankProductEdges);
				
				if (degree == 0) {
		    		productSpace.labelSourceNodesStart();
		    		productSpace.labelSinkNodesAccept();
		    		
		    		productSpace.deleteUnacceptableNodes();
		    	} else if (degree == 2) {
		    		productSpace.deleteUnacceptableNodes();
		    	}
			}
		}
    	
    	return blankProductEdges;
    }
    
    private NodeSpace initializeProduct(NodeSpace colSpace) {
    	NodeSpace rowSpace = productSpace;
		productSpace = new NodeSpace(0);
    	
    	rowIDsToProductEdges = new HashMap<String, Set<Edge>>();
		rowIDToProductNodes = new HashMap<String, Set<Node>>();
		
		for (Edge rowEdge : rowSpace.getEdges()) {
			String rowIDs = rowEdge.getTailID() + rowEdge.getHeadID();
			
			if (!rowIDsToProductEdges.containsKey(rowIDs)) {
				rowIDsToProductEdges.put(rowIDs, new HashSet<Edge>());
			}
			
			if (!rowIDToProductNodes.containsKey(rowEdge.getTailID())) {
				rowIDToProductNodes.put(rowEdge.getTailID(), new HashSet<Node>());
			}
			
			if (!rowIDToProductNodes.containsKey(rowEdge.getHeadID())) {
				rowIDToProductNodes.put(rowEdge.getHeadID(), new HashSet<Node>());
			}
		}
		
		colIDsToProductEdges = new HashMap<String, Set<Edge>>();
		colIDToProductNodes = new HashMap<String, Set<Node>>();
		
		for (Edge colEdge : colSpace.getEdges()) {
			String colIDs = colEdge.getTailID() + colEdge.getHeadID();

			if (!colIDsToProductEdges.containsKey(colIDs)) {
				colIDsToProductEdges.put(colIDs, new HashSet<Edge>());
			}
			
			if (!colIDToProductNodes.containsKey(colEdge.getTailID())) {
				colIDToProductNodes.put(colEdge.getTailID(), new HashSet<Node>());
			}
			
			if (!colIDToProductNodes.containsKey(colEdge.getHeadID())) {
				colIDToProductNodes.put(colEdge.getHeadID(), new HashSet<Node>());
			}
		}
		
		crossIDToProductNode = new HashMap<String, Node>();
		
		return rowSpace;
    }
    
    private void crossEdges(Set<Edge> rowEdges, Set<Edge> colEdges, int tolerance, int degree, 
    		Set<String> roles) {
    	for (Edge rowEdge : rowEdges) {
			String rowIDs = rowEdge.getTailID() + rowEdge.getHeadID();
			
			for (Edge colEdge : colEdges) {
				String colIDs = colEdge.getTailID() + colEdge.getHeadID();
				
				if (rowEdge.isMatching(colEdge, tolerance, roles)) {
					Node productTail = crossNodes(rowEdge.getTail(), colEdge.getTail(), degree);

					Node productHead = crossNodes(rowEdge.getHead(), colEdge.getHead(), degree);

					Edge productEdge = productTail.copyEdge(colEdge, productHead);

					if (tolerance == 1) {
						productEdge.intersectWithEdge(rowEdge);
					} else if (tolerance > 1) {
						productEdge.unionWithEdge(rowEdge);
					}
					
					rowIDsToProductEdges.get(rowIDs).add(productEdge);
					colIDsToProductEdges.get(colIDs).add(productEdge);
				}
			}
    	}
    }
    
    private void crossNodes(Set<Node> rowNodes, Set<Node> colNodes, int degree) {
    	for (Node rowNode : rowNodes) {
    		for (Node colNode : colNodes) {
    			crossNodes(rowNode, colNode, degree);
    		}
    	}
    }
    
    private Node crossNodes(Node rowNode, Node colNode, int degree) {
    	Node productNode;
    	
    	String crossID = rowNode.getNodeID() + colNode.getNodeID();
    	
		if (crossIDToProductNode.containsKey(crossID)) {
			productNode = crossIDToProductNode.get(crossID);
		} else {
			productNode = productSpace.createNode();
			
			if ((degree == 1 && (rowNode.isStartNode() || colNode.isStartNode()))
					|| (degree == 2 && rowNode.isStartNode() && colNode.isStartNode())) {
				productNode.addNodeType(NodeType.START.getValue());
			}
			
			if ((degree == 1 && (rowNode.isAcceptNode() || colNode.isAcceptNode()))
					|| (degree == 2 && rowNode.isAcceptNode() && colNode.isAcceptNode())) {
				productNode.addNodeType(NodeType.ACCEPT.getValue());
			}
			
			crossIDToProductNode.put(crossID, productNode);
			
			rowIDToProductNodes.get(rowNode.getNodeID()).add(productNode);
			colIDToProductNodes.get(colNode.getNodeID()).add(productNode);
		}
		
		return productNode;
    }
    
    private void insertBlankEdges(Set<Edge> iBlankEdges, Set<Node> jNodes, HashMap<String, Set<Node>> iToProductNodes,
    		HashMap<String, Set<Node>> jToProductNodes, List<Set<Edge>> blankProductEdges) {
    	for (Edge iEdge : iBlankEdges) {
    		List<Set<Node>> projectedHeads = projectBlankEdge(iEdge, iToProductNodes);
    		
    		if (!projectedHeads.isEmpty()) {
    			for (Node jNode : jNodes) {
    				Set<Node> productTails = new HashSet<Node>(iToProductNodes.get(iEdge.getTailID()));
    				productTails.retainAll(jToProductNodes.get(jNode.getNodeID()));

    				if (!productTails.isEmpty()) {
    					Set<Node> productHeads = new HashSet<Node>(projectedHeads.get(0));
    					productHeads.retainAll(jToProductNodes.get(jNode.getNodeID()));

    					if (!productHeads.isEmpty()) {
    						linkProductNodes(productTails, productHeads, blankProductEdges);
    					}
    				}
    			}
    		}
		}
    }
    
    private List<Set<Node>> projectBlankEdge(Edge blankEdge, HashMap<String, Set<Node>> idToProductNodes) {
    	List<Set<Node>> productHeads = new LinkedList<Set<Node>>();
    	productHeads.add(new HashSet<Node>());
    	
    	Stack<Edge> edgeStack = new Stack<Edge>();
    	
    	edgeStack.push(blankEdge);
    	
    	Set<Edge> headEdges = new HashSet<Edge>();
    	
    	while (!edgeStack.isEmpty()) {
    		Edge tempEdge = edgeStack.pop();
    		
    		Set<Node> tempProductHeads = idToProductNodes.get(tempEdge.getHeadID());
    		
    		if (tempProductHeads.isEmpty()) {
    			if (tempEdge.getHead().hasEdges()) {
    				for (Edge headEdge : tempEdge.getHead().getEdges()) {
    					if (headEdge.isBlank()) {
    						edgeStack.push(headEdge);
    					}
    				}
    			}
    		} else {
    			productHeads.get(productHeads.size() - 1).addAll(tempProductHeads);
    			
    			if (tempEdge.getHead().hasEdges()) {
    				for (Edge headEdge : tempEdge.getHead().getEdges()) {
    					if (headEdge.isBlank()) {
    						headEdges.add(headEdge);
    					}
    				}
    			}
    		}
    		
    		if (edgeStack.isEmpty() && !headEdges.isEmpty()) {
    			for (Edge headEdge : headEdges) {
    				edgeStack.push(headEdge);
    			}
    			
    			headEdges.clear();
    			
    			productHeads.add(new HashSet<Node>());
    		}
    	}
    	
    	return productHeads;
    }
    
    private void multiCrossBlankEdges(Set<Edge> blankRowEdges, Set<Edge> blankColEdges, 
    		List<Set<Edge>> blankProductEdges) {
    	for (Edge rowEdge : blankRowEdges) {
			for (Edge colEdge : blankColEdges) {
				Set<Node> productTails = new HashSet<Node>(rowIDToProductNodes.get(rowEdge.getTailID()));
				productTails.retainAll(colIDToProductNodes.get(colEdge.getTailID()));
				
				if (!productTails.isEmpty()) {
					List<Set<Node>> rowProjectedHeads = projectBlankEdge(rowEdge, rowIDToProductNodes);
					List<Set<Node>> colProjectedHeads = projectBlankEdge(colEdge, colIDToProductNodes);
					
					List<Set<Node>> iProjectedHeads;
					List<Set<Node>> jProjectedHeads;

					if (rowProjectedHeads.size() > colProjectedHeads.size()) {
						iProjectedHeads = rowProjectedHeads;
						jProjectedHeads = colProjectedHeads;
					} else {
						iProjectedHeads = colProjectedHeads;
						jProjectedHeads = rowProjectedHeads;
					}
					
					int bound = iProjectedHeads.size() + jProjectedHeads.size() - 1;
			    	
					for (int k = 1; k <= bound; k++) {
						for (int i = Math.min(iProjectedHeads.size() - 1, k - 1), j = Math.max(0, k - iProjectedHeads.size());
								j < Math.min(jProjectedHeads.size(), k); i--, j++) {
							Set<Node> productHeads = new HashSet<Node>(iProjectedHeads.get(i));
							productHeads.retainAll(jProjectedHeads.get(j));

							if (!productHeads.isEmpty()) {
								linkProductNodes(productTails, productHeads, blankProductEdges);
								
								bound = k;
							}
						}
					}
				}
			}
		}
    }
    
    private void crossBlankEdges(Set<Edge> blankRowEdges, Set<Edge> blankColEdges, List<Set<Edge>> blankProductEdges) {
    	for (Edge rowEdge : blankRowEdges) {
    		for (Edge colEdge : blankColEdges) {
    			Set<Node> productTails = new HashSet<Node>(rowIDToProductNodes.get(rowEdge.getTailID()));
				productTails.retainAll(colIDToProductNodes.get(colEdge.getTailID()));
				
				if (!productTails.isEmpty()) {
					Set<Node> productHeads = new HashSet<Node>(rowIDToProductNodes.get(rowEdge.getHeadID()));
					productHeads.retainAll(colIDToProductNodes.get(colEdge.getHeadID()));
					
					if (!productHeads.isEmpty()) {
						Edge productEdge = linkProductNodes(productTails, productHeads, blankProductEdges);
						
						rowIDsToProductEdges.get(rowEdge.getTailID() + rowEdge.getHeadID()).add(productEdge);
						colIDsToProductEdges.get(colEdge.getTailID() + colEdge.getHeadID()).add(productEdge);
					}
				}
    		}
    	}
    }
    
    private void updateIDToProductNodes(HashMap<String, Set<Node>> idToProductNodes) {
    	for (String id : idToProductNodes.keySet()) {
    		Set<Node> deletedNodes = new HashSet<Node>();
    		
    		for (Node productNode : idToProductNodes.get(id)) {
    			if (!productSpace.hasNode(productNode)) {
    				deletedNodes.add(productNode);
    			}
    		}
    		
    		idToProductNodes.get(id).removeAll(deletedNodes);
    	}
    }
    
    private Edge linkProductNodes(Set<Node> productTails, Set<Node> productHeads, List<Set<Edge>> blankProductEdges) {
    	Edge productEdge;
    	
    	if (productTails.size() == 1 && productHeads.size() == 1) {
    		Node productTail = productTails.iterator().next();
			Node productHead = productHeads.iterator().next();
			
			productEdge = productTail.createEdge(productHead);
    	} else if (productTails.size() > 1 && productHeads.size() == 1) {
    		Node primaryTail = productSpace.createNode();
			
			for (Node productTail : productTails) {
				blankProductEdges.get(0).add(productTail.createEdge(primaryTail));
			}
			
			Node productHead = productHeads.iterator().next();
			
			productEdge = primaryTail.createEdge(productHead);
    	} else if (productTails.size() == 1 && productHeads.size() > 1) {
    		Node primaryHead = productSpace.createNode();
    		
    		for (Node productHead : productHeads) {
    			blankProductEdges.get(0).add(primaryHead.createEdge(productHead));
    		}
    		
    		Node productTail = productTails.iterator().next();
    		
    		productEdge = productTail.createEdge(primaryHead);
    	} else if (productTails.size() > 1 && productHeads.size() > 1) {
    		Node primaryTail = productSpace.createNode();
    		Node primaryHead = productSpace.createNode();

    		for (Node productTail : productTails) {
    			blankProductEdges.get(0).add(productTail.createEdge(primaryTail));
    		}
    		for (Node productHead : productHeads) {
    			blankProductEdges.get(0).add(primaryHead.createEdge(productHead));
    		}
    		
    		productEdge = primaryTail.createEdge(primaryHead);
    	} else {
    		return null;
    	}
    	
    	blankProductEdges.get(1).add(productEdge);
    	
    	return productEdge;
    }
    
    public List<Set<Edge>> applyModifiedStrong(NodeSpace colSpace, int tolerance, Set<String> roles) {
    	NodeSpace rowSpace = productSpace;
    	
    	List<Set<Edge>> blankEdges = applyTensor(colSpace, tolerance, 1, roles, true);
    	
    	rowIDToDiffNode = new HashMap<String, Node>();
    	colIDToDiffNode = new HashMap<String, Node>();
    	
    	Set<Edge> rowEdges = new HashSet<Edge>(rowSpace.getEdges());
    	Set<Edge> colEdges = new HashSet<Edge>(colSpace.getEdges());
    	
    	Set<Edge> blankRowEdges = rowSpace.getBlankEdges();
    	Set<Edge> blankColEdges = colSpace.getBlankEdges();
    	
    	rowEdges.removeAll(blankRowEdges);
    	colEdges.removeAll(blankColEdges);

    	if (tolerance == 0 || tolerance == 1) {
    		strongDiffEdges(rowEdges, roles, rowIDsToProductEdges, rowIDToDiffNode);
        	strongDiffEdges(colEdges, roles, colIDsToProductEdges, colIDToDiffNode);
    	} else if (tolerance > 1) {
    		weakDiffEdges(rowEdges, roles, rowIDsToProductEdges, rowIDToDiffNode);
        	weakDiffEdges(colEdges, roles, colIDsToProductEdges, colIDToDiffNode);
    	}
    	
    	HashMap<String, Set<Edge>> nodeIDToIncomingEdges = productSpace.mapNodeIDsToIncomingEdges();
    	
    	Set<Edge> shimEdges = new HashSet<Edge>();
    	
    	insertProductShims(rowIDToProductNodes, shimEdges, nodeIDToIncomingEdges);
    	insertProductShims(colIDToProductNodes, shimEdges, nodeIDToIncomingEdges);
    	
    	blankEdges.get(1).addAll(shimEdges);
    	
    	diffBlankEdges(blankRowEdges, rowIDsToProductEdges, rowIDToDiffNode, blankEdges.get(1));
    	diffBlankEdges(blankColEdges, colIDsToProductEdges, colIDToDiffNode, blankEdges.get(1));
    	
    	nodeIDToIncomingEdges = productSpace.mapNodeIDsToIncomingEdges();
    	
    	Set<Edge> feedbackEdges = productSpace.getFeedbackEdges(nodeIDToIncomingEdges);
    	
    	Set<Edge> linkerEdges = new HashSet<Edge>();
    	
    	insertDiffLinkers(rowIDToDiffNode, rowIDToProductNodes, linkerEdges, feedbackEdges, nodeIDToIncomingEdges);
    	insertDiffLinkers(colIDToDiffNode, colIDToProductNodes, linkerEdges, feedbackEdges, nodeIDToIncomingEdges);
    	
    	blankEdges.get(0).addAll(linkerEdges);
    	
    	insertFeedbackSpacers(feedbackEdges, linkerEdges, blankEdges.get(1), nodeIDToIncomingEdges);
    	
    	return blankEdges;
    }
    
    @SuppressWarnings("unchecked")
	private void insertProductShims(HashMap<String, Set<Node>> idToProductNodes, Set<Edge> shimEdges,
			HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	for (String id : idToProductNodes.keySet()) {
    		Set<Node> productTails = new HashSet<Node>();
    		Set<Node> productHeads = new HashSet<Node>();
    		
    		for (Node productNode : idToProductNodes.get(id)) {
    			Set<Edge> productEdges = productNode.getOtherEdges(shimEdges);
    			Set<Edge> incomingProductEdges = productNode.getOtherIncomingEdges(nodeIDToIncomingEdges, shimEdges);
    			
    			if (!productEdges.isEmpty() && incomingProductEdges.isEmpty()) {
    				productHeads.add(productNode);
    			} else if (productEdges.isEmpty() && !incomingProductEdges.isEmpty()) {
    				productTails.add(productNode);
    			}
    		}
    		
    		for (Node productTail : productTails) {
    			for (Node productHead : productHeads) {
					shimEdges.add(productTail.createEdge(productHead));
    			}
    		}
    	}
    }
    
    @SuppressWarnings("unchecked")
	private void insertFeedbackSpacers(Set<Edge> feedbackEdges, Set<Edge> linkerEdges,
    		Set<Edge> spacerEdges, HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	Set<Node> feedbackNodes = new HashSet<Node>();

    	for (Edge feedbackEdge : feedbackEdges) {
    		feedbackNodes.add(feedbackEdge.getTail());
    		feedbackNodes.add(feedbackEdge.getHead());
    	}

    	for (Node feedbackNode : feedbackNodes) {
    		Set<Edge> outLinkers = feedbackNode.getEdges(linkerEdges);
    		Set<Edge> incomingEdges = feedbackNode.getOtherIncomingEdges(nodeIDToIncomingEdges, feedbackEdges);

    		if (!outLinkers.isEmpty() && !incomingEdges.isEmpty()) {
    			Node spacerNode = productSpace.copyNode(feedbackNode);

    			for (Edge incomingEdge : incomingEdges) {
    				incomingEdge.setHead(spacerNode);
    			}

    			for (Edge outLinker : outLinkers) {
    				outLinker.setTail(spacerNode);

    				spacerNode.addEdge(outLinker);
    			}

    			feedbackNode.deleteEdges(outLinkers);
    			
    			spacerEdges.add(spacerNode.createEdge(feedbackNode));
    		}
    		
    		Set<Edge> inLinkers = feedbackNode.getIncomingEdges(linkerEdges, nodeIDToIncomingEdges);
    		Set<Edge> outgoingEdges = feedbackNode.getOtherEdges(feedbackEdges);
    		
    		if (!inLinkers.isEmpty() && !outgoingEdges.isEmpty()) {
    			Node spacerNode = productSpace.copyNode(feedbackNode);

    			Set<Edge> deletedEdges = new HashSet<Edge>();

    			for (Edge outgoingEdge : outgoingEdges) {
    				outgoingEdge.setTail(spacerNode);

    				spacerNode.addEdge(outgoingEdge);

    				deletedEdges.add(outgoingEdge);
    			}

    			feedbackNode.deleteEdges(deletedEdges);

    			for (Edge inLinker : inLinkers) {
    				inLinker.setHead(spacerNode);
    			}

    			spacerEdges.add(feedbackNode.createEdge(spacerNode));
    		}
    	}
    }
    
    private void strongDiffEdges(Set<Edge> edges, Set<String> roles, 
    		HashMap<String, Set<Edge>> idsToProductEdges, HashMap<String, Node> idToDiffNode) {
    	for (Edge edge : edges) {
    		Set<Edge> productEdges = idsToProductEdges.get(edge.getTailID() + edge.getHeadID());
    		Edge diffEdge = edge.copy();

    		for (Edge productEdge : productEdges) {
    			diffEdge.diffWithEdge(productEdge);
    		}

    		if (diffEdge.hasComponentIDs()) {
    			Node diffTail;

    			if (idToDiffNode.containsKey(edge.getTailID())) {
    				diffTail = idToDiffNode.get(edge.getTailID());
    			} else {
    				diffTail = productSpace.copyNode(edge.getTail());

    				idToDiffNode.put(edge.getTailID(), diffTail);
    			}
    			diffEdge.setTail(diffTail);

    			Node diffHead;

    			if (idToDiffNode.containsKey(edge.getHeadID())) {
    				diffHead = idToDiffNode.get(edge.getHeadID());
    			} else {
    				diffHead = productSpace.copyNode(edge.getHead());

    				idToDiffNode.put(edge.getHeadID(), diffHead);
    			}
    			diffEdge.setHead(diffHead);

    			diffTail.addEdge(diffEdge);
    		}
    	}
    }
    
    private void weakDiffEdges(Set<Edge> edges, Set<String> roles, 
    		HashMap<String, Set<Edge>> idsToProductEdges, HashMap<String, Node> idToDiffNode) {
    	for (Edge edge : edges) {
    		Set<Edge> productEdges = idsToProductEdges.get(edge.getTailID() + edge.getHeadID());

    		boolean isMatching = false;

    		Iterator<Edge> productEdgerator = productEdges.iterator();

    		while (productEdgerator.hasNext() && !isMatching) {
    			isMatching = edge.isMatching(productEdgerator.next(), 1, roles);
    		}

    		if (!isMatching) {
    			Node diffTail;

    			if (idToDiffNode.containsKey(edge.getTailID())) {
    				diffTail = idToDiffNode.get(edge.getTailID());
    			} else {
    				if (productEdges.isEmpty()) {
    					diffTail = productSpace.copyNode(edge.getTail());
    				} else {
    					diffTail = productSpace.createNode();
    				}

    				idToDiffNode.put(edge.getTailID(), diffTail);
    			}

    			Node diffHead;

    			if (idToDiffNode.containsKey(edge.getHeadID())) {
    				diffHead = idToDiffNode.get(edge.getHeadID());
    			} else {
    				if (productEdges.isEmpty()) {
    					diffHead = productSpace.copyNode(edge.getHead());
    				} else {
    					diffHead = productSpace.createNode();
    				}

    				idToDiffNode.put(edge.getHeadID(), diffHead);
    			}

    			diffTail.copyEdge(edge, diffHead);
    		}
    	}
    }
    
    private void diffBlankEdges(Set<Edge> edges, HashMap<String, Set<Edge>> idsToProductEdges,
    		HashMap<String, Node> idToDiffNode, Set<Edge> blankEdges) {
    	for (Edge blankEdge : edges) {
    		Set<Edge> productEdges = idsToProductEdges.get(blankEdge.getTailID() + blankEdge.getHeadID());
    		
    		Set<Edge> revProductEdges;
    		if (idsToProductEdges.containsKey(blankEdge.getHeadID() + blankEdge.getTailID())) {
    			revProductEdges = idsToProductEdges.get(blankEdge.getHeadID() + blankEdge.getTailID());
    		} else {
    			revProductEdges = new HashSet<Edge>();
    		}
    		
    		if (!productEdges.isEmpty() || !revProductEdges.isEmpty()) {
    			for (Edge productEdge : productEdges) {
    				if (!productEdge.getTail().hasBlankEdge(productEdge.getHead())) {
    					blankEdges.add(productEdge.getTail().createEdge(productEdge.getHead()));
    				}
    			}
    			
    			for (Edge reverseProductEdge : revProductEdges) {
    				if (!reverseProductEdge.getHead().hasBlankEdge(reverseProductEdge.getTail())) {
    					blankEdges.add(reverseProductEdge.getHead().createEdge(reverseProductEdge.getTail()));
    				}
    			}

    			if (idToDiffNode.containsKey(blankEdge.getTailID()) 
    					&& idToDiffNode.containsKey(blankEdge.getHeadID())) {
    				Node diffTail = idToDiffNode.get(blankEdge.getTailID());
    				Node diffHead = idToDiffNode.get(blankEdge.getHeadID());

    				if (diffTail.hasEdge(diffHead) || diffHead.hasEdge(diffTail)) {
    					blankEdges.add(diffTail.createEdge(diffHead));
    				}
    			}
    		} else {
    			Node diffTail;

    			if (idToDiffNode.containsKey(blankEdge.getTailID())) {
    				diffTail = idToDiffNode.get(blankEdge.getTailID());
    			} else {
    				diffTail = productSpace.copyNode(blankEdge.getTail());
    				
    				idToDiffNode.put(blankEdge.getTailID(), diffTail);
    			}
    			
    			Node diffHead;

    			if (idToDiffNode.containsKey(blankEdge.getHeadID())) {
    				diffHead = idToDiffNode.get(blankEdge.getHeadID());
    			} else {
    				diffHead = productSpace.copyNode(blankEdge.getHead());
    				
    				idToDiffNode.put(blankEdge.getHeadID(), diffHead);
    			}
    			
    			blankEdges.add(diffTail.createEdge(diffHead));
    		}
    	}
    }
    
    @SuppressWarnings("unchecked")
	private void insertDiffLinkers(HashMap<String, Node> idToDiffNode, HashMap<String, Set<Node>> idToProductNodes,  
			Set<Edge> linkerEdges, Set<Edge> feedbackEdges, HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	for (String id : idToProductNodes.keySet()) {
    		if (idToDiffNode.containsKey(id)) {
    			Node diffNode = idToDiffNode.get(id);
    			
    			Set<Edge> diffEdges = diffNode.getOtherEdges(feedbackEdges, linkerEdges);
    			Set<Edge> incomingDiffEdges = diffNode.getOtherIncomingEdges(nodeIDToIncomingEdges, feedbackEdges, 
    					linkerEdges);
    			
    			if (diffEdges.isEmpty() && !incomingDiffEdges.isEmpty()) {
    				for (Node productNode : idToProductNodes.get(id)) {
    					if (productNode.hasEdges()) {
    						Edge linkerEdge = diffNode.createEdge(productNode);
    						
    						linkerEdges.add(linkerEdge);
    						
    						nodeIDToIncomingEdges.get(productNode.getNodeID()).add(linkerEdge);
    					}
    				}
    			} else if (!diffEdges.isEmpty() && incomingDiffEdges.isEmpty()) {
    				for (Node productNode : idToProductNodes.get(id)) {
    					if (productNode.hasIncomingEdges(nodeIDToIncomingEdges)) {
    						Edge linkerEdge = productNode.createEdge(diffNode);
    						
    						linkerEdges.add(linkerEdge);
    						
    						nodeIDToIncomingEdges.get(diffNode.getNodeID()).add(linkerEdge);
    					}
    				}
    			} else {
    				for (Node productNode : idToProductNodes.get(id)) {
    					Set<Edge> productEdges = productNode.getOtherEdges(feedbackEdges, linkerEdges);
            			Set<Edge> incomingProductEdges = productNode.getOtherIncomingEdges(nodeIDToIncomingEdges, feedbackEdges, 
            					linkerEdges);
            			
    					if (productEdges.isEmpty() && !incomingProductEdges.isEmpty()) {
    						Edge linkerEdge = productNode.createEdge(diffNode);
    						
    						linkerEdges.add(linkerEdge);
    						
    						nodeIDToIncomingEdges.get(diffNode.getNodeID()).add(linkerEdge);
    					} else if (!productEdges.isEmpty() && incomingProductEdges.isEmpty()) {
    						Edge linkerEdge = diffNode.createEdge(productNode);
    						
    						linkerEdges.add(linkerEdge);
    						
    						nodeIDToIncomingEdges.get(productNode.getNodeID()).add(linkerEdge);
    					}
    				}
    			}
    		}
    	}
    }
    
    public enum ProductType {
        TENSOR("tensor"),
        CARTESIAN("cartesian"),
        STRONG("strong");

        private final String value;

        ProductType(String value) { 
        	this.value = value;
        }

        public String getValue() {
        	return value; 
        }
    }
    
}
