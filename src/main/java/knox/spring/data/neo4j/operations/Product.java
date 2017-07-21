package knox.spring.data.neo4j.operations;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

import java.util.*;

public class Product {
	private List<Node> rowNodes;
	
	private List<Node> colNodes;
	
	private List<List<Node>> productNodes;
	
	private HashMap<String, Integer> idToRowIndex = new HashMap<String, Integer>();
	
	private HashMap<String, Integer> idToColIndex = new HashMap<String, Integer>();
	
	private NodeSpace productSpace;
	
	public Product(NodeSpace rowSpace, NodeSpace colSpace) {
		productSpace = new NodeSpace();
		
		this.rowNodes = rowSpace.orderNodes();
		
		this.colNodes = colSpace.orderNodes();
		
		productNodes = new ArrayList<List<Node>>(rowNodes.size());
		
		idToRowIndex = new HashMap<String, Integer>();
		
		idToColIndex = new HashMap<String, Integer>();
    	
    	for (int i = 0; i < rowNodes.size(); i++) {
			productNodes.add(new ArrayList<Node>(colNodes.size()));

			for (int j = 0; j < colNodes.size(); j++) {
				Node node;
				
				if (rowNodes.get(i).isAcceptNode() || colNodes.get(j).isAcceptNode()) {
					node = productSpace.createAcceptNode();
					
					productNodes.get(i).add(node);
				} else if (rowNodes.get(i).isStartNode() && colNodes.get(j).isStartNode()) {
					node = productSpace.createStartNode();
					
					productNodes.get(i).add(node);
				} else {
					node = productSpace.createNode();
					
					productNodes.get(i).add(node);
				}
				
				idToRowIndex.put(node.getNodeID(), new Integer(i));
				
				idToColIndex.put(node.getNodeID(), new Integer(j));
			}
		}
	}
	
	public void connect(String type, int tolerance, boolean isModified) {
		if (type.equals(ProductType.TENSOR.getValue())) {
			if (isModified) {
				modifiedTensor(tolerance);
			} else {
				tensor(tolerance);
			}
		} else if (type.equals(ProductType.CARTESIAN.getValue())) {
			cartesian();
		} else if (type.equals(ProductType.STRONG.getValue())) {
			if (isModified) {
				modifiedStrong(tolerance);
			} else {
				strong(tolerance);
			}
		}
	}
	

    public NodeSpace getProductSpace() {
    	return productSpace;
    }
    
    public List<Set<Edge>> cartesian() {
    	List<Set<Edge>> orthogonalEdges = new ArrayList<Set<Edge>>(2);
    	
    	orthogonalEdges.add(new HashSet<Edge>());

		for (int i = 0; i < rowNodes.size(); i++) {
			if (rowNodes.get(i).hasEdges()) {
				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
					int r = locateNode(rowEdge.getHead(), i, rowNodes);

					for (int j = 0; j < colNodes.size(); j++) {
						orthogonalEdges.get(0).add(productNodes.get(i).get(j).copyEdge(rowEdge, 
								productNodes.get(r).get(j)));
					}
				}
			}
		}

		orthogonalEdges.add(new HashSet<Edge>());

		for (int j = 0; j < colNodes.size(); j++) {
			if (colNodes.get(j).hasEdges()) {
				for (Edge colEdge : colNodes.get(j).getEdges()) {
					int c = locateNode(colEdge.getHead(), j, colNodes);

					for (int i = 0; i < rowNodes.size(); i++) {
						orthogonalEdges.get(1).add(productNodes.get(i).get(j).copyEdge(colEdge, 
								productNodes.get(i).get(c)));
					}
				}
			}
		}
		
