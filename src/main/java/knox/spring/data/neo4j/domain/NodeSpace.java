package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import knox.spring.data.neo4j.domain.Node.NodeType;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NodeEntity
public class NodeSpace {
	@GraphId
    Long id;
	
	int nodeIndex;
	
	@Relationship(type = "CONTAINS") 
    Set<Node> nodes;
	
	private static final Logger LOG = LoggerFactory.getLogger(NodeSpace.class);
	
	public NodeSpace() {
		
	}
	
	public NodeSpace(int nodeIndex) {
		this.nodeIndex = nodeIndex;
		
		this.nodes = new HashSet<Node>();
	}
	
	public void addNode(Node node) {
		if (nodes == null) {
			nodes = new HashSet<Node>();
		}
		
		nodes.add(node);
	}
	
	public void clearEdges() {
		if (hasNodes()) {
			for (Node node : nodes) {
				node.clearEdges();
			}
		}
	}
	
	public void clearNodes() {
		if (hasNodes()) {
			nodes.clear();
		}
    	
    	nodeIndex = 0;
    }
	
	public NodeSpace copy() {
		NodeSpace spaceCopy = new NodeSpace(0);
		
		spaceCopy.copyNodeSpace(this);
		
		return spaceCopy;
	}
	
	public void shallowCopyNodeSpace(NodeSpace space) {
		if (space.hasNodes()) {
			nodeIndex = space.getNodeIndex();

			nodes = new HashSet<Node>(space.getNodes());
		} else {
			nodeIndex = 0;
			
			nodes = null;
		}
	}
	
	public void copyNodeSpace(NodeSpace space) {
		if (space.hasNodes()) {
			HashMap<String, Node> idToNodeCopy = new HashMap<String, Node>();
			
			for (Node node : space.getNodes()) {
				idToNodeCopy.put(node.getNodeID(), copyNodeWithID(node));
			}
			
			for (Node node : space.getNodes()) {
				if (node.hasEdges()) {
					Node nodeCopy = idToNodeCopy.get(node.getNodeID());

					for (Edge edge : node.getEdges()) {
						nodeCopy.copyEdge(edge, idToNodeCopy.get(edge.getHead().getNodeID()));
					}
				}
			}
		}
		
		nodeIndex = space.getNodeIndex();
	}
	
	public Node copyNodeWithEdges(Node node) {
		Node nodeCopy = copyNode(node);
		
		if (node.hasEdges()) {
			for (Edge edge : node.getEdges()) {
				nodeCopy.copyEdge(edge);
			}
		}
		
		return nodeCopy;
	}
	
	public Node copyNodeWithID(Node node) {
		Node nodeCopy = createNode(node.getNodeID());
		
		for (String nodeType : node.getNodeTypes()) {
			nodeCopy.addNodeType(nodeType);
		}
		
		return nodeCopy;
	}
	
	public Node copyNode(Node node) {
		Node nodeCopy = createNode();
		
		for (String nodeType : node.getNodeTypes()) {
			nodeCopy.addNodeType(nodeType);
		}
		
		return nodeCopy;
	}
	
	public boolean clearStartNodeTypes() {
		boolean isCleared = false;
		
		if (hasNodes()) {
			for (Node node : nodes) {
				if (node.deleteStartNodeType()) {
					isCleared = true;
				}
			}
		}
		
		return isCleared;
	}
	
	public Node createNode() {
		Node node = new Node("n" + nodeIndex++);
		
		addNode(node);
		
		return node;
	}
	
	public Node createNode(String nodeID) {
		Node node = new Node(nodeID);
		
		addNode(node);
		
		return node;
	}
	
	public Node createTypedNode(String nodeType) {
		Node node = createNode();
		
		node.addNodeType(nodeType);
		
		addNode(node);
		
		return node;
	}
	
