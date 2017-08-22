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
		nodes.add(node);
	}
	
	public void clearEdges() {
		for (Node node : nodes) {
			node.clearEdges();
		}
	}
	
	public void clearNodes() {
    	nodes.clear();
    	
    	nodeIndex = 0;
    }
	
	public NodeSpace copy() {
		NodeSpace spaceCopy = new NodeSpace(0);
		
		spaceCopy.copyNodeSpace(this);
		
		return spaceCopy;
	}
	
	public void shallowCopyNodeSpace(NodeSpace space) {
		nodeIndex = space.getNodeIndex();
		
		nodes = new HashSet<Node>(space.getNodes());
	}
	
	public void copyNodeSpace(NodeSpace space) {
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
	
	public Node createAcceptNode() {
		return createTypedNode(NodeType.ACCEPT.getValue());
	}
	
	public Node createStartNode() {
		return createTypedNode(NodeType.START.getValue());
	}
	
	public boolean deleteNodes(Set<Node> deletedNodes) {
		return nodes.removeAll(deletedNodes);
	}
	
	public boolean detachDeleteNodes(Set<Node> deletedNodes) {	
		for (Node node : nodes) {
			if (node.hasEdges() && !deletedNodes.contains(node)) {
				Set<Edge> deletedEdges = new HashSet<Edge>();

				for (Edge edge : node.getEdges()) {
					if (deletedNodes.contains(edge.getHead())) {
						deletedEdges.add(edge);
					}
				}

				node.deleteEdges(deletedEdges);
			}
		}

		return deleteNodes(deletedNodes);
	}
	
	public Set<Node> getAcceptNodes() {
    	Set<Node> acceptNodes = new HashSet<Node>();
    	
    	for (Node node : nodes) {
    		if (node.isAcceptNode()) {
    			acceptNodes.add(node);
    		}
    	}

    	return acceptNodes;
    }
	
	public Set<Edge> getEdges() {
		Set<Edge> edges = new HashSet<Edge>();
		
		for (Node node : nodes) {
			if (node.hasEdges()) {
				edges.addAll(node.getEdges());
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
		for (Node node : nodes) {
			if (node.getNodeID().equals(nodeID)) {
				return node;
			}
		}
		
		return null;
	}
    
    public Set<Node> getNodes() {
    	return nodes;
    }
    
    public int getSize() {
    	return nodes.size();
    }

    public Node getStartNode() {
    	for (Node node : nodes) {
    		if (node.isStartNode()) {
    			return node;
    		}
    	}

    	return null;
    }

    public Set<Node> getStartNodes() {
        Set<Node> startNodes = new HashSet<Node>();

        for (Node node : nodes) {
        	if (node.isStartNode()) {
        		startNodes.add(node);
        	}
        }

        return startNodes;
    }

    public Set<Node> getTypedNodes() {
        Set<Node> typedNodes = new HashSet<Node>();

        for (Node node : nodes) {
        	if (node.hasNodeTypes()) {
        		typedNodes.add(node);
        	}
        }

        return typedNodes;
    }
    
    public boolean hasNodes() {
    	return nodes != null && !nodes.isEmpty();
    }
    
    public boolean isEmpty() {
    	return getSize() == 0;
    }
    
    public void loadEdges(HashMap<String, Set<Edge>> nodeIDToEdges) { 
    	for (Node node : nodes) {
    		if (nodeIDToEdges.containsKey(node.getNodeID())) {
    			node.setEdges(nodeIDToEdges.get(node.getNodeID()));
    		}
    	}
    }
    
    public HashMap<String, Set<Edge>> mapNodeIDsToEdges() {
    	HashMap<String, Set<Edge>> nodeIDToOutgoingEdges = new HashMap<String, Set<Edge>>();
        
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
        
        return nodeIDToOutgoingEdges;
    }

    public HashMap<String, Set<Edge>> mapNodeIDsToIncomingEdges() {
        HashMap<String, Set<Edge>> nodeIDToIncomingEdges = new HashMap<String, Set<Edge>>();
        
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
        
        return nodeIDToIncomingEdges;
    }

    public HashMap<String, Node> mapNodeIDsToNodes() {
    	HashMap<String, Node> nodeIDToNode = new HashMap<String, Node>();
    	
    	for (Node node : nodes) {
    		nodeIDToNode.put(node.getNodeID(), node);
    	}

    	return nodeIDToNode;
    }
    
    public void minimize() {
    	HashMap<String, Set<Edge>> idToIncomingEdges = mapNodeIDsToIncomingEdges();

    	boolean isMini;

    	do {
    		isMini = true;

    		Set<Node> deletedNodes = new HashSet<Node>();

    		for (Node node : nodes) {
    			if (node.hasEdges()) {
    				Set<Edge> deletedEdges = new HashSet<Edge>();

    				for (Edge edge : node.getEdges()) {
    					if (edge.isUnlabeled()
    							&& !node.hasConflictingType(edge.getHead())
    							&& !(node.getNumEdges() > 1 
    									&& (idToIncomingEdges.get(edge.getHead().getNodeID()).size() > 1
    											|| edge.getHead().isStartNode()))) {
    						isMini = false;

    						deletedEdges.add(edge);
    					}
    				}

    				node.deleteEdges(deletedEdges);

    				Set<Node> mergedNodes = new HashSet<Node>();

    				for (Edge deletedEdge : deletedEdges) {
    					mergedNodes.add(deletedEdge.getHead());
    				}

    				node.unionWithNodes(mergedNodes, idToIncomingEdges);

    				deletedNodes.addAll(mergedNodes);
    			}
    		}

    		deleteNodes(deletedNodes);
    	} while (!isMini);
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
    	
    	for (Node node : nodes) {
    		if (!node.hasEdges()) {
    			sinkNodes.add(node);
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
    
//    public boolean isConnected() {
//    	Set<String> globalIDs = new HashSet<String>();
//
//    	int numPartitions = 0;
//
//    	for (Node startNode : getStartNodes()) {
//    		if (depthFirstTraversal(startNode, new LinkedList<Node>(), globalIDs)) {
//    			numPartitions++;
//    		}
//
//    		if (numPartitions > 1) {
//    			return false;
//    		}
//    	}
//    	
//    	return true;
//    }
    
    public List<Node> reverseDepthFirstTraversal() {
    	HashMap<String, Set<Edge>> idToIncomingEdges = mapNodeIDsToIncomingEdges();
    	
    	List<Node> traversalNodes = new ArrayList<Node>(nodes.size());

    	Set<Node> globalNodes = new HashSet<Node>();

    	for (Node acceptNode : getAcceptNodes()) {
    		traversalNodes.addAll(reverseDepthFirstTraversal(acceptNode, globalNodes, 
    				idToIncomingEdges));
    	}

    	return traversalNodes;
    }
    
    private List<Node> reverseDepthFirstTraversal(Node acceptNode, 
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
    
    public List<Node> depthFirstTraversal() {
    	List<Node> traversalNodes = new ArrayList<Node>(nodes.size());

    	Set<Node> globalNodes = new HashSet<Node>();

    	for (Node startNode : getStartNodes()) {
    		traversalNodes.addAll(depthFirstTraversal(startNode, globalNodes));
    	}

    	return traversalNodes;
    }
    
    private List<Node> depthFirstTraversal(Node startNode, Set<Node> globalNodes) {
    	List<Node> traversalNodes = new LinkedList<Node>();
    	
    	Stack<Node> nodeStack = new Stack<Node>();

    	nodeStack.push(startNode);

    	Set<Node> localNodes = new HashSet<Node>();
    	
    	localNodes.add(startNode);

    	while (!nodeStack.isEmpty()) {
    		Node node = nodeStack.pop();

    		traversalNodes.add(node);

    		for (Edge edge : node.getEdges()) {
    			if (!globalNodes.contains(edge.getHead()) 
    					&& !localNodes.contains(edge.getHead())) {
    				nodeStack.push(edge.getHead());

    				localNodes.add(edge.getHead());
    			}
    		}
    	}
    	
    	globalNodes.addAll(localNodes);
    	
    	return traversalNodes;
    }
    
    public Set<Node> getOtherNodes(Set<Node> nodes) {
        Set<Node> diffNodes = new HashSet<Node>();

        for (Node node : this.nodes) {
        	if (!nodes.contains(node)) {
        		diffNodes.add(node);
        	}
        }

        return diffNodes;
    }
    
    private void reindexNodes() {
    	nodeIndex = 0;

    	for (Node node : nodes) {
    		node.setNodeID("n" + nodeIndex++);
    	}
    }

//    public Set<Node> retainNodes(Set<Node> retainedNodes) {
//        Set<Node> diffNodes = getOtherNodes(retainedNodes);
//
//        if (diffNodes.size() > 0) {
//            deleteNodes(diffNodes);
//        }
//
//        return diffNodes;
//    }

    public Set<Edge> getOtherEdges(Set<Edge> edges) {
        Set<Edge> diffEdges = new HashSet<Edge>();

        for (Node node : nodes) {
        	diffEdges.addAll(getOtherEdges(node, edges));
        }

        return diffEdges;
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
    	
    	for (Node node : nodes) {
    		Set<Edge> localDiffEdges = getOtherEdges(node, retainedEdges);

    		if (localDiffEdges.size() > 0) {
    			node.deleteEdges(localDiffEdges);

    			diffEdges.addAll(localDiffEdges);
    		}
    	}
    	
    	return diffEdges;
    }
    
    public void setNodeIndex(int nodeIndex) {
    	this.nodeIndex = nodeIndex;
    }
    
    public void deleteUnacceptableNodes() {
    	deleteUnreachableNodes();
    	
    	retainNodes(reverseDepthFirstTraversal());
    }
    
    public void deleteUnreachableNodes() {
    	retainNodes(depthFirstTraversal());
    }
    
    public void deleteNodesWithSameIDs(Collection<Node> nodes) {
    	Set<String> nodeIDs = new HashSet<String>();
    	
    	for (Node node : nodes) {
    		nodeIDs.add(node.getNodeID());
    	}
    	
    	removeNodesByID(nodeIDs);
    }
    
    public Set<Node> removeNodesByID(Set<String> nodeIDs) {
    	Set<Node> removedNodes = new HashSet<Node>();
    	
    	for (Node node : nodes) {
    		if (nodeIDs.contains(node.getNodeID())) {
    			removedNodes.add(node);
    		}
    	}
    	
    	nodes.removeAll(removedNodes);
    	
    	return removedNodes;
    }
     
    public boolean deleteNode(Node node) {
    	return nodes.remove(node);
    }
    
    public void deleteNodesWithoutSameIDs(Collection<Node> nodes) {
    	Set<String> nodeIDs = new HashSet<String>();
    	
    	for (Node node : nodes) {
    		nodeIDs.add(node.getNodeID());
    	}
    	
    	retainNodesByID(nodeIDs);
    }
    
    public boolean retainNodes(Collection<Node> nodes) {
    	boolean isChanged = this.nodes.retainAll(nodes);
    	
    	if (isChanged) {
    		if (hasNodes()) {
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
    		} else {
    			this.nodes = null;
    		}
    	}
    	
    	return isChanged;
    }
    
    public Set<Node> retainNodesByID(Set<String> nodeIDs) {
    	Set<Node> removedNodes = new HashSet<Node>();
    	
    	for (Node node : nodes) {
    		if (!nodeIDs.contains(node.getNodeID())) {
    			removedNodes.add(node);
    		}
    	}
    	
    	nodes.removeAll(removedNodes);
    	
    	return removedNodes;
    }
    
    public void unionNodes(NodeSpace space) {
    	HashMap<String, Node> idToNodeCopy = new HashMap<String, Node>();

    	for (Node node : space.getNodes()) {
    		idToNodeCopy.put(node.getNodeID(), copyNode(node));
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
}
