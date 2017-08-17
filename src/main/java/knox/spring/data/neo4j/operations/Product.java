package knox.spring.data.neo4j.operations;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Node.NodeType;

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
    
    
    
    
    
    public void modifiedStrong(int tolerance, Set<String> roles) {
    	tensor(tolerance, 1, roles);
    	
    	if (tolerance == 1) {
    		diffEdges();
    	}
    	
    	joinProductNodes(tolerance, roles);
    	
    	joinDiffNodes(rowNodes, rowToProductNodes);
    	
    	joinDiffNodes(colNodes, colToProductNodes);
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
    
    private void diffEdge(Edge edge, int index, List<Node> nodes, Node productNode, 
    		Edge productEdge, HashMap<Integer, Set<Node>> indexToProductNodes) {
    	if (edge.hasSharedComponents(productEdge)
				&& !edge.hasSameComponents(productEdge)) {
    		Node diffHead = productSpace.copyNode(nodes.get(index));

    		indexToProductNodes.get(index).add(diffHead);

    		Edge diffEdge = productNode.copyEdge(edge, diffHead);

    		diffEdge.diffWithEdge(productEdge);
    	}
    }
    
    private void diffEdges() {
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
    							
    							diffEdge(rowEdge, r, rowNodes, productNode, productEdge,
    									rowToProductNodes);
    							
    							diffEdge(colEdge, c, colNodes, productNode, productEdge,
    									colToProductNodes);
    						}
    					}
    				}
    			}
    		}
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
    
    private Set<Node> joinDiffNode(int i, List<Node> nodes, HashMap<Integer, 
    		Set<Node>> indexToProductNodes) {
    	Set<Node> productNodes = new HashSet<Node>();

		if (indexToProductNodes.containsKey(i)) {
			productNodes.addAll(indexToProductNodes.get(i));
		} else {
			Node diffNode = productSpace.copyNode(nodes.get(i));
			
			indexToProductNodes.put(i, new HashSet<Node>());
			
			indexToProductNodes.get(i).add(diffNode);
			
			productNodes.add(diffNode);
		}
		
		return productNodes;
    }
    
    private void joinDiffNodes(List<Node> nodes, HashMap<Integer, Set<Node>> indexToProductNodes) {
    	for (int i = 0; i < nodes.size(); i++) {
    		Set<Node> productNodes = joinDiffNode(i, nodes, indexToProductNodes);

    		for (Edge edge : nodes.get(i).getEdges()) {
    			int j = locateNode(edge.getHead(), i, nodes);
    			
    			Set<Node> productHeads = joinDiffNode(j, nodes, indexToProductNodes);
    			
    			for (Node productNode : productNodes) {
    				for (Node productHead : productHeads) {
    					productNode.copyEdge(edge, productHead);
    				}
    			}
    		}
    	}
    }
    
    private void joinProductNodes(int tolerance, Set<String> roles) {
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