	public Node createTypedNode(String nodeID, String nodeType) {
		Node node = createNode(nodeID);
		
		node.addNodeType(nodeType);
		
		addNode(node);
		
		return node;
	}
	
	public Node createStartAcceptNode() {
		Node startAcceptNode = createStartNode();
		
		startAcceptNode.addNodeType(NodeType.ACCEPT.getValue());
		
		return startAcceptNode;
	}
	
	public Node createAcceptNode() {
		return createTypedNode(NodeType.ACCEPT.getValue());
	}
	
	public Node createStartNode() {
		return createTypedNode(NodeType.START.getValue());
	}
	
	public boolean deleteNode(Node deletedNode) {
		if (hasNodes()) {
			return nodes.remove(deletedNode);
		} else {
			return false;
		} 
	}
	
	public boolean deleteNodes(Collection<Node> deletedNodes) {
		if (hasNodes()) {
			return nodes.removeAll(deletedNodes);
		} else {
			return false;
		}
	}
	
	public Set<Node> getAcceptNodes() {
    	Set<Node> acceptNodes = new HashSet<Node>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			if (node.isAcceptNode()) {
    				acceptNodes.add(node);
    			}
    		}
    	}

    	return acceptNodes;
    }
	
	public Set<Edge> getEdges() {
		Set<Edge> edges = new HashSet<Edge>();
		
		if (hasNodes()) {
			for (Node node : nodes) {
				if (node.hasEdges()) {
					edges.addAll(node.getEdges());
				}
			}
		}

		return edges;
	}
	
