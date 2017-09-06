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
	
	private NodeSpace productSpace;
	
	private static final Logger LOG = LoggerFactory.getLogger(Product.class);
	
	public Product(NodeSpace rowSpace, NodeSpace colSpace) {
		productSpace = new NodeSpace(0);
		
		this.rowNodes = rowSpace.depthFirstTraversal();
		
		this.colNodes = colSpace.depthFirstTraversal();
	}
	
    public NodeSpace getProductSpace() {
    	return productSpace;
    }
    
//    public void cartesian() {
//    	for (int i = 0; i < rowNodes.size(); i++) {
//    		for (int j = 0; j < colNodes.size(); j++) {
//    			crossNodes(i, j, 1);
//    		}
//    	}
//    }
    
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
    
    public List<Set<Node>> modifiedStrong(int tolerance, int degree, Set<String> roles) {
    	List<Set<Node>> globalDiffNodes = new ArrayList<Set<Node>>(2);
    	
    	globalDiffNodes.add(new HashSet<Node>());
    	
    	globalDiffNodes.add(new HashSet<Node>());
    	
    	List<Partition> partitions = tensor(tolerance, degree, roles);
    	
    	partitions = unionNonOverlapPartitions(partitions);
    	
    	for (Partition partition : partitions) {
    		HashMap<Integer, Node> rowToDiffNode = new HashMap<Integer, Node>();
    		
    		HashMap<Integer, Node> colToDiffNode = new HashMap<Integer, Node>();
    		
    		List<Set<Node>> localDiffNodes;
    		
    		if (tolerance == 1) {
    			localDiffNodes = partition.prosectNodes(rowToDiffNode, colToDiffNode, roles);
    			
    			globalDiffNodes.get(0).addAll(localDiffNodes.get(0));
        		
        		globalDiffNodes.get(1).addAll(localDiffNodes.get(1));
    		}
    		
    		localDiffNodes = partition.projectNodes(rowToDiffNode, colToDiffNode, tolerance, roles);
    	
    		globalDiffNodes.get(0).addAll(localDiffNodes.get(0));
    		
    		globalDiffNodes.get(1).addAll(localDiffNodes.get(1));
    	}
    	
    	return globalDiffNodes;
    }
    
