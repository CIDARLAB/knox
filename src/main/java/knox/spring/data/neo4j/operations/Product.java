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
    
    public NodeSpace applyTensor(NodeSpace colSpace, int tolerance, int degree, Set<String> roles) {
    	if (productSpace.hasNodes() && colSpace.hasNodes()) {
    		NodeSpace rowSpace = productSpace;
    		productSpace = new NodeSpace(0);
    		
			crossIDToProductNode = new HashMap<String, Node>();
			
			rowIDsToProductEdges = new HashMap<String, Set<Edge>>();
			colIDsToProductEdges = new HashMap<String, Set<Edge>>();
			
			rowIDToProductNodes = new HashMap<String, Set<Node>>();
			colIDToProductNodes = new HashMap<String, Set<Node>>();
			
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
					
					if (rowEdge.isMatching(colEdge, tolerance, roles)) {
						Node productTail;
						
						String crossTailID = rowEdge.getTailID() + colEdge.getTailID();
						if (crossIDToProductNode.containsKey(crossTailID)) {
							productTail = crossIDToProductNode.get(crossTailID);
						} else {
							productTail = crossNodes(rowEdge.getTail(), colEdge.getTail(), degree);
							
							crossIDToProductNode.put(crossTailID, productTail);
						}

						Node productHead;

						String crossHeadID = rowEdge.getHeadID() + colEdge.getHeadID();
						if (crossIDToProductNode.containsKey(crossHeadID)) {
							productHead = crossIDToProductNode.get(crossHeadID);
						} else {
							productHead = crossNodes(rowEdge.getHead(), colEdge.getHead(), degree);
							
							crossIDToProductNode.put(crossHeadID, productHead);
						}

						Edge productEdge = productTail.copyEdge(colEdge, productHead);

						if (tolerance == 1) {
							productEdge.intersectWithEdge(rowEdge);
						} else if (tolerance > 1) {
							productEdge.unionWithEdge(rowEdge);
						}
						
						rowIDsToProductEdges.get(rowIDs).add(productEdge);
						colIDsToProductEdges.get(colIDs).add(productEdge);
						
						rowIDToProductNodes.get(rowEdge.getTailID()).add(productTail);
						rowIDToProductNodes.get(rowEdge.getHeadID()).add(productHead);
						
						colIDToProductNodes.get(colEdge.getTailID()).add(productTail);
						colIDToProductNodes.get(colEdge.getHeadID()).add(productHead);
					}
				}
	    	}
	    	
	    	if (degree == 0) {
	    		productSpace.labelSourceNodesStart();

	    		productSpace.labelSinkNodesAccept();
	    	} else if (degree == 2) {
	    		productSpace.deleteUnacceptableNodes();
	    	}
	    	
	    	return rowSpace;
		} else {
			return productSpace;
		}
    }
    
    public List<Set<Edge>> applyModifiedStrong(NodeSpace colSpace, int tolerance, Set<String> roles) {
    	List<Set<Edge>> blankEdges = new LinkedList<Set<Edge>>();
    	blankEdges.add(new HashSet<Edge>());
    	blankEdges.add(new HashSet<Edge>());
    	
    	NodeSpace rowSpace = applyTensor(colSpace, tolerance, 1, roles);
    	
    	rowIDToDiffNode = new HashMap<String, Node>();
    	colIDToDiffNode = new HashMap<String, Node>();

    	for (Edge rowEdge : rowSpace.getEdges()) {
    		String rowIDs = rowEdge.getTailID() + rowEdge.getHeadID();

    		diffProductEdges(rowIDsToProductEdges.get(rowIDs), rowEdge, tolerance, roles, 
    				rowIDToProductNodes, rowIDToDiffNode, blankEdges.get(0), blankEdges.get(1));
    	}

    	for (Edge colEdge : colSpace.getEdges()) {
    		String colIDs = colEdge.getTailID() + colEdge.getHeadID();

    		diffProductEdges(colIDsToProductEdges.get(colIDs), colEdge, tolerance, roles,
    				colIDToProductNodes, colIDToDiffNode, blankEdges.get(0), blankEdges.get(1));
    	}
    	
    	if (tolerance == 1) {
    		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = productSpace.mapNodeIDsToIncomingEdges();
    		
    		for (String rowID : rowIDToProductNodes.keySet()) {
    			if (rowIDToDiffNode.containsKey(rowID) && rowIDToProductNodes.containsKey(rowID)) {
    				linkDiffNode(rowIDToDiffNode.get(rowID), rowIDToProductNodes.get(rowID), 
    						nodeIDToIncomingEdges, blankEdges.get(0));
    			}
    		}
    		
    		for (String colID : colIDToProductNodes.keySet()) {
    			if (colIDToDiffNode.containsKey(colID) && colIDToProductNodes.containsKey(colID)) {
    				linkDiffNode(colIDToDiffNode.get(colID), colIDToProductNodes.get(colID), 
    						nodeIDToIncomingEdges, blankEdges.get(0));
    			}
    		}
    	}
    	
    	return blankEdges;
    }
    
    private void linkDiffNode(Node diffNode, Set<Node> productNodes, 
    		HashMap<String, Set<Edge>> nodeIDToIncomingEdges, Set<Edge> linkerEdges) {
    	for (Node productNode : productNodes) {
    		if (!diffNode.hasEdges()) {
    			linkerEdges.add(diffNode.createEdge(productNode));
    		} else if (!nodeIDToIncomingEdges.containsKey(diffNode.getNodeID())) {
    			linkerEdges.add(productNode.createEdge(diffNode));
    		}
    	}
    }
    
    private void diffProductEdges(Set<Edge> productEdges, Edge edge, int tolerance, Set<String> roles,
    		HashMap<String, Set<Node>> idToProductNodes, HashMap<String, Node> idToDiffNode,
    		Set<Edge> linkerEdges, Set<Edge> blankEdges) {
    	if (tolerance == 0 || tolerance == 1) {
    		Edge diffEdge = edge.copy();

    		if (edge.isBlank()) {
    			blankEdges.add(diffEdge);
    		} else {
    			for (Edge productEdge : productEdges) {
    				diffEdge.diffWithEdge(productEdge);
    			}
    		}

    		if (diffEdge.hasComponentIDs() || edge.isBlank()) {
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
    			
    			if (productEdges.isEmpty() || edge.isBlank()) {
    				if (idToProductNodes.containsKey(edge.getTailID())) {
    					for (Node productNode : idToProductNodes.get(edge.getTailID())) {
    						linkerEdges.add(productNode.createEdge(diffTail));
    					}
    				}
    				
    				if (idToProductNodes.containsKey(edge.getHeadID())) {
    					for (Node productNode : idToProductNodes.get(edge.getHeadID())) {
    						linkerEdges.add(diffHead.createEdge(productNode));
    					}
    				}
    			}
    		}
    	} else if (tolerance > 1) {
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
    
    private Node crossNodes(Node rowNode, Node colNode, int degree) {
		Node productNode = productSpace.createNode();
		
		if ((degree == 1 && (rowNode.isStartNode() || colNode.isStartNode()))
				|| (degree == 2 && rowNode.isStartNode() && colNode.isStartNode())) {
			productNode.addNodeType(NodeType.START.getValue());
		}
		
		if ((degree == 1 && (rowNode.isAcceptNode() || colNode.isAcceptNode()))
				|| (degree == 2 && rowNode.isAcceptNode() && colNode.isAcceptNode())) {
			productNode.addNodeType(NodeType.ACCEPT.getValue());
		}
		
		return productNode;
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
