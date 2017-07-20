package knox.spring.data.neo4j.operations;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.Node.NodeType;
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
    	
    	List<Partition> partitions = partitionNodesBySource();

		List<Set<Edge>> orthogonalEdges = cartesian();
		
		Set<Edge> rowEdges = orthogonalEdges.get(0);
		
		Set<Edge> colEdges = orthogonalEdges.get(1);
    	
    	HashMap<String, Set<Edge>> idToIncomingEdges = productSpace.mapNodeIDsToIncomingEdges();
    	
    	productSpace.clearNodes();
    	
    	for (Partition partition : partitions) {
    		Set<String> edgeCopyCodes = new HashSet<String>();
    		
    		copyPartitionToNodeSpace(partition, rowEdges, colEdges);
    		
    		copyCartesianPaths(partition, rowEdges, rowEdges, colEdges, edgeCopyCodes,
    				idToIncomingEdges, tolerance, true);
    		
    		copyCartesianPaths(partition, colEdges, rowEdges, colEdges, edgeCopyCodes,
    				idToIncomingEdges, tolerance, true);
    		
    		copyCartesianPaths(partition, rowEdges, rowEdges, colEdges, edgeCopyCodes,
    				idToIncomingEdges, tolerance, false);
    		
    		copyCartesianPaths(partition, colEdges, rowEdges, colEdges, edgeCopyCodes,
    				idToIncomingEdges, tolerance, false);
    	}
    	
    	if (!productNodes.isEmpty() && !productNodes.get(0).isEmpty()) {
        	if (partitions.isEmpty()) {
        		Set<String> edgeCopyCodes = new HashSet<String>();
        		
        		Partition partition = new Partition(productNodes.get(0).get(0));
        		
        		copyPartitionToNodeSpace(partition, rowEdges, colEdges);
        		
        		copyCartesianPaths(partition, rowEdges, rowEdges, colEdges, 
        				edgeCopyCodes, idToIncomingEdges, tolerance, true);

        		copyCartesianPaths(partition, colEdges, rowEdges, colEdges, 
        				edgeCopyCodes, idToIncomingEdges, tolerance, true);
        			
        	} else if (productSpace.getStartNodes().size() > 1) {
        		Union union = new Union(productSpace);
        		
        		if (partitions.size() > 1) {
        			union.connect(true);
        		} else {
        			union.connect(false);
        		}
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
    							Node outputNode = productNodes.get(i).get(j);

    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);

    							Edge productEdge = outputNode.copyEdge(colEdge, 
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
    
    private String computeEdgeCode(Edge edge, Set<Edge> rowEdges, Set<Edge> colEdges) {
    	if (rowEdges.contains(edge)) {
    		return getRow(edge.getTail()).toString() + getRow(edge.getHead()).toString() 
    				+ "r";
    	} else if (colEdges.contains(edge)) {
    		return getColumn(edge.getTail()).toString() + getColumn(edge.getHead()).toString() 
    				+ "c";
    	} else {
    		return "";
    	}
    }
    
    private void copyCartesianPaths(Partition partition, Set<Edge> pathEdges, 
    		Set<Edge> rowEdges, Set<Edge> colEdges, Set<String> edgeCopyCodes, 
    		HashMap<String, Set<Edge>> idToIncomingEdges, int tolerance, boolean isForward) {
    	for (Node partitionNode : partition.getNodes()) {
    		HashMap<String, Node> idToNodeCopy = new HashMap<String, Node>();
    		
    		Stack<Node> nodeStack = new Stack<Node>();
    		
    		nodeStack.push(partitionNode);
    		
    		while (!nodeStack.isEmpty()) {
        		Node node = nodeStack.pop();
        		
        		Node nodeCopy;
        		
        		if (idToNodeCopy.containsKey(node.getNodeID())) {
        			nodeCopy = idToNodeCopy.get(node.getNodeID());
        		} else {
        			nodeCopy = partition.getNodeCopy(node.getNodeID());
        		}

        		for (Edge edge : determineNextEdges(node, pathEdges, rowEdges, colEdges, 
        				edgeCopyCodes, idToIncomingEdges, tolerance, isForward)) {
        			edgeCopyCodes.add(computeEdgeCode(edge, rowEdges, colEdges));

        			for (Node nextNode : determineNextNodes(edge, rowEdges, colEdges, partition, 
        					isForward)) {
        				if (!partition.hasNodeCopy(nextNode.getNodeID())
        						&& !idToNodeCopy.containsKey(nextNode.getNodeID())
        						&& !nextNode.isIdenticalTo(node)) {
        					idToNodeCopy.put(nextNode.getNodeID(), productSpace.copyNode(nextNode));

        					nodeStack.push(nextNode);
        				}

        				Node nextNodeCopy;

        				if (idToNodeCopy.containsKey(nextNode.getNodeID())) {
        					nextNodeCopy = idToNodeCopy.get(nextNode.getNodeID());
        					
        					if (isPathStartNode(nextNode, edge, rowEdges, colEdges)) {
        						nextNodeCopy.setNodeType(NodeType.START.getValue());
        					} else if (!isPathAcceptNode(nextNode, edge, rowEdges, colEdges)) {
        						nextNodeCopy.clearNodeType();
        					}
        				} else {
        					nextNodeCopy = partition.getNodeCopy(nextNode.getNodeID());
        				}

        				if (isForward) {
        					nodeCopy.copyEdge(edge, nextNodeCopy);
        				} else {
        					nextNodeCopy.copyEdge(edge, nodeCopy);
        				}
        			}
        		}
        	}
    	}
    }
    
    private void copyPartitionToNodeSpace(Partition partition, Set<Edge> rowEdges, 
    		Set<Edge> colEdges) {
		partition.copyToNodeSpace();
		
		for (Node node : partition.getNodes()) {
			Node nodeCopy = partition.getNodeCopy(node.getNodeID());
			
    		if (node.hasEdges()) {
    			for (Edge edge : node.getEdges()) {
    				if (!rowEdges.contains(edge) && !colEdges.contains(edge)) {
    					Node headCopy = partition.getNodeCopy(edge.getHead().getNodeID());

    					nodeCopy.copyEdge(edge, headCopy);
    				}
    			}
    		}
    	}
    }
    
    private Set<Edge> determineNextEdges(Node node, Set<Edge> pathEdges, Set<Edge> rowEdges, 
    		Set<Edge> colEdges, Set<String> copiedEdgeCodes, HashMap<String, Set<Edge>> idToIncomingEdges, 
    		int tolerance, boolean isForward) {
    	Set<Edge> edges = new HashSet<Edge>();
		
		Set<Edge> diagonalEdges = new HashSet<Edge>();
		
		if (isForward) {
			if (node.hasEdges()) {
				edges.addAll(node.getEdges());
			}
		} else if (idToIncomingEdges.containsKey(node.getNodeID())) {
			edges.addAll(idToIncomingEdges.get(node.getNodeID()));
		}
		
		for (Edge edge : edges) {
			if (!rowEdges.contains(edge) && !colEdges.contains(edge)) {
				diagonalEdges.add(edge);
			}
		}
		
		edges.retainAll(pathEdges);
		
		Set<Edge> nextEdges = new HashSet<Edge>();
		
		for (Edge edge : edges) {
			if (!isEdgeCopied(edge, rowEdges, colEdges, copiedEdgeCodes) 
					&& !isEdgeMatching(edge, diagonalEdges, tolerance)) {
				nextEdges.add(edge);
			}
		}
		
		return nextEdges;
    }
    
    private Set<Node> determineNextNodes(Edge edge, Set<Edge> rowEdges, Set<Edge> colEdges,
    		Partition partition, boolean isForward) {
    	Set<Node> nextNodes = new HashSet<Node>();
    	
    	Node nextNode;
    	
    	if (isForward) {
    		nextNode = edge.getHead();
    	} else {
    		nextNode = edge.getTail();
    	}
    	
    	if (rowEdges.contains(edge)) {
    		Integer row = getRow(nextNode);
    		
    		if (partition.isRowProjectable(row)) {
    			nextNodes.addAll(partition.projectRow(row));
    		} else {
    			nextNodes.add(nextNode);
    		}
    	} else if (colEdges.contains(edge)) {
    		Integer col = getColumn(nextNode);
    		
    		if (partition.isColumnProjectable(col)) {
    			nextNodes.addAll(partition.projectColumn(col));
    		} else {
    			nextNodes.add(nextNode);
    		}
    	}
    	
    	return nextNodes;
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
    
    private boolean isEdgeCopied(Edge edge, Set<Edge> rowEdges, Set<Edge> colEdges,
    		Set<String> copiedEdgeCodes) {
    	return (copiedEdgeCodes.contains(computeEdgeCode(edge, rowEdges, colEdges)));
    }
    
    private boolean isEdgeMatching(Edge edge, Set<Edge> edges, int tolerance) {
		for (Edge e : edges) {
			if (edge.isMatchingTo(e, tolerance)) {
				return true;
			}
		}
		
		return false;
	}
    
    private boolean isPathAcceptNode(Node node, Edge pathEdge, Set<Edge> rowEdges, 
    		Set<Edge> colEdges) {
    	if (rowEdges.contains(pathEdge)) {
    		return rowNodes.get(getRow(node)).isAcceptNode();
    	} else if (colEdges.contains(pathEdge)) {
    		return colNodes.get(getColumn(node)).isAcceptNode();
    	} else {
    		return false;
    	}
    }
    
    private boolean isPathStartNode(Node node, Edge pathEdge, Set<Edge> rowEdges, 
    		Set<Edge> colEdges) {
    	if (rowEdges.contains(pathEdge)) {
    		return rowNodes.get(getRow(node)).isStartNode();
    	} else if (colEdges.contains(pathEdge)) {
    		return colNodes.get(getColumn(node)).isStartNode();
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
    
    private List<Partition> partitionNodesBySource() {
    	List<Partition> partitions = new LinkedList<Partition>();
    	
    	for (Node sourceNode : productSpace.getSourceNodes()) {
    		Partition partition = new Partition();
    		
    		partition.addNode(sourceNode);
    		
    		int partitionIndex = -1;
    		
    		Stack<Node> nodeStack = new Stack<Node>();
    		
    		nodeStack.push(sourceNode);
    		 
    		while (!nodeStack.isEmpty()) {
        		Node node = nodeStack.pop();
        		
        		if (node.hasEdges()) {
        			for (Edge edge : node.getEdges()) {
        				int i = 0;
        				
        				if (partitionIndex < 0) {
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
        					if (!partition.hasNode(edge.getHead())
        							&& !edge.getHead().isIdenticalTo(node)) {
        						nodeStack.push(edge.getHead());
            					
            					partition.addNode(edge.getHead());
        					}
        				} else {
        					if (!partitions.get(partitionIndex).hasNode(edge.getHead())
        							&& !edge.getHead().isIdenticalTo(node)) {
        						nodeStack.push(edge.getHead());
            					
            					partitions.get(partitionIndex).addNode(edge.getHead());
        					}
        				}
        			}
        		}
        	}
    		
    		if (partitionIndex < 0) {
    			partitions.add(partition);
    		}
    	}
    	
    	for (Partition partition : partitions) {
    		partition.mapNodes(this);
    	}
    	
    	return partitions;
    }
    
    private class Partition {
    	
    	private Set<Node> nodes = new HashSet<Node>();
    	
    	private Set<String> nodeIDs = new HashSet<String>();
    	
    	private HashMap<String, Node> idToNodeCopy = new HashMap<String, Node>();
    	
    	private HashMap<Integer, Set<Node>> rowToNodes = new HashMap<Integer, Set<Node>>();
    	
    	private HashMap<Integer, Set<Node>> colToNodes = new HashMap<Integer, Set<Node>>();
    	
    	public Partition() {
    		
    	}
    	
    	public Partition(Node node) {
    		addNode(node);
    	}
    	
    	public void addNode(Node node) {
    		nodes.add(node);
    		
    		nodeIDs.add(node.getNodeID());
    	}
    	
    	public void copyToNodeSpace() {
    		for (Node node : nodes) {
    			idToNodeCopy.put(node.getNodeID(), productSpace.copyNode(node));
        	}
    	}
    	
    	public Node getNodeCopy(String nodeID) {
    		return idToNodeCopy.get(nodeID);
    	}
    	
    	public Set<Node> getNodes() {
    		return nodes;
    	}
    	
    	public boolean hasNodeCopy(String nodeID) {
    		return idToNodeCopy.containsKey(nodeID);
    	}
    	
    	public boolean hasNode(Node node) {
    		return nodeIDs.contains(node.getNodeID());
    	}
    	
    	public boolean isRowProjectable(Integer row) {
    		return rowToNodes.containsKey(row);
    	}
    	
    	public boolean isColumnProjectable(Integer col) {
    		return colToNodes.containsKey(col);
    	}
    	
    	public void mapNodes(Product nodeProduct) {
    		for (Node node : nodes) {
    			Integer row = nodeProduct.getRow(node);
    			
    			if (row.intValue() > 0 ) {
    				if (!rowToNodes.containsKey(row)) {
    					rowToNodes.put(row, new HashSet<Node>());
    				}

    				rowToNodes.get(row).add(node);
    			}
    			
    			Integer col = nodeProduct.getColumn(node);
    			
    			if (col.intValue() > 0 ) {
    				if (!colToNodes.containsKey(col)) {
    					colToNodes.put(col, new HashSet<Node>());
    				}

    				colToNodes.get(col).add(node);
    			}
    		}
    	}
    	
    	public Set<Node> projectRow(Integer row) {
    		return rowToNodes.get(row);
    	}
    	
    	public Set<Node> projectColumn(Integer col) {
    		return colToNodes.get(col);
    	}
    	
    	public void union(Partition partition) {
    		for (Node node : partition.getNodes()) {
    			addNode(node);
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
