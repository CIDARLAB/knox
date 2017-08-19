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
	
//	private HashMap<Integer, Set<Node>> rowToProductNodes;
//	
//	private HashMap<Integer, Set<Node>> colToProductNodes;
	
	private NodeSpace productSpace;
	
	private static final Logger LOG = LoggerFactory.getLogger(Product.class);
	
	public Product(NodeSpace rowSpace, NodeSpace colSpace) {
		productSpace = new NodeSpace(0);
		
		this.rowNodes = rowSpace.depthFirstTraversal();
		
		this.colNodes = colSpace.depthFirstTraversal();
		
//		rowToProductNodes = new HashMap<Integer, Set<Node>>();
//		
//		colToProductNodes = new HashMap<Integer, Set<Node>>();
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
    
    
    public List<Partition> unionNonOverlapPartitions(List<Partition> partitions) {
    	List<Partition> overlapPartitions = new LinkedList<Partition>();
    	
    	for (int i = 0; i < partitions.size(); i++) {
    		boolean onRepeat = false;
    		
    		while (onRepeat && i < partitions.size()) {
    			for (Partition overlapPartition : overlapPartitions) {
    				if (partitions.get(i).hasOverlap(overlapPartition)) {
    					overlapPartitions.add(partitions.remove(i));

    					onRepeat = true;
    				}
    			}
    		}
    		
    		int backI = i;
    		
    		for (int k = i + 1; k < partitions.size(); k++) {
    			if (partitions.get(i).hasOverlap(partitions.get(k))) {
    				overlapPartitions.add(partitions.remove(k));
    				
    				overlapPartitions.add(partitions.remove(i));

    				i = backI - 1;
    				
    				k = partitions.size();
    			}
    		}
    	}
    	
    	for (int i = 1; i < partitions.size(); i++) {
    		partitions.get(0).union(partitions.get(i));
    	}
    	
    	overlapPartitions.add(partitions.get(0));
    	
    	return overlapPartitions;
    }
    
    
    public void modifiedStrong(int tolerance, Set<String> roles) {
    	List<Partition> partitions = tensor2(tolerance, 1, roles);
    	
    	partitions = unionNonOverlapPartitions(partitions);
    	
    	for (Partition partition : partitions) {
    		partition.projectNodes(rowNodes, colNodes);
    	}
    	
//    	List<Set<Node>> nodePartitions = productSpace.partition();
    	
//    	if (tolerance == 1) {
//    		diffEdges();
//    	}
    	
//    	joinProductNodes(tolerance, roles);
    	
//    	joinDiffNodes(rowNodes, rowToProductNodes);
//    	
//    	joinDiffNodes(colNodes, colToProductNodes);
    }
    
    public void strong(int tolerance, Set<String> roles) {
//    	tensor(tolerance, 1, roles);
    	
    	cartesian();
    }
    
    public List<Partition> tensor2(int tolerance, int degree, Set<String> roles) {
    	List<Partition> partitions = new LinkedList<Partition>();
    	
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() 
    					&& colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (rowEdge.isMatchingTo(colEdge, tolerance, roles)) {
    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);
    							
    							int p = locateProductPartition(i, j, partitions);
    							
    							int q = locateProductPartition(r, c, partitions);
    							
    							Node productNode;
    							
    							Node productHead;
    							
    							if (p >= 0 && q >= 0) {
    								if (p > q) {
    									partitions.get(q).union(partitions.remove(p));
    								} else if (p < q) {
    									partitions.get(p).union(partitions.remove(q));
    								}
    								
    								productNode = partitions.get(p).getProductNode(i, j);
									
									productHead = partitions.get(p).getProductNode(r, c);
    								
    							} else if (p >= 0 && q < 0) {
    								productNode = partitions.get(p).getProductNode(i, j);
    								
    								productHead = crossNodes(r, c, degree);
    								
    								partitions.get(p).addProductNode(r, c, productHead);
    							} else if (p < 0 && q >= 0) {
    								productNode = crossNodes(i, j, degree);
    								
    								partitions.get(q).addProductNode(i, j, productNode);
    								
    								productHead = partitions.get(q).getProductNode(r, c);
    							} else {
    								Partition partition = new Partition();
    								
    								productNode = crossNodes(i, j, degree);
    								
    								productHead = crossNodes(r, c, degree);
    								
    								partition.addProductNode(i, j, productNode);
    								
    								partition.addProductNode(r, c, productHead);
    								
    								partitions.add(partition);
    							}
    							
    							Edge productEdge = productNode.copyEdge(colEdge, 
    									productHead);
								
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
    	
    	return partitions;
    }
    
    public void tensor(int tolerance, int degree, Set<String> roles) {
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() 
    					&& colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (rowEdge.isMatchingTo(colEdge, tolerance, roles)) {
//    							if (!hasProductNode(i, j)) {
//    								crossNodes(i, j, degree);
//    							}
//
//    							int r = locateNode(rowEdge.getHead(), i, 
//    									rowNodes);
//
//    							int c = locateNode(colEdge.getHead(), j,
//    									colNodes);
//    							
//    							if (!hasProductNode(r, c)) {
//    								crossNodes(r, c, degree);
//    							}
//    							
//    							Node productNode = getProductNode(i, j);
//    							
//    							Edge productEdge = productNode.copyEdge(colEdge, 
//    									getProductNode(r, c));
//
//    							if (tolerance == 1) {
//    								productEdge.intersectWithEdge(rowEdge);
//    							} else if (tolerance != 0) {
//    								productEdge.unionWithEdge(rowEdge);
//    							} 
    						}
    					}
    				}
    			}
    		}
    	}
    }
    
