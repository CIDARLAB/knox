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
import java.util.Map;

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
	
	public NodeSpace(ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		this(0);
		
		Node startNode = createStartNode();
		
		Node acceptNode = createAcceptNode();
		
		startNode.createEdge(acceptNode, componentIDs, componentRoles);
	}

	public NodeSpace(ArrayList<String> componentIDs, ArrayList<String> componentRoles, Edge.Orientation orientation) {
		this(0);

		Node startNode = createStartNode();

		Node acceptNode = createAcceptNode();

		startNode.createEdge(acceptNode, componentIDs, componentRoles, orientation);
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
	
	public NodeSpace shallowCopy() {
		NodeSpace spaceCopy = new NodeSpace(0);
		
		spaceCopy.shallowCopyNodeSpace(this);
		
		return spaceCopy;
	}
	
	public void shallowCopyNodeSpace(NodeSpace space) {
		if (space.hasNodes()) {
			nodeIndex = space.getNodeIndex();

			nodes = new HashSet<Node>(space.getNodes());
		} else {
			nodeIndex = 0;
			
			nodes = new HashSet<Node>();
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
	
	public Node copyNodeWithID(Node node) {
		Node nodeCopy = createNode(node.getNodeID());
		
		if (node.hasNodeType()) {
			for (String nodeType : node.getNodeTypes()) {
				nodeCopy.addNodeType(nodeType);
			}
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

	public void printAllEdges() {
		Set<Edge> edges = getEdges();
		for (Edge edge : edges) {
			System.out.println(edge);
		}
	}
	
	public Set<Edge> getBlankEdges() {
		return getBlankEdges(getEdges());
	}
	
	public Set<Edge> getBlankEdges(Set<Edge> edges) {
		Set<Edge> blankEdges = new HashSet<Edge>();
		
		for (Edge edge : edges) {
			if (edge.isBlank()) {
				blankEdges.add(edge);
			}
		}

		return blankEdges;
	}
	
	public int getNodeIndex() {
		return nodeIndex;
	}
    
    public Set<Node> getNodes() {
    	return nodes;
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
    
    public Set<Node> getNonStartNodes() {
        Set<Node> nonStartNodes = new HashSet<Node>();

        if (hasNodes()) {
        	for (Node node : nodes) {
        		if (!node.isStartNode()) {
        			nonStartNodes.add(node);
        		}
        	}
        }

        return nonStartNodes;
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
        		if (!nodeIDToIncomingEdges.containsKey(node.getNodeID())) {
					nodeIDToIncomingEdges.put(node.getNodeID(), new HashSet<Edge>());
				}
        		
        		if (node.hasEdges()) {
        			for (Edge edge : node.getEdges()) {
        				if (!nodeIDToIncomingEdges.containsKey(edge.getHeadID())) {
        					nodeIDToIncomingEdges.put(edge.getHeadID(), new HashSet<Edge>());
        				}

        				nodeIDToIncomingEdges.get(edge.getHeadID()).add(edge);
        			}
        		}
        	}
        }
        
        return nodeIDToIncomingEdges;
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
    
    public List<Edge> reverseDepthFirstEdgeTraversal() {
    	HashMap<String, Set<Edge>> nodeIDToIncomingEdges = mapNodeIDsToIncomingEdges();
    	
    	List<Edge> traversalEdges = new LinkedList<Edge>();
    	
    	for (Node acceptNode : getAcceptNodes()) {
    		for (Edge acceptEdge : nodeIDToIncomingEdges.get(acceptNode.getNodeID())) {
    			if (!traversalEdges.contains(acceptEdge)) {
    				traversalEdges.addAll(acceptEdge.reverseDepthFirstTraversal(nodeIDToIncomingEdges, false));
    			}
    		}
    	}

    	return traversalEdges;
    }
    
    public List<Edge> depthFirstEdgeTraversal() {
    	List<Edge> traversalEdges = new LinkedList<Edge>();

    	for (Node startNode : getStartNodes()) {
    		if (startNode.hasEdges()) {
        		for (Edge startEdge : startNode.getEdges()) {
        			if (!traversalEdges.contains(startEdge)) {
            			traversalEdges.addAll(startEdge.depthFirstTraversal(true, false));
            		}
        		}
        	}
    		
    	}

    	return traversalEdges;
    }
    
    public Set<Edge> getFeedbackEdges(HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	Set<Edge> feedbackEdges = new HashSet<Edge>();
    	
    	for (Edge edge : getEdges()) {
    		if (!feedbackEdges.contains(edge)) {
    			feedbackEdges.addAll(edge.getCycle(nodeIDToIncomingEdges, false));
    		}
    	}
    	
    	return feedbackEdges;
    }
    
    public void deleteUnacceptableNodes() {
    	Set<Edge> traversalEdges = new HashSet<Edge>(depthFirstEdgeTraversal());
    	
    	retainEdges(traversalEdges);
    	
    	Set<Edge> reverseTraversalEdges = new HashSet<Edge>(reverseDepthFirstEdgeTraversal());
    	
    	retainEdges(reverseTraversalEdges);
    }
    
    public void deleteUnconnectedNodes() {
    	if (hasNodes()) {
    		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = mapNodeIDsToIncomingEdges();
    		
    		Set<Node> deletedNodes = new HashSet<Node>();

    		for (Node node : nodes) {
    			if (!node.hasEdges() && nodeIDToIncomingEdges.get(node.getNodeID()).isEmpty()) {
    				deletedNodes.add(node);
    			}
    		}
    		
    		deleteNodes(deletedNodes);
    	}
    }
    
    public boolean retainEdges(Set<Edge> retainedEdges) {
    	if (hasNodes()) {
    		boolean isChanged = false;
    		
    		for (Node node : nodes) {
    			if (node.hasEdges()) {
    				Set<Edge> deletedEdges = new HashSet<Edge>();

    				for (Edge edge : node.getEdges()) {
    					if (!retainedEdges.contains(edge)) {
    						deletedEdges.add(edge);
    					}
    				}
    				
    				if (!deletedEdges.isEmpty()) {
    					isChanged = true;
    					
    					node.deleteEdges(deletedEdges);
    				}
    			}
    		}
    		
    		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = mapNodeIDsToIncomingEdges();

    		Set<Node> deletedNodes = new HashSet<Node>();

    		for (Node node : nodes) {
    			if (!node.hasEdges() && nodeIDToIncomingEdges.get(node.getNodeID()).isEmpty()) {
    				deletedNodes.add(node);
    			}
    		}

    		if (!deletedNodes.isEmpty()) {
    			isChanged = true;

    			deleteNodes(deletedNodes);
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
    
    public void minimizeEdges() {
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			node.mergeEdges();
    		}
    	}
    }
    
    public Set<List<Edge>> getBlankCycles(HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	Set<List<Edge>> blankCycles = new HashSet<List<Edge>>();
    	
    	Set<Edge> blankCycleEdges = new HashSet<Edge>();
    	
    	for (Edge blankEdge : getBlankEdges()) {
    		if (!blankCycleEdges.contains(blankEdge)) {
    			List<Edge> blankCycle = blankEdge.getCycle(nodeIDToIncomingEdges, true);
        		
        		if (!blankCycle.isEmpty()) {
        			blankCycles.add(blankCycle);
        			
        			blankCycleEdges.addAll(blankCycle);
        		}
    		}
    	}
    	
    	return blankCycles;
    }
    
    @SuppressWarnings("unchecked")
	public void removeBlankCycles() {
    	HashMap<String, Set<Edge>> nodeIDToIncomingEdges = mapNodeIDsToIncomingEdges();
    	
    	Set<List<Edge>> blankCycles = getBlankCycles(nodeIDToIncomingEdges);
    	
    	for (List<Edge> blankCycle : blankCycles) {
    		Node outerSource = createNode();
    		Node outerSink = createNode();
    		Node innerSource = createNode();
    		Node innerSink = createNode();
    		
    		outerSource.createEdge(innerSource);
    		outerSource.createEdge(outerSink);
    		innerSink.createEdge(outerSink);
    		innerSink.createEdge(innerSource);
    		
    		Set<Edge> incomingEdges = new HashSet<Edge>();
    		Set<Edge> outgoingEdges = new HashSet<Edge>();
    		
    		Set<Edge> sourceEdges = new HashSet<Edge>();
    		Set<Edge> sinkEdges = new HashSet<Edge>();
    		
    		Set<Edge> blankCycleEdges = new HashSet<Edge>(blankCycle);
    		
    		Set<Node> cycleNodes = new HashSet<Node>();
    		   
    		for (Edge blankEdge : blankCycle) {
    			cycleNodes.add(blankEdge.getTail());
    			cycleNodes.add(blankEdge.getHead());
    		}
    		
    		for (Node cycleNode : cycleNodes) {
    			incomingEdges.addAll(cycleNode.getOtherIncomingEdges(nodeIDToIncomingEdges, blankCycleEdges));
    		}
    		
    		for (Node cycleNode : cycleNodes) {
    			for (Edge outgoingEdge : cycleNode.getOtherEdges(blankCycleEdges)) {
    				Set<Edge> traversalEdges = new HashSet<Edge>(outgoingEdge.depthFirstTraversal(true, false));
    
    				traversalEdges.retainAll(incomingEdges);
    				
    				if (traversalEdges.isEmpty()) {
    					outgoingEdges.add(outgoingEdge);
    				} else {
    					sourceEdges.add(outgoingEdge);
						sinkEdges.addAll(traversalEdges);
    				}
    			}
    		}
    		
    		incomingEdges.removeAll(sinkEdges);
    		
    		Set<Node> mergeNodes = new HashSet<Node>();
    		
    		if (!sourceEdges.isEmpty()) {
    			for (Edge sourceEdge : sourceEdges) {
    				if (sourceEdge.getTail().isAcceptNode()) {
    					outerSink.addNodeType(NodeType.ACCEPT.getValue());
    				} else if (sourceEdge.getTail().isStartNode()) {
    					outerSource.addNodeType(NodeType.START.getValue());
    				}
    				
    				sourceEdge.delete();
    				
    				sourceEdge.setTail(innerSource);
    				
    				innerSource.addEdge(sourceEdge);
    			}
    			
    			mergeNodes.add(innerSource);
    		}
    		
    		if (!sinkEdges.isEmpty()) {
    			nodeIDToIncomingEdges.put(innerSink.getNodeID(), new HashSet<Edge>());

    			for (Edge sinkEdge : sinkEdges) {
    				if (sinkEdge.getHead().isAcceptNode()) {
    					outerSink.addNodeType(NodeType.ACCEPT.getValue());
    				} else if (sinkEdge.getHead().isStartNode()) {
    					outerSource.addNodeType(NodeType.START.getValue());
    				}
    				
    				nodeIDToIncomingEdges.get(sinkEdge.getHeadID()).remove(sinkEdge);
    				
    				sinkEdge.setHead(innerSink);

    				nodeIDToIncomingEdges.get(innerSink.getNodeID()).add(sinkEdge);

    				mergeNodes.add(sinkEdge.getTail());
    			}
    		}
    		
    		if (!incomingEdges.isEmpty()) {
    			nodeIDToIncomingEdges.put(outerSource.getNodeID(), new HashSet<Edge>());
    			
    			for (Edge incomingEdge : incomingEdges) {
    				if (incomingEdge.getHead().isAcceptNode()) {
    					outerSink.addNodeType(NodeType.ACCEPT.getValue());
    				} else if (incomingEdge.getHead().isStartNode()) {
    					outerSource.addNodeType(NodeType.START.getValue());
    				}
    				
    				nodeIDToIncomingEdges.get(incomingEdge.getHeadID()).remove(incomingEdge);
    				
    				incomingEdge.setHead(outerSource);
    				
    				nodeIDToIncomingEdges.get(outerSource.getNodeID()).add(incomingEdge);
    				
    				mergeNodes.add(incomingEdge.getTail());
    			}
    		}
    		
    		if (!outgoingEdges.isEmpty()) {
    			for (Edge outgoingEdge : outgoingEdges) {
    				if (outgoingEdge.getTail().isAcceptNode()) {
    					outerSink.addNodeType(NodeType.ACCEPT.getValue());
    				} else if (outgoingEdge.getTail().isStartNode()) {
    					outerSource.addNodeType(NodeType.START.getValue());
    				}
    				
    				outgoingEdge.delete();
    				
    				outgoingEdge.setTail(outerSink);
    				
    				outerSink.addEdge(outgoingEdge);
    			}
    			
    			mergeNodes.add(outerSink);
    		}
    		
    		deleteNodes(cycleNodes);
    		
    		for (Node node : mergeNodes) {
    			node.mergeEdges();
    		}
    		
    		Set<Edge> blankEdges = new HashSet<Edge>();
    		
    		blankEdges.addAll(outerSink.getBlankEdges());
    		blankEdges.addAll(innerSource.getBlankEdges());
    		blankEdges.addAll(outerSource.getIncomingBlankEdges(nodeIDToIncomingEdges));
    		blankEdges.addAll(innerSink.getIncomingBlankEdges(nodeIDToIncomingEdges));
    		
    		deleteBlankEdges(blankEdges);
    	}
    }
    
    public void deleteBlankEdges(Set<Edge> blankEdges) {
		Set<Node> primaryMergeNodes = new HashSet<Node>();
		Set<Node> secondaryMergeNodes = new HashSet<Node>();
		
		Set<Node> redundantStartNodes = new HashSet<Node>();
		Set<Node> redundantAcceptNodes = new HashSet<Node>();
		
		HashMap<String, Set<Edge>> nodeIDToIncomingEdges = mapNodeIDsToIncomingEdges();
		
		for (Edge blankEdge : blankEdges) {
			if (deleteBlankEdge(blankEdge, nodeIDToIncomingEdges)) {
				primaryMergeNodes.remove(blankEdge.getHead());
				secondaryMergeNodes.remove(blankEdge.getHead());
				
				primaryMergeNodes.add(blankEdge.getTail());
				
				for (Edge incomingEdge : nodeIDToIncomingEdges.get(blankEdge.getTailID())) {
					secondaryMergeNodes.add(incomingEdge.getTail());
				}
			} else {
				if (blankEdge.getTail().isStartNode() && blankEdge.getHead().isStartNode()) {
		    		redundantStartNodes.add(blankEdge.getHead());
				}

				if (blankEdge.getTail().isAcceptNode() && blankEdge.getHead().isAcceptNode()) {
					redundantAcceptNodes.add(blankEdge.getTail());
				}
			}
		}
		
		for (Node node : primaryMergeNodes) {
			node.mergeEdges();
			
			for (Edge blankEdge : node.getBlankEdges()) {
				if (blankEdge.getTail().isStartNode() && blankEdge.getHead().isStartNode()) {
		    		redundantStartNodes.add(blankEdge.getHead());
				}

				if (blankEdge.getTail().isAcceptNode() && blankEdge.getHead().isAcceptNode()) {
					redundantAcceptNodes.add(blankEdge.getTail());
				}
			}
		}
		
		for (Node node : secondaryMergeNodes) {
			node.mergeEdges();
			
			for (Edge blankEdge : node.getBlankEdges()) {
				if (blankEdge.getTail().isStartNode() && blankEdge.getHead().isStartNode()) {
		    		redundantStartNodes.add(blankEdge.getHead());
				}

				if (blankEdge.getTail().isAcceptNode() && blankEdge.getHead().isAcceptNode()) {
					redundantAcceptNodes.add(blankEdge.getTail());
				}
			}
		}
		
		for (Node startNode : redundantStartNodes) {
			startNode.deleteStartNodeType();
		}
		for (Node acceptNode : redundantAcceptNodes) {
			acceptNode.deleteAcceptNodeType();
		}
    }
    
    public boolean deleteBlankEdge(Edge edge, HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	if (((nodeIDToIncomingEdges.containsKey(edge.getHeadID()) && nodeIDToIncomingEdges.get(edge.getHeadID()).size() == 1)
    				|| edge.getTail().getNumEdges() == 1) 
    			&& !edge.getTail().hasDiffNodeType(edge.getHead())
    			&& (!edge.getTail().isAcceptNode() || edge.getHead().isAcceptNode() 
    					|| nodeIDToIncomingEdges.get(edge.getHeadID()).size() == 1)
    			&& (!edge.getHead().isStartNode() || edge.getTail().isStartNode() 
    					|| edge.getTail().getNumEdges() == 1)
    			&& (!edge.getTail().isStartNode() 
    					|| (nodeIDToIncomingEdges.containsKey(edge.getHeadID()) && nodeIDToIncomingEdges.get(edge.getHeadID()).size() == 1)
    					|| (nodeIDToIncomingEdges.containsKey(edge.getTailID()) && !nodeIDToIncomingEdges.get(edge.getTailID()).isEmpty()))) {
    		edge.delete();
    		
    		Set<Edge> headEdges = edge.getHead().removeEdges();
    		
    		for (Edge headEdge : headEdges) {
    			edge.getTail().addEdge(headEdge);
    			
    			headEdge.setTail(edge.getTail());
    		}
    		
    		Set<Edge> incomingHeadEdges = new HashSet<Edge>();
    		
    		if (nodeIDToIncomingEdges.containsKey(edge.getHeadID())) {
    			for (Edge incomingHeadEdge : nodeIDToIncomingEdges.get(edge.getHeadID())) {
    				if (incomingHeadEdge != edge) {
    					incomingHeadEdge.setHead(edge.getTail());

    					incomingHeadEdges.add(incomingHeadEdge);
    				}
    			}
    		}
    		
    		if (!incomingHeadEdges.isEmpty()) {
    			if (!nodeIDToIncomingEdges.containsKey(edge.getTailID())) {
    				nodeIDToIncomingEdges.put(edge.getTailID(), new HashSet<Edge>());
    			}
    			
    			nodeIDToIncomingEdges.get(edge.getTailID()).addAll(incomingHeadEdges);
    		}
    		
    		if (edge.getHead().hasNodeType()) {
    			edge.getTail().copyNodeType(edge.getHead());
    		}
    		
    		deleteNode(edge.getHead());
    		
    		nodeIDToIncomingEdges.remove(edge.getHeadID());
    		
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public String toString() {
    	String spaceMap = "";
    	
    	Set<Edge> edges = getEdges();
    	
    	for (Edge edge : edges) {
    		spaceMap = spaceMap + edge.toString() + "\n";
    	}
    	
    	return spaceMap;
    }

	public void weightBlankEdges() {
		
		Set<Edge> blankEdges = getBlankEdges();

		// Remove current weight
		for (Edge e : blankEdges) {
			e.emptyWeight();
		}

		// Determine weights for blankEdges
		for (Edge edge : blankEdges) {
			avgWeight(edge);

		}


	}

	public void avgWeight(Edge edge){

		Node head = edge.getHead();

		Set<Edge> allEdges = head.getEdges();

		double weight = 0;

		int countEdges = 0;

		for (Edge e : allEdges) {
			if (edge.getHeadID() == e.getTailID()) {
				if (e.isBlank() && e.weight == 0) {
					avgWeight(e);

					weight += e.weight;
					countEdges += 1;
				}

				else {
					weight += e.weight;
					countEdges += 1;
				}
			}
		}

		double averageWeight = weight / countEdges;
		edge.weight = averageWeight;

	}

	public String getTotalScoreOfNonBlankEdges() {
		Set<Edge> allEdges = getEdges();
		Set<Edge> blankEdges = getBlankEdges();

		double totalWeight = 0;
		double blankEdgesTotalWeight = 0;

		for (Edge e : allEdges) {
			totalWeight += e.weight;
		}

		for (Edge b : blankEdges) {
			blankEdgesTotalWeight += b.weight;
		}

		return String.valueOf(totalWeight - blankEdgesTotalWeight);
	}

	public String getTotalScoreOfEdges() {
		Set<Edge> allEdges = getEdges();

		double totalWeight = 0;

		for (Edge e : allEdges) {
			totalWeight += e.weight;
		}

		return String.valueOf(totalWeight);
	}

	public String getAvgScoreofAllNonBlankEdges() {
		Set<Edge> allEdges = getEdges();

		double totalNumberOfNonBlankEdges = 0;
		double totalWeightOfNonBlankEdges = 0;

		for (Edge e : allEdges) {
			if (!e.isBlank()) {
				totalNumberOfNonBlankEdges = totalNumberOfNonBlankEdges + 1;
				totalWeightOfNonBlankEdges = totalWeightOfNonBlankEdges + e.weight;
			}
		}

		return String.valueOf(totalWeightOfNonBlankEdges / totalNumberOfNonBlankEdges);
	}

	public String getAvgScoreofAllEdges() {
		Set<Edge> allEdges = getEdges();

		double totalNumberOfEdges = 0;
		double totalWeightOfEdges = 0;

		for (Edge e : allEdges) {
			totalNumberOfEdges = totalNumberOfEdges + 1;
			totalWeightOfEdges = totalWeightOfEdges + e.weight;
		}

		return String.valueOf(totalWeightOfEdges / totalNumberOfEdges);
	}

	public void clearNodeWeights() {
		Set<Node> nodes = getNodes();
		for (Node node: nodes) {
			node.hasWeight = false;
		}
	}

}
