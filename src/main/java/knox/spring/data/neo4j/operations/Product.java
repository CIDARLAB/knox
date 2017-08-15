package knox.spring.data.neo4j.operations;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Node.NodeType;
import knox.spring.data.neo4j.services.DesignSpaceService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Product {
	private List<Node> rowNodes;
	
	private List<Node> colNodes;
	
	private HashMap<Integer, Set<Node>> rowToProductNodes;
	
	private HashMap<Integer, Set<Node>> colToProductNodes;
	
	private NodeSpace productSpace;
	
	private static final Logger LOG = LoggerFactory.getLogger(Product.class);
	
	public Product(NodeSpace rowSpace, NodeSpace colSpace) {
		productSpace = new NodeSpace(0);
		
		this.rowNodes = rowSpace.depthFirstTraversal();
		
		this.colNodes = colSpace.depthFirstTraversal();
		
		rowToProductNodes = new HashMap<Integer, Set<Node>>();
		
		colToProductNodes = new HashMap<Integer, Set<Node>>();
	}
	
    public NodeSpace getProductSpace() {
    	return productSpace;
    }
    
    public void cartesian() {
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			crossNodes(i, j, 1);
    		}
    	}
    }
    
    private void intersectIndices(int i, int j, Edge edge, List<Node> nodes, Set<String> roles, 
    		HashMap<Integer, Set<Node>> indexToProductNodes, 
    		HashMap<Integer, Node> indexToDiffNode) {
    	for (Node productNode : indexToProductNodes.get(i)) {
			for (Node productHead : indexToProductNodes.get(j)) {
				for (Edge productEdge : productNode.getEdges(productHead)) {
					if (!productEdge.isBlank() 
							&& productEdge.isMatchingTo(edge, 1, roles)
							&& !productEdge.isMatchingTo(edge, 0, roles)) {
						Node diffHead;

						if (indexToDiffNode.containsKey(j)) {
							diffHead = indexToDiffNode.get(j);
						} else {
							diffHead = productSpace.copyNode(nodes.get(j));

							indexToDiffNode.put(j, diffHead);
						}

						Edge diffEdge = productNode.copyEdge(edge, diffHead);

						diffEdge.intersectWithEdge(productEdge);
					}
				}
			}
		}
    }
    
    private Set<Node> projectIndex(int i, List<Node> nodes, HashMap<Integer, Set<Node>> indexToProductNodes, 
    		HashMap<Integer, Node> indexToDiffNode, Set<Node> diffNodes) {
    	Set<Node> productNodes = new HashSet<Node>();

		if (indexToProductNodes.containsKey(i)) {
			productNodes.addAll(indexToProductNodes.get(i));
		} else {
			Node diffNode;
			
			if (indexToDiffNode.containsKey(i)) {
				diffNode = indexToDiffNode.get(i);
			} else {
				diffNode = productSpace.copyNode(nodes.get(i));

				indexToDiffNode.put(i, diffNode);
			}
			
			diffNodes.add(diffNode);
		}
		
		return productNodes;
    }
    
    private void projectNodes(List<Node> nodes, HashMap<Integer, Set<Node>> indexToProductNodes,
    		int tolerance, Set<String> roles) {
    	HashMap<Integer, Node> indexToDiffNode = new HashMap<Integer, Node>();
    	
    	if (tolerance == 1) {
    		for (int i = 0; i < nodes.size(); i++) {
    			if (indexToProductNodes.containsKey(i)) {
    				for (Edge edge : nodes.get(i).getEdges()) {
    					int j = locateNode(edge.getHead(), i, nodes);

    					if (indexToProductNodes.containsKey(j)) {
    						intersectIndices(i, j, edge, nodes, roles, indexToProductNodes,
    								indexToDiffNode);
    					}
    				}
    			}
    		}
    	}

    	for (int i = 0; i < nodes.size(); i++) {
    		Set<Node> diffNodes = new HashSet<Node>();

    		Set<Node> productNodes = projectIndex(i, nodes, indexToProductNodes, 
    				indexToDiffNode, diffNodes);

    		for (Edge edge : nodes.get(i).getEdges()) {
    			int j = locateNode(edge.getHead(), i, nodes);

    			Set<Node> diffHeads = new HashSet<Node>();

    			Set<Node> productHeads = projectIndex(j, nodes, indexToProductNodes, 
    					indexToDiffNode, diffHeads);

    			for (Node diffNode : diffNodes) {
    				for (Node productHead : productHeads) {
    					diffNode.copyEdge(edge, productHead);
    				}
    			}

    			for (Node productNode : productNodes) {
    				for (Node diffHead : diffHeads) {
    					productNode.copyEdge(edge, diffHead);
    				}
    			}

    			for (Node diffNode : diffNodes) {
    				for (Node diffHead : diffHeads) {
    					diffNode.copyEdge(edge, diffHead);
    				}
    			}
    		}
    	}
    }
    
    public void modifiedStrong(int tolerance, Set<String> roles) {
    	tensor(tolerance, 1, roles);
    	
    	projectNodes(rowNodes, rowToProductNodes, tolerance, roles);
    	
    	projectNodes(colNodes, colToProductNodes, tolerance, roles);
    }
    
    public void strong(int tolerance, Set<String> roles) {
    	tensor(tolerance, 1, roles);
    	
    	cartesian();
    }
    
    public void tensor(int tolerance, int degree, Set<String> roles) {
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() 
    					&& colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (rowEdge.isMatchingTo(colEdge, tolerance, roles)) {
    							if (!hasProductNode(i, j)) {
    								crossNodes(i, j, degree);
    							}

    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);
    							
    							if (!hasProductNode(r, c)) {
    								crossNodes(r, c, degree);
    							}
    							
    							Edge productEdge = getProductNode(i, j).copyEdge(colEdge, 
    									getProductNode(r, c));

    							if (tolerance == 1) {
    								productEdge.intersectWithEdge(rowEdge);
    							} else if (tolerance != 0) {
    								productEdge.unionWithEdge(rowEdge);
    							} 
    						}
    					}
    				}
    			}
    		}
    	}
    }
    
    private void addProductNode(int i, int j, Node productNode) {
    	if (!rowToProductNodes.containsKey(i)) {
    		rowToProductNodes.put(i, new HashSet<Node>());
    	} 
    	
    	rowToProductNodes.get(i).add(productNode);
    	
    	if (!colToProductNodes.containsKey(j)) {
    		colToProductNodes.put(j, new HashSet<Node>());
    	}
    	
    	colToProductNodes.get(j).add(productNode);
    }
    
    private void crossNodes(int i, int j, int degree) {
    	if (!hasProductNode(i, j)) {
			Node productNode = productSpace.createNode();
			
//			if (rowNodes.get(i).isStartNode() && colNodes.get(j).isStartNode()) {
//				productNode.addNodeType(NodeType.START.getValue());
//			}
			
			if ((degree == 1 && (rowNodes.get(i).isStartNode() || colNodes.get(j).isStartNode()))
					|| (degree == 2 && rowNodes.get(i).isStartNode() && colNodes.get(j).isStartNode())) {
				productNode.addNodeType(NodeType.START.getValue());
			}
			
			
			if ((degree == 1 && (rowNodes.get(i).isAcceptNode() || colNodes.get(j).isAcceptNode()))
					|| (degree == 2 && rowNodes.get(i).isAcceptNode() && colNodes.get(j).isAcceptNode())) {
				productNode.addNodeType(NodeType.ACCEPT.getValue());
			}
			
			addProductNode(i, j, productNode);
		}
    }
   
    private Node getProductNode(int i, int j) {
    	if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
    		Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));
    		
        	productNodes.retainAll(colToProductNodes.get(j));
        	
        	return productNodes.iterator().next();
    	} else {
    		return null;
    	}
    }
    
    private boolean hasProductNode(int i, int j) {
    	if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
    		Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));
    		
        	productNodes.retainAll(colToProductNodes.get(j));
        	
        	return productNodes.size() == 1;
    	} else {
    		return false;
    	}
    }
    
    private int locateNode(Node node, int k, List<Node> nodes) {
    	for (int i = k; i < nodes.size(); i++) {
    		if (nodes.get(i).isIdenticalTo(node)) {
    			return i;
    		}
    	}
    	
    	for (int i = 0; i < k; i++) {
    		if (nodes.get(i).isIdenticalTo(node)) {
    			return i;
    		}
    	}
    	
    	return -1;
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