		return orthogonalEdges;
    }
    
    public void modifiedStrong(int tolerance) {
    	tensor(tolerance);
    	
    	productSpace.deleteUnconnectedNodes();
    	
    	List<Partition> partitions = partitionNodesBySource();
    	
    	for (Partition partition : partitions) {
    		HashMap<Integer, Node> rowToProductNode = new HashMap<Integer, Node>();
    		
    		for (int i = 0; i < rowNodes.size(); i++) {
    			Node productNode;

    			if (partition.hasRowNode(i)) {
    				productNode = partition.getRowNode(i);
    			} else if (rowToProductNode.containsKey(i)) {
    				productNode = rowToProductNode.get(i);
    			} else {
    				productNode = productSpace.copyNode(rowNodes.get(i));

    				rowToProductNode.put(i, productNode);
    			}

    			if (rowNodes.get(i).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					int r = locateNode(rowEdge.getHead(), i, rowNodes);

    					Node productHead;

    					if (partition.hasRowNode(r)) {
    						productHead = partition.getRowNode(r);
    					} else if (rowToProductNode.containsKey(r)) {
    						productHead = rowToProductNode.get(r);
    					} else {
    						productHead = productSpace.copyNode(rowEdge.getHead());

    						rowToProductNode.put(r, productHead);
    					}

    					productNode.copyEdge(rowEdge, productHead);
        			}
        		}
        	}
    		
    		HashMap<Integer, Node> colToProductNode = new HashMap<Integer, Node>();
    		
    		for (int j = 0; j < colNodes.size(); j++) {
    			Node productNode;

    			if (partition.hasColumnNode(j)) {
    				productNode = partition.getColumnNode(j);
    			} else if (colToProductNode.containsKey(j)) {
    				productNode = colToProductNode.get(j);
    			} else {
    				productNode = productSpace.copyNode(colNodes.get(j));

    				colToProductNode.put(j, productNode);
    			}

    			if (colNodes.get(j).hasEdges()) {
    				for (Edge colEdge : colNodes.get(j).getEdges()) {
    					int c = locateNode(colEdge.getHead(), j, colNodes);

    					Node productHead;

    					if (partition.hasColumnNode(c)) {
    						productHead = partition.getColumnNode(c);
    					} else if (colToProductNode.containsKey(c)) {
    						productHead = colToProductNode.get(c);
    					} else {
    						productHead = productSpace.copyNode(colEdge.getHead());

    						colToProductNode.put(c, productHead);
    					}

    					productNode.copyEdge(colEdge, productHead);
    				}
        		}
        	}
    	}
    	
    	if (productSpace.getStartNodes().size() > 1) {
    		Union union = new Union(productSpace);
    		
    		if (partitions.size() > 1) {
    			union.connect(true);
    		} else {
    			union.connect(false);
    		}
    	}
    }
    
    public void modifiedTensor(int tolerance) {
    	tensor(tolerance);
    	
    	productSpace.deleteUnconnectedNodes();
    	
    	productSpace.labelSourceNodesStart();
    	
    	productSpace.labelSinkNodesAccept();
    	
    	Union union = new Union(productSpace);
    	
    	union.connect(true);
    }
    
    public void strong(int tolerance) {
    	tensor(tolerance);
    	
    	cartesian();
    }
    
    public void tensor(int tolerance) {
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() 
    					&& colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (rowEdge.isMatchingTo(colEdge, tolerance)) {
    							Node productNode = productNodes.get(i).get(j);

    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);

    							Edge productEdge = productNode.copyEdge(colEdge, 
    									productNodes.get(r).get(c));

    							if (tolerance == 0 || tolerance > 1 && tolerance <= 4) {
    								productEdge.unionWithEdge(rowEdge);
    							} else if (tolerance == 1) {
    								productEdge.intersectWithEdge(rowEdge);
    							}
    						}
    					}
    				}
    			}
    		}
    	}
    }
    
    private Integer getColumn(Node node) {
    	if (idToColIndex.containsKey(node.getNodeID())) {
    		return idToColIndex.get(node.getNodeID());
    	} else {
    		return new Integer(-1);
    	}
    }
    
    private Integer getRow(Node node) {
    	if (idToRowIndex.containsKey(node.getNodeID())) {
    		return idToRowIndex.get(node.getNodeID());
    	} else {
    		return new Integer(-1);
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
    
    private List<Partition> partitionNodesBySource() {
    	List<Partition> partitions = new LinkedList<Partition>();
    	
    	for (Node sourceNode : productSpace.getSourceNodes()) {
    		Partition partition = new Partition();
    		
    		partition.addNode(getRow(sourceNode), getColumn(sourceNode), sourceNode);
    		
    		int partitionIndex = -1;
    		
    		Stack<Node> nodeStack = new Stack<Node>();
    		
    		nodeStack.push(sourceNode);
    		 
    		while (!nodeStack.isEmpty()) {
        		Node node = nodeStack.pop();
        		
        		if (node.hasEdges()) {
        			for (Edge edge : node.getEdges()) {
        				
        				
        				if (partitionIndex < 0) {
        					int i = 0;
        					
        					while (i < partitions.size()) {
        						if (partitions.get(i).hasNode(edge.getHead())) {
        							partitions.get(i).union(partition);
        							
        							partitionIndex = i;

        							i = partitions.size();
        						} else {
        							i++;
        						}
        					}
        				}
        				
        				if (partitionIndex < 0) {
        					if (!partition.hasNode(edge.getHead())) {
        						nodeStack.push(edge.getHead());
            					
            					partition.addNode(getRow(edge.getHead()), getColumn(edge.getHead()),
            							edge.getHead());
        					}
        				} else if (!partitions.get(partitionIndex).hasNode(edge.getHead())) {
        					nodeStack.push(edge.getHead());

        					partitions.get(partitionIndex).addNode(getRow(edge.getHead()), 
        							getColumn(edge.getHead()), edge.getHead());
        				}
        			}
        		}
        	}
    		
    		if (partitionIndex < 0) {
    			partitions.add(partition);
    		}
    	}
    	
    	return partitions;
    }
    
    private class Partition {
    	
    	private Set<Node> nodes = new HashSet<Node>();
    	
    	private HashMap<Integer, Node> rowToNode = new HashMap<Integer, Node>();
    	
    	private HashMap<Integer, Node> colToNode = new HashMap<Integer, Node>();
    	
    	public Partition() {
    		
    	}
    	
    	public void addNode(Integer row, Integer col, Node node) {
    		nodes.add(node);
    		
    		rowToNode.put(row, node);
    		
    		colToNode.put(col, node);
    	}
    	
    	public Set<Node> getNodes() {
    		return nodes;
    	}
    	
    	public boolean hasNode(Node node) {
    		return nodes.contains(node);
    	}
    	
    	public boolean hasRowNode(Integer row) {
    		return rowToNode.containsKey(row);
    	}
    	
    	public boolean hasColumnNode(Integer col) {
    		return colToNode.containsKey(col);
    	}
    	
    	public Node getRowNode(Integer row) {
    		return rowToNode.get(row);
    	}
    	
    	public Node getColumnNode(Integer col) {
    		return colToNode.get(col);
    	}
    	
    	public HashMap<Integer, Node> getRowToNode() {
    		return rowToNode;
    	}
    	
    	public HashMap<Integer, Node> getColToNode() {
    		return colToNode;
    	}
    	
    	public void union(Partition partition) {
    		nodes.addAll(partition.getNodes());
    		
    		rowToNode.putAll(partition.getRowToNode());
    		
    		colToNode.putAll(partition.getColToNode());
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
