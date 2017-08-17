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
    
    private Set<Node> projectNode(int i, List<Node> nodes, HashMap<Integer, 
    		Set<Node>> indexToProductNodes) {
    	Set<Node> productNodes = new HashSet<Node>();

		if (indexToProductNodes.containsKey(i)) {
			productNodes.addAll(indexToProductNodes.get(i));
		} else {
			Node productNode = productSpace.copyNode(nodes.get(i));
			
			indexToProductNodes.put(i, new HashSet<Node>());
			
			indexToProductNodes.get(i).add(productNode);
			
			productNodes.add(productNode);
		}
		
		return productNodes;
    }
    
    private void projectNodes(List<Node> nodes, HashMap<Integer, Set<Node>> indexToProductNodes) {
    	for (int i = 0; i < nodes.size(); i++) {
    		Set<Node> productNodes = projectNode(i, nodes, indexToProductNodes);

    		for (Edge edge : nodes.get(i).getEdges()) {
    			int j = locateNode(edge.getHead(), i, nodes);
    			
    			Set<Node> productHeads = projectNode(j, nodes, indexToProductNodes);
    			
    			for (Node productNode : productNodes) {
    				for (Node productHead : productHeads) {
    					productNode.copyEdge(edge, productHead);
    				}
    			}
    		}
    	}
    }
    
    public void modifiedStrong(int tolerance, Set<String> roles) {
    	tensor(tolerance, 1, roles);
    	
    	if (tolerance == 1) {
    		tensor2();
    	}
    	
    	tensor3(tolerance, roles);
    	
    	projectNodes(rowNodes, rowToProductNodes);
    	
    	projectNodes(colNodes, colToProductNodes);
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
    							
    							Node productNode = getProductNode(i, j);
    							
    							Edge productEdge = productNode.copyEdge(colEdge, 
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
    
    public void tensor2() {
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() 
    					&& colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (rowEdge.hasSharedComponents(colEdge)
    								&& !rowEdge.hasSameComponents(colEdge)) {
    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);

    							Node productNode = getProductNode(i, j);
    							
    							Edge productEdge = productNode.getLabeledEdge();
    							
    							if (rowEdge.hasSharedComponents(productEdge)
    									&& !rowEdge.hasSameComponents(productEdge)) {
    								Node rowDiffHead = productSpace.copyNode(rowNodes.get(r));

    								rowToProductNodes.get(r).add(rowDiffHead);

    								Edge rowDiffEdge = productNode.copyEdge(rowEdge, rowDiffHead);

    								rowDiffEdge.diffWithEdge(productEdge);
    							}
    							
    							if (colEdge.hasSharedComponents(productEdge)
    									&& !colEdge.hasSameComponents(productEdge)) {
    								Node colDiffHead = productSpace.copyNode(colNodes.get(c));

    								colToProductNodes.get(c).add(colDiffHead);

    								Edge colDiffEdge = productNode.copyEdge(colEdge, colDiffHead);

    								colDiffEdge.diffWithEdge(productEdge);
    							}
    						}
    					}
    				}
    			}
    		}
    	}
    }
    	
    public void tensor3(int tolerance, Set<String> roles) {
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() 
    					&& colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (!rowEdge.isMatchingTo(colEdge, tolerance, roles)) {
    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);
    							
    							if (hasProductNode(i, j) && hasProductNode(r, c)) {
    								Edge productEdge = getProductNode(i, j).copyEdge(colEdge, 
        									getProductNode(r, c));
    								
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