//    private void addProductNode(int i, int j, Node productNode) {
//    	if (!rowToProductNodes.containsKey(i)) {
//    		rowToProductNodes.put(i, new HashSet<Node>());
//    	} 
//    	
//    	rowToProductNodes.get(i).add(productNode);
//    	
//    	if (!colToProductNodes.containsKey(j)) {
//    		colToProductNodes.put(j, new HashSet<Node>());
//    	}
//    	
//    	colToProductNodes.get(j).add(productNode);
//    }
    
    private Node crossNodes(int i, int j, int degree) {
//    	if (!hasProductNode(i, j)) {
			Node productNode = productSpace.createNode();
			
			if ((degree == 1 && (rowNodes.get(i).isStartNode() || colNodes.get(j).isStartNode()))
					|| (degree == 2 && rowNodes.get(i).isStartNode() && colNodes.get(j).isStartNode())) {
				productNode.addNodeType(NodeType.START.getValue());
			}
			
			
			if ((degree == 1 && (rowNodes.get(i).isAcceptNode() || colNodes.get(j).isAcceptNode()))
					|| (degree == 2 && rowNodes.get(i).isAcceptNode() && colNodes.get(j).isAcceptNode())) {
				productNode.addNodeType(NodeType.ACCEPT.getValue());
			}
			
//			addProductNode(i, j, productNode);
//		}
			return productNode;
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
    
//    private void diffEdges() {
//    	for (int i = 0; i < rowNodes.size(); i++) {
//    		for (int j = 0; j < colNodes.size(); j++) {
//    			if (rowNodes.get(i).hasEdges() 
//    					&& colNodes.get(j).hasEdges()) {
//    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
//    					for (Edge colEdge : colNodes.get(j).getEdges()) {
//    						if (rowEdge.hasSharedComponents(colEdge)
//    								&& !rowEdge.hasSameComponents(colEdge)) {
//    							int r = locateNode(rowEdge.getHead(), i, 
//    									rowNodes);
//
//    							int c = locateNode(colEdge.getHead(), j,
//    									colNodes);
//
//    							Node productNode = getProductNode(i, j);
//    							
//    							Edge productEdge = productNode.getLabeledEdge();
//    							
//    							diffEdge(rowEdge, r, rowNodes, productNode, productEdge,
//    									rowToProductNodes);
//    							
//    							diffEdge(colEdge, c, colNodes, productNode, productEdge,
//    									colToProductNodes);
//    						}
//    					}
//    				}
//    			}
//    		}
//    	}
//    }
   
//    private Node getProductNode(int i, int j) {
//    	if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
//    		Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));
//    		
//        	productNodes.retainAll(colToProductNodes.get(j));
//        	
//        	return productNodes.iterator().next();
//    	} else {
//    		return null;
//    	}
//    }
    
//    private boolean hasProductNode(int i, int j) {
//    	if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
//    		Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));
//    		
//        	productNodes.retainAll(colToProductNodes.get(j));
//        	
//        	return productNodes.size() == 1;
//    	} else {
//    		return false;
//    	}
//    }
    
//    private Set<Node> joinDiffNode(int i, List<Node> nodes, HashMap<Integer, 
//    		Set<Node>> indexToProductNodes) {
//    	Set<Node> productNodes = new HashSet<Node>();
//
//		if (indexToProductNodes.containsKey(i)) {
//			productNodes.addAll(indexToProductNodes.get(i));
//		} else {
//			Node diffNode = productSpace.copyNode(nodes.get(i));
//			
//			indexToProductNodes.put(i, new HashSet<Node>());
//			
//			indexToProductNodes.get(i).add(diffNode);
//			
//			productNodes.add(diffNode);
//		}
//		
//		return productNodes;
//    }
//    
//    private void joinDiffNodes(List<Node> nodes, HashMap<Integer, Set<Node>> indexToProductNodes) {
//    	for (int i = 0; i < nodes.size(); i++) {
//    		Set<Node> productNodes = joinDiffNode(i, nodes, indexToProductNodes);
//
//    		for (Edge edge : nodes.get(i).getEdges()) {
//    			int j = locateNode(edge.getHead(), i, nodes);
//    			
//    			Set<Node> productHeads = joinDiffNode(j, nodes, indexToProductNodes);
//    			
//    			for (Node productNode : productNodes) {
//    				for (Node productHead : productHeads) {
//    					productNode.copyEdge(edge, productHead);
//    				}
//    			}
//    		}
//    	}
//    }
    
//    private void joinProductNodes(int tolerance, Set<String> roles) {
//    	for (int i = 0; i < rowNodes.size(); i++) {
//    		for (int j = 0; j < colNodes.size(); j++) {
//    			if (rowNodes.get(i).hasEdges() 
//    					&& colNodes.get(j).hasEdges()) {
//    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
//    					for (Edge colEdge : colNodes.get(j).getEdges()) {
//    						if (!rowEdge.isMatchingTo(colEdge, tolerance, roles)) {
//    							int r = locateNode(rowEdge.getHead(), i, 
//    									rowNodes);
//
//    							int c = locateNode(colEdge.getHead(), j,
//    									colNodes);
//    							
//    							if (hasProductNode(i, j) && hasProductNode(r, c)) {
//    								Edge productEdge = getProductNode(i, j).copyEdge(colEdge, 
//        									getProductNode(r, c));
//    								
//    								productEdge.unionWithEdge(rowEdge);
//    							} 
//    						}
//    					}
//    				}
//    			}
//    		}
//    	}
//    }
    
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
    
    private int locateProductPartition(int i, int j, List<Partition> partitions) {
    	for (int p = 0; p < partitions.size(); p++) {
    		if (partitions.get(p).hasProductNode(i, j)) {
    			return p;
    		}
    	}
    	
    	return -1;
    }
    
    private class Partition {
    	private HashMap<Integer, Set<Node>> rowToProductNodes;
    	
    	private HashMap<Integer, Set<Node>> colToProductNodes;
    	
    	public Partition() {
    		rowToProductNodes = new HashMap<Integer, Set<Node>>();
    		
    		colToProductNodes = new HashMap<Integer, Set<Node>>();
    	}
    	
    	public void addProductNode(int i, int j, Node productNode) {
    		if (!rowToProductNodes.containsKey(i)) {
        		rowToProductNodes.put(i, new HashSet<Node>());
        	} 
        	
        	rowToProductNodes.get(i).add(productNode);
        	
        	if (!colToProductNodes.containsKey(j)) {
        		colToProductNodes.put(j, new HashSet<Node>());
        	}
        	
        	colToProductNodes.get(j).add(productNode);
        }

    	public Node getProductNode(int i, int j) {
    		if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
        		Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));
        		
            	productNodes.retainAll(colToProductNodes.get(j));
            	
            	return productNodes.iterator().next();
        	} else {
        		return null;
        	}
    	}
    	
    	public Set<Integer> getRowIndices() {
    		return new HashSet<Integer>(rowToProductNodes.keySet());
    	}
    	
    	public Set<Integer> getColumnIndices() {
    		return new HashSet<Integer>(colToProductNodes.keySet());
    	}
    	
    	public HashMap<Integer, Set<Node>> getRowToProductNodes() {
    		return rowToProductNodes;
    	}
    	
    	public HashMap<Integer, Set<Node>> getColumnToProductNodes() {
    		return colToProductNodes;
    	}
    	
    	public boolean hasOverlap(Partition partition) {
    		Set<Integer> rowIndices = partition.getRowIndices();
    		
    		rowIndices.retainAll(rowToProductNodes.keySet());
    		
    		if (!rowIndices.isEmpty()) {
    			return true;
    		}
    		
    		Set<Integer> colIndices = partition.getColumnIndices();
    		
    		colIndices.retainAll(colToProductNodes.keySet());
    		
    		return !colIndices.isEmpty();
    	}
    	
    	public boolean hasProductNode(int i, int j) {
    		if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
        		Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));
        		
            	productNodes.retainAll(colToProductNodes.get(j));
            	
            	return productNodes.size() == 1;
        	} else {
        		return false;
        	}
    	}
    	
    	private Set<Node> projectNode(int i, List<Node> nodes, HashMap<Integer, 
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
    	
    	public void projectNodes(List<Node> rowNodes, List<Node> colNodes) {
    		projectNodes(rowNodes, rowToProductNodes);
    		
    		projectNodes(colNodes, colToProductNodes);
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
    	
    	public void union(Partition partition) {
    		rowToProductNodes.putAll(partition.getRowToProductNodes());
    		
    		colToProductNodes.putAll(partition.getColumnToProductNodes());
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