//	public Set<Edge> getMinimizableEdges(Node node, HashMap<String, Set<Edge>> nodeIDsToIncomingEdges) {
//		 Set<Edge> minimizableEdges = new HashSet<Edge>();
//
//		 if (nodeIDsToIncomingEdges.containsKey(node.getNodeID())) {
//			 Set<Edge> incomingEdges = nodeIDsToIncomingEdges.get(node.getNodeID());
//
//			 if (incomingEdges.size() == 1) {
//				 Edge incomingEdge = incomingEdges.iterator().next();
//
//				 Node predecessor = incomingEdge.getTail();
//
//				 if (!incomingEdge.hasComponents() && !incomingEdge.isCyclic()
//						 && (predecessor.getNumEdges() == 1 || !node.hasConflictingNodeType(predecessor))) {
//					 minimizableEdges.add(incomingEdge);
//				 }
//			 } else if (incomingEdges.size() > 1) {
//				 for (Edge incomingEdge : incomingEdges) {
//					 Node predecessor = incomingEdge.getTail();
//
//					 if (!incomingEdge.hasComponents() && !incomingEdge.isCyclic() 
//							 && predecessor.getNumEdges() == 1 && !predecessor.hasConflictingNodeType(node)) {
//						 minimizableEdges.add(incomingEdge);
//					 }
//				 }
//			 }
//		 }
//
//		 return minimizableEdges;
//	 }
	
	public int getNodeIndex() {
		return nodeIndex;
	}
	
	public Node getNode(String nodeID) {
		if (hasNodes()) {
			for (Node node : nodes) {
				if (node.getNodeID().equals(nodeID)) {
					return node;
				}
			}
		} 
		
		return null;
	}
    
    public Set<Node> getNodes() {
    	return nodes;
    }
    
    public Set<String> getNodeIDs() {
    	Set<String> nodeIDs = new HashSet<String>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			nodeIDs.add(node.getNodeID());
    		}
    	}
    	
    	return nodeIDs;
    }
    
    public int getNumNodes() {
    	if (hasNodes()) {
    		return nodes.size();
    	} else {
    		return 0;
    	}
    }

    public Node getStartNode() {
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			if (node.isStartNode()) {
    				return node;
    			}
    		}
    	}

    	return null;
    }
    
    public int getNumStartNodes() {
    	return getStartNodes().size();
    }

    public Set<Node> getStartNodes() {
        Set<Node> startNodes = new HashSet<Node>();

        if (hasNodes()) {
        	for (Node node : nodes) {
        		if (node.isStartNode()) {
        			startNodes.add(node);
        		}
        	}
        }

        return startNodes;
    }

    public Set<Node> getTypedNodes() {
        Set<Node> typedNodes = new HashSet<Node>();

        if (hasNodes()) {
        	for (Node node : nodes) {
        		if (node.hasNodeTypes()) {
        			typedNodes.add(node);
        		}
        	}
        }

        return typedNodes;
    }
    
    public boolean hasNodes() {
    	return nodes != null && !nodes.isEmpty();
    }
    
    public boolean hasNode(Node node) {
    	return hasNodes() && nodes.contains(node);
    }
    
    public void loadEdges(HashMap<String, Set<Edge>> nodeIDToEdges) { 
    	if (hasNodes()) {
    		for (Node node : nodes) {
        		if (nodeIDToEdges.containsKey(node.getNodeID())) {
        			node.setEdges(nodeIDToEdges.get(node.getNodeID()));
        		}
        	}
    	}
    }
    
    public HashMap<String, Set<Edge>> mapNodeIDsToEdges() {
    	HashMap<String, Set<Edge>> nodeIDToOutgoingEdges = new HashMap<String, Set<Edge>>();
        
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			if (node.hasEdges()) {
    				for (Edge edge : node.getEdges()) {
    					if (!nodeIDToOutgoingEdges.containsKey(node.getNodeID())) {
    						nodeIDToOutgoingEdges.put(node.getNodeID(), new HashSet<Edge>());
    					}

    					nodeIDToOutgoingEdges.get(node.getNodeID()).add(edge);
    				}
    			}
    		}
    	}
        
        return nodeIDToOutgoingEdges;
    }

    public HashMap<String, Set<Edge>> mapNodeIDsToIncomingEdges() {
        HashMap<String, Set<Edge>> nodeIDToIncomingEdges = new HashMap<String, Set<Edge>>();
        
        if (hasNodes()) {
        	for (Node node : nodes) {
        		if (node.hasEdges()) {
        			for (Edge edge : node.getEdges()) {
        				Node successor = edge.getHead();

        				if (!nodeIDToIncomingEdges.containsKey(successor.getNodeID())) {
        					nodeIDToIncomingEdges.put(successor.getNodeID(), new HashSet<Edge>());
        				}

        				nodeIDToIncomingEdges.get(successor.getNodeID()).add(edge);
        			}
        		}
        	}
        }
        
        return nodeIDToIncomingEdges;
    }

    public HashMap<String, Node> mapNodeIDsToNodes() {
    	HashMap<String, Node> nodeIDToNode = new HashMap<String, Node>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			nodeIDToNode.put(node.getNodeID(), node);
    		}
    	}

    	return nodeIDToNode;
    }
    
    public void labelSinkNodesAccept() {
    	Set<Node> sinkNodes = getSinkNodes();
    	
    	for (Node sinkNode : sinkNodes) {
    		sinkNode.addNodeType(Node.NodeType.ACCEPT.getValue());
    	}
    }
    
    public void labelSourceNodesStart() {
    	Set<Node> sourceNodes = getSourceNodes();
    	
    	for (Node sourceNode : sourceNodes) {
    		sourceNode.addNodeType(Node.NodeType.START.getValue());
    	}
    }
    
    public Set<Node> getSinkNodes() {
    	Set<Node> sinkNodes = new HashSet<Node>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			if (!node.hasEdges()) {
    				sinkNodes.add(node);
    			}
    		}
    	}
    	
    	return sinkNodes;
    }
    