//    public void strong(int tolerance, int degree, Set<String> roles) {
//    	tensor(tolerance, degree, roles);
//    	
//    	cartesian();
//    }
    
    public List<Partition> tensor(int tolerance, int degree, Set<String> roles) {
    	List<Partition> partitions = new LinkedList<Partition>();
    	
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() && colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (rowEdge.isMatching(colEdge, tolerance, roles)) {
    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);
    							
    							int p = locatePartition(i, j, partitions);
    							
    							int q = locatePartition(r, c, partitions);
    							
    							Node productNode;
    							
    							Node productHead;
    							
    							if (p >= 0 && q >= 0) {
    								if (p > q) {
    									partitions.get(q).union(partitions.remove(p));
    									
    									productNode = partitions.get(q).getProductNode(i, j);
    									
    									productHead = partitions.get(q).getProductNode(r, c);
    								} else {
    									if (q > p) {
    										partitions.get(p).union(partitions.remove(q));
    									}
    									
    									productNode = partitions.get(p).getProductNode(i, j);
    									
    									productHead = partitions.get(p).getProductNode(r, c);
    								}
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
    
    private Node crossNodes(int i, int j, int degree) {
			Node productNode = productSpace.createNode();
			
			if ((degree == 1 && (rowNodes.get(i).isStartNode() || colNodes.get(j).isStartNode()))
					|| (degree == 2 && rowNodes.get(i).isStartNode() && colNodes.get(j).isStartNode())) {
				productNode.addNodeType(NodeType.START.getValue());
			}
			
			
			if ((degree == 1 && (rowNodes.get(i).isAcceptNode() || colNodes.get(j).isAcceptNode()))
					|| (degree == 2 && rowNodes.get(i).isAcceptNode() && colNodes.get(j).isAcceptNode())) {
				productNode.addNodeType(NodeType.ACCEPT.getValue());
			}
			
			return productNode;
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
    
    private int locatePartition(int i, int j, List<Partition> partitions) {
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

//    	private void connectProductNodes(int tolerance, Set<String> roles) {
//    		for (int i = 0; i < rowNodes.size(); i++) {
//    			for (int j = 0; j < colNodes.size(); j++) {
//    				if (rowNodes.get(i).hasEdges() && colNodes.get(j).hasEdges()) {
//    					for (Edge rowEdge : rowNodes.get(i).getEdges()) {
//    						for (Edge colEdge : colNodes.get(j).getEdges()) {
//    							if (!rowEdge.isMatchingTo(colEdge, tolerance, roles)) {
//    								int r = locateNode(rowEdge.getHead(), i, rowNodes);
//
//    								int c = locateNode(colEdge.getHead(), j, colNodes);
//
//    								if (hasProductNode(i, j) && hasProductNode(r, c)) {
//    									Edge productEdge = getProductNode(i, j).copyEdge(colEdge, 
//    											getProductNode(r, c));
//
//    									productEdge.unionWithEdge(rowEdge);
//    								} 
//    							}
//    						}
//    					}
//    				}
//    			}
//    		}
//    	}

    	public Node getProductNode(int i, int j) {
    		if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
    			Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));

    			productNodes.retainAll(colToProductNodes.get(j));

    			return productNodes.iterator().next();
    		} else {
    			return null;
    		}
    	}

    	public Set<Node> getRowProductNodes(int i) {
    		return rowToProductNodes.get(i);
    	}
    	
    	public Set<Node> getColumnProductNodes(int j) {
    		return colToProductNodes.get(j);
    	}

    	public Set<Integer> getRowIndices() {
    		return new HashSet<Integer>(rowToProductNodes.keySet());
    	}

    	public Set<Integer> getColumnIndices() {
    		return new HashSet<Integer>(colToProductNodes.keySet());
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
    	
    	private List<Set<Node>> prosectNodes(HashMap<Integer, Node> rowToDiffNode, 
    			HashMap<Integer, Node> colToDiffNode, Set<String> roles) {
    		List<Set<Node>> diffNodes = new ArrayList<Set<Node>>(2);
    		
    		diffNodes.add(new HashSet<Node>());
    		
    		diffNodes.add(new HashSet<Node>());
    		
    		for (int i = 0; i < rowNodes.size(); i++) {
    			for (int j = 0; j < colNodes.size(); j++) {
    				if (rowNodes.get(i).hasEdges() && colNodes.get(j).hasEdges()) {
    					for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    						for (Edge colEdge : colNodes.get(j).getEdges()) {
    							if (rowEdge.isLabeled() 
    									&& rowEdge.isMatching(colEdge, 1, roles)
    									&& !rowEdge.isMatching(colEdge, 0, roles)) {
    								int r = locateNode(rowEdge.getHead(), i, rowNodes);

    								int c = locateNode(colEdge.getHead(), j, colNodes);

    								Node productNode = getProductNode(i, j);
    								
    								Node productHead = getProductNode(r, c);

    								Edge productEdge = productNode.getLabeledEdges(productHead,
    										rowEdge.getOrientation()).iterator().next();
    								
    								if (rowEdge.isMatching(productEdge, 1, roles) 
    					    				&& !rowEdge.isMatching(productEdge, 0, roles)) {
    									Node diffNode = projectNode(i, rowNodes, rowToDiffNode);
    									
    									Node diffHead = projectNode(r, rowNodes, rowToDiffNode);
    									
    									Edge diffEdge = diffNode.copyEdge(rowEdge, diffHead);

    					    			diffEdge.diffWithEdge(productEdge);
    					    			
    					    			diffNodes.get(0).add(diffNode);
    					    			
    					    			diffNodes.get(0).add(diffHead);
    								}
    								
    								if (colEdge.isMatching(productEdge, 1, roles) 
    					    				&& !colEdge.isMatching(productEdge, 0, roles)) {
    									Node diffNode = projectNode(j, colNodes, colToDiffNode);
    									
    									Node diffHead = projectNode(c, colNodes, colToDiffNode);
    									
    									Edge diffEdge = diffNode.copyEdge(colEdge, diffHead);

    					    			diffEdge.diffWithEdge(productEdge);
    					    			
    					    			diffNodes.get(1).add(diffNode);
    					    			
    					    			diffNodes.get(1).add(diffHead);
    								}
    							}
    						}
    					}
    				}
    			}
    		}
    		
    		return diffNodes;
    	}
    	
    	private Node projectNode(int i, List<Node> nodes, 
    			HashMap<Integer, Node> indexToDiffNode) {
    		if (indexToDiffNode.containsKey(i)) {
    			return indexToDiffNode.get(i);
    		} else {
    			Node diffNode = productSpace.copyNode(nodes.get(i));
    			
    			indexToDiffNode.put(i, diffNode);
    			
    			return diffNode;
    		}
    	}

    	private Set<Node> projectNode(int i, List<Node> nodes, 
    			HashMap<Integer, Set<Node>> indexToProductNodes,
    			HashMap<Integer, Node> indexToDiffNode, Set<Node> diffNodes) {
    		Set<Node> productNodes = new HashSet<Node>();

    		if (indexToProductNodes.containsKey(i)) {
    			productNodes.addAll(indexToProductNodes.get(i));
    			
    			if (indexToDiffNode.containsKey(i)) {
    				diffNodes.add(indexToDiffNode.get(i));
    			}
    		} else if (indexToDiffNode.containsKey(i)) {
    			diffNodes.add(indexToDiffNode.get(i));
    		} else {
    			Node diffNode = productSpace.copyNode(nodes.get(i));

    			indexToDiffNode.put(i, diffNode);

    			diffNodes.add(diffNode);
    		}

    		return productNodes;
    	}
    	
    	public List<Set<Node>> projectNodes(HashMap<Integer, Node> rowToDiffNode, 
    			HashMap<Integer, Node> colToDiffNode, int tolerance, Set<String> roles) {
    		List<Set<Node>> diffNodes = new ArrayList<Set<Node>>(2);
    		
    		diffNodes.add(projectNodes(rowNodes, rowToProductNodes, rowToDiffNode, tolerance, roles));

    		diffNodes.add(projectNodes(colNodes, colToProductNodes, colToDiffNode, tolerance, roles));
    		
    		return diffNodes;
    	}

    	private Set<Node> projectNodes(List<Node> nodes, HashMap<Integer, Set<Node>> indexToProductNodes,
    			HashMap<Integer, Node> indexToDiffNode, int tolerance, Set<String> roles) {
    		Set<Edge> diffEdges = new HashSet<Edge>();
    		
    		for (int i = 0; i < nodes.size(); i++) {
    			Set<Node> diffNodes = new HashSet<Node>();
    			
    			Set<Node> productNodes = projectNode(i, nodes, indexToProductNodes,
    					indexToDiffNode, diffNodes);

    			for (Edge edge : nodes.get(i).getEdges()) {
    				int ii = locateNode(edge.getHead(), i, nodes);
    				
    				Set<Node> diffHeads = new HashSet<Node>();
    				
    				Set<Node> productHeads = projectNode(ii, nodes, indexToProductNodes,
    						indexToDiffNode, diffHeads);

    				for (Node productNode : productNodes) {
    					for (Node productHead : productHeads) {
    						if (!productNode.hasMatchingEdge(edge, tolerance, roles)) {
    							diffEdges.add(productNode.copyEdge(edge, productHead));
    						}
    					}
    					
    					if (diffNodes.isEmpty()) {
    						for (Node diffHead : diffHeads) {
    							diffEdges.add(productNode.copyEdge(edge, diffHead));
    						}
    					}
    				}
    				
    				for (Node diffNode : diffNodes) {
    					for (Node diffHead : diffHeads) {
    						if (!diffNode.hasMatchingEdge(edge, diffHead, tolerance, roles)) {
    							diffEdges.add(diffNode.copyEdge(edge, diffHead));
    						}
    					}
    					
    					if (diffHeads.isEmpty()) {
    						for (Node productHead : productHeads) {
        						diffEdges.add(diffNode.copyEdge(edge, productHead));
        					}
    					}
    				}
    			}
    		}
    		
    		Set<Node> diffNodes = new HashSet<Node>();
    		
    		for (Edge diffEdge : diffEdges) {
    			diffNodes.add(diffEdge.getTail());
    			
    			diffNodes.add(diffEdge.getHead());
    		}
    		
    		return diffNodes;
    	}

    	public void union(Partition partition) {
    		for (int i : partition.getRowIndices()) {
    			if (rowToProductNodes.containsKey(i)) {
    				rowToProductNodes.get(i).addAll(partition.getRowProductNodes(i));
    			} else {
    				rowToProductNodes.put(i, partition.getRowProductNodes(i));
    			}
    		}
    		
    		for (int j : partition.getColumnIndices()) {
    			if (colToProductNodes.containsKey(j)) {
    				colToProductNodes.get(j).addAll(partition.getColumnProductNodes(j));
    			} else {
    				colToProductNodes.put(j, partition.getColumnProductNodes(j));
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