//    public void deleteUnconnectedNodes() {
//    	Set<Node> deletedNodes = new HashSet<Node>();
//    	
//    	
//    	Set<String> successorIDs = new HashSet<String>();
//
//    	for (Node node : nodes) {
//    		if (node.hasEdges()) {
//    			for (Edge edge : node.getEdges()) {
//    				successorIDs.add(edge.getHead().getNodeID());
//    			}
//    		}
//    	}
//
//    	for (Node node : nodes) {
//    		if (!successorIDs.contains(node.getNodeID()) && !node.hasEdges()) {
//    			deletedNodes.add(node);
//    		}
//    	}
//
//    	nodes.removeAll(deletedNodes);
//    }
    
    public Set<Node> getSourceNodes() {
    	Set<Node> sourceNodes = new HashSet<Node>();
    	
    	if (hasNodes()) {
    		Set<String> successorIDs = new HashSet<String>();

    		for (Node node : nodes) {
    			if (node.hasEdges()) {
    				for (Edge edge : node.getEdges()) {
    					successorIDs.add(edge.getHead().getNodeID());
    				}
    			}
    		}

    		for (Node node : nodes) {
    			if (!successorIDs.contains(node.getNodeID()) && node.hasEdges()) {
    				sourceNodes.add(node);
    			}
    		}
    	}
    	
    	return sourceNodes;
    }
    
    public Node mergeSourceNodes() {
    	Iterator<Node> startNodes = getStartNodes().iterator();

    	if (startNodes.hasNext()) {
    		Node primaryStartNode = startNodes.next();

    		while (startNodes.hasNext()) {
    			Node secondaryStartNode = startNodes.next();

    			if (secondaryStartNode.hasEdges()) {
    				for (Edge secondaryEdge : secondaryStartNode.getEdges()) {
    					primaryStartNode.copyEdge(secondaryEdge);
    				}
    			}

    			nodes.remove(secondaryStartNode);
    		}

    		reindexNodes();

    		return primaryStartNode;
    	}
    	
    	return null;
    }
    
    public List<Edge> reverseDepthFirstEdgeTraversal() {
    	HashMap<String, Set<Edge>> idToIncomingEdges = mapNodeIDsToIncomingEdges();
    	
    	List<Edge> traversalEdges = new LinkedList<Edge>();

    	Set<Edge> globalEdges = new HashSet<Edge>();

    	for (Node acceptNode : getAcceptNodes()) {
    		if (idToIncomingEdges.containsKey(acceptNode.getNodeID())) {
    			for (Edge acceptEdge : idToIncomingEdges.get(acceptNode.getNodeID())) {
    				if (!globalEdges.contains(acceptEdge)) {
    	    			traversalEdges.addAll(reverseDepthFirstEdgeTraversal(acceptEdge, globalEdges, 
    	    					idToIncomingEdges));
    	    		}
    			}
    		}
    	}

    	return traversalEdges;
    }
    
    private List<Edge> reverseDepthFirstEdgeTraversal(Edge acceptEdge, 
    		Set<Edge> globalEdges, HashMap<String, Set<Edge>> idToIncomingEdges) {
    	List<Edge> traversalEdges = new LinkedList<Edge>();

    	Stack<Edge> edgeStack = new Stack<Edge>();

    	edgeStack.push(acceptEdge);

    	Set<Edge> localEdges = new HashSet<Edge>();

    	localEdges.add(acceptEdge);

    	while (!edgeStack.isEmpty()) {
    		Edge edge = edgeStack.pop();

    		traversalEdges.add(edge);

    		if (idToIncomingEdges.containsKey(edge.getTail().getNodeID())) {
    			for (Edge tailEdge : idToIncomingEdges.get(edge.getTail().getNodeID())) {
    				if (!globalEdges.contains(tailEdge)
    						&& !localEdges.contains(tailEdge)) {
    					edgeStack.push(tailEdge);

    					localEdges.add(tailEdge);
    				}
    			}
    		}
    	}

    	globalEdges.addAll(localEdges);

    	return traversalEdges;
    }
    
    public List<Node> reverseDepthFirstNodeTraversal() {
    	HashMap<String, Set<Edge>> idToIncomingEdges = mapNodeIDsToIncomingEdges();
    	
    	List<Node> traversalNodes = new ArrayList<Node>(getNumNodes());

    	Set<Node> globalNodes = new HashSet<Node>();

    	for (Node acceptNode : getAcceptNodes()) {
    		if (!globalNodes.contains(acceptNode)) {
    			traversalNodes.addAll(reverseDepthFirstNodeTraversal(acceptNode, globalNodes, 
    					idToIncomingEdges));
    		}
    	}

    	return traversalNodes;
    }
    
    private List<Node> reverseDepthFirstNodeTraversal(Node acceptNode, 
    		Set<Node> globalNodes, HashMap<String, Set<Edge>> idToIncomingEdges) {
    	List<Node> traversalNodes = new LinkedList<Node>();
    	
    	Stack<Node> nodeStack = new Stack<Node>();

    	nodeStack.push(acceptNode);

    	Set<Node> localNodes = new HashSet<Node>();
    	
    	localNodes.add(acceptNode);

    	while (!nodeStack.isEmpty()) {
    		Node node = nodeStack.pop();

    		traversalNodes.add(node);

    		if (idToIncomingEdges.containsKey(node.getNodeID())) {
    			for (Edge edge : idToIncomingEdges.get(node.getNodeID())) {
    				if (!globalNodes.contains(edge.getTail())
    						&& !localNodes.contains(edge.getTail())) {
    					nodeStack.push(edge.getTail());

    					localNodes.add(edge.getTail());
    				}
    			}
    		}
    	}
    	
    	globalNodes.addAll(localNodes);
    	
    	return traversalNodes;
    }
    
    public List<Edge> depthFirstEdgeTraversal() {
    	List<Edge> traversalEdges = new LinkedList<Edge>();

    	Set<Edge> globalEdges = new HashSet<Edge>();

    	for (Node startNode : getStartNodes()) {
    		if (startNode.hasEdges()) {
        		for (Edge startEdge : startNode.getEdges()) {
        			if (!globalEdges.contains(startEdge)) {
            			traversalEdges.addAll(depthFirstEdgeTraversal(startEdge, globalEdges));
            		}
        		}
        	}
    		
    	}

    	return traversalEdges;
    }
    
    private List<Edge> depthFirstEdgeTraversal(Edge startEdge, Set<Edge> globalEdges) {
    	List<Edge> traversalEdges = new LinkedList<Edge>();
    	
    	Stack<Edge> edgeStack = new Stack<Edge>();

    	edgeStack.push(startEdge);

    	Set<Edge> localEdges = new HashSet<Edge>();
    	
    	localEdges.add(startEdge);

    	while (!edgeStack.isEmpty()) {
    		Edge edge = edgeStack.pop();

    		traversalEdges.add(edge);

    		if (edge.getHead().hasEdges()) {
    			for (Edge headEdge : edge.getHead().getEdges()) {
    				if (!globalEdges.contains(headEdge) 
    						&& !localEdges.contains(headEdge)) {
    					edgeStack.push(headEdge);

    					localEdges.add(headEdge);
    				}
    			}
    		}
    	}
    	
    	globalEdges.addAll(localEdges);
    	
    	return traversalEdges;
    }
    
    public List<Node> depthFirstNodeTraversal() {
    	List<Node> traversalNodes = new ArrayList<Node>(getNumNodes());

    	Set<Node> globalNodes = new HashSet<Node>();

    	for (Node startNode : getStartNodes()) {
    		if (!globalNodes.contains(startNode)) {
    			traversalNodes.addAll(depthFirstNodeTraversal(startNode, globalNodes));
    		}
    	}

    	return traversalNodes;
    }
    
    private List<Node> depthFirstNodeTraversal(Node startNode, Set<Node> globalNodes) {
    	List<Node> traversalNodes = new LinkedList<Node>();
    	
    	Stack<Node> nodeStack = new Stack<Node>();

    	nodeStack.push(startNode);

    	Set<Node> localNodes = new HashSet<Node>();
    	
    	localNodes.add(startNode);

    	while (!nodeStack.isEmpty()) {
    		Node node = nodeStack.pop();

    		traversalNodes.add(node);

    		if (node.hasEdges()) {
    			for (Edge edge : node.getEdges()) {
    				if (!globalNodes.contains(edge.getHead()) 
    						&& !localNodes.contains(edge.getHead())) {
    					nodeStack.push(edge.getHead());

    					localNodes.add(edge.getHead());
    				}
    			}
    		}
    	}
    	
    	globalNodes.addAll(localNodes);
    	
    	return traversalNodes;
    }
    
    private void reindexNodes() {
    	nodeIndex = 0;

    	if (hasNodes()) {
    		for (Node node : nodes) {
    			node.setNodeID("n" + nodeIndex++);
    		}
    	}
    }
    
    public Set<Node> getOtherNodes(Node nodes) {
    	Set<Node> otherNodes;
    	
    	if (hasNodes()) {
    		otherNodes = new HashSet<Node>(this.nodes);
    		
    		otherNodes.remove(nodes);
    	} else {
    		otherNodes = new HashSet<Node>();
    	}
    	
    	return otherNodes;
    }

    private Set<Edge> getOtherEdges(Node node, Set<Edge> edges) {
        Set<Edge> diffEdges = new HashSet<Edge>();

        if (node.hasEdges()) {
            for (Edge edge : node.getEdges()) {
                if (!edges.contains(edge)) {
                    diffEdges.add(edge);
                }
            }
        }

        return diffEdges;
    }

    public Set<Edge> retainEdges(Set<Edge> retainedEdges) {
    	Set<Edge> diffEdges = new HashSet<Edge>();
    	
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			Set<Edge> localDiffEdges = getOtherEdges(node, retainedEdges);

    			if (localDiffEdges.size() > 0) {
    				node.deleteEdges(localDiffEdges);

    				diffEdges.addAll(localDiffEdges);
    			}
    		}
    	}
    	
    	return diffEdges;
    }
    
    public void setNodeIndex(int nodeIndex) {
    	this.nodeIndex = nodeIndex;
    }
    
    public void deleteUnacceptableNodes() {
    	deleteUnconnectedNodes();
    	
    	retainEdges(reverseDepthFirstEdgeTraversal());
    }
    
    public void deleteUnconnectedNodes() {
    	retainEdges(depthFirstEdgeTraversal());
    }
    
    public void deleteOrthogonalPaths(Collection<Edge> edges) {
    	Set<Node> tails = new HashSet<Node>();
    	
    	Set<Node> heads = new HashSet<Node>();
    	
    	for (Edge edge : edges) {
    		tails.add(edge.getTail());
    		
    		heads.add(edge.getHead());
    	}
    	
    	Set<Edge> pathEdges = new HashSet<Edge>();
    	
    	HashMap<String, Set<Edge>> idToIncomingEdges = mapNodeIDsToIncomingEdges();
    	
    	for (Edge edge : edges) {
    		if (!heads.contains(edge.getTail())) {
    			pathEdges.addAll(reverseDepthFirstEdgeTraversal(edge, pathEdges,
        				idToIncomingEdges));
    		}
    		
    		if (!tails.contains(edge.getHead())) {
    			pathEdges.addAll(depthFirstEdgeTraversal(edge, pathEdges));
    		}
    	}
    	
    	retainEdges(pathEdges);
    }
    
    public boolean detachDeleteNodes(Collection<Node> nodes) {
    	detachNodes(nodes);
    	
    	return deleteNodes(nodes);
    }
    
    private void detachNodes(Collection<Node> nodes) {
    	for (Node node : this.nodes) {
			if (node.hasEdges()) {
				Set<Edge> deletedEdges = new HashSet<Edge>();

				for (Edge edge : node.getEdges()) {
					if (nodes.contains(edge.getHead())) {
						deletedEdges.add(edge);
					}
				}

				node.deleteEdges(deletedEdges);
			}
		}
    }
    
    public boolean retainEdges(Collection<Edge> retainedEdges) {
    	if (hasNodes()) {
    		boolean isChanged = false;
    		
    		for (Node node : this.nodes) {
    			if (node.hasEdges()) {
    				Set<Edge> deletedEdges = new HashSet<Edge>();

    				for (Edge edge : node.getEdges()) {
    					if (!retainedEdges.contains(edge)) {
    						deletedEdges.add(edge);
    					}
    				}
    				
    				if (!deletedEdges.isEmpty()) {
    					isChanged = true;
    				}

    				node.deleteEdges(deletedEdges);
    			}
    		}
    		
    		if (isChanged) {
    			HashMap<String, Set<Edge>> idToIncomingEdges = mapNodeIDsToIncomingEdges();
    			
    			Set<Node> deletedNodes = new HashSet<Node>();
    			
    			for (Node node : this.nodes) {
    				if (!node.hasEdges() && !idToIncomingEdges.containsKey(node.getNodeID())) {
    					deletedNodes.add(node);
    				}
    			}
    			
    			deleteNodes(deletedNodes);
    		}
    		
    		return isChanged;
    	} else {
    		return false;
    	}
    }
    
    public boolean retainNodes(Collection<Node> retainedNodes) {
    	if (hasNodes()) {
    		boolean isChanged = this.nodes.retainAll(retainedNodes);

    		if (isChanged) {
    			for (Node node : this.nodes) {
    				if (node.hasEdges()) {
    					Set<Edge> deletedEdges = new HashSet<Edge>();

    					for (Edge edge : node.getEdges()) {
    						if (!this.nodes.contains(edge.getHead())) {
    							deletedEdges.add(edge);
    						}
    					}

    					node.deleteEdges(deletedEdges);
    				}
    			}
    		}
    		
    		return isChanged;
    	} else {
    		return false;
    	}
    }
    
    public HashMap<String, Node> union(NodeSpace space) {
    	HashMap<String, Node> idToNodeCopy = new HashMap<String, Node>();

    	if (space.hasNodes()) {
    		for (Node node : space.getNodes()) {
    			Node nodeCopy = copyNode(node);
    			
    			idToNodeCopy.put(node.getNodeID(), nodeCopy);
    		}
    		
    		for (Node node : space.getNodes()) {
    			if (node.hasEdges()) {
    				Node nodeCopy = idToNodeCopy.get(node.getNodeID());

    				for (Edge edge : node.getEdges()) {
    					nodeCopy.copyEdge(edge, idToNodeCopy.get(edge.getHead().getNodeID()));
    				}
    			} 
    		}
    	}
    	
    	return idToNodeCopy;
    }
    
    public static boolean hasAcceptNode(Set<Node> nodes) {
    	for (Node node : nodes) {
    		if (node.isAcceptNode()) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    public void concatenateNodes(Set<Node> tails, Set<Node> heads) {
    	HashMap<String, Set<Edge>> idToIncomingEdges = mapNodeIDsToIncomingEdges();
    	
    	if (hasAcceptNode(heads)) {
    		for (Node tail : tails) {
    			tail.addAcceptNodeType();
    		}
    	} else {
    		for (Node tail : tails) {
    			tail.deleteAcceptNodeType();
    		}
    	}
    	
    	for (Node head : heads) {
    		if (idToIncomingEdges.containsKey(head.getNodeID())) {
				head.deleteStartNodeType();
			} else {
				deleteNode(head);
			}
    	}
    	
    	for (Node tail : tails) {
    		tail.unionEdges(heads);
    	}
	}
    
    public void minimizeEdges() {
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			node.minimizeEdges();
    		}
    	}
    }
    
}
