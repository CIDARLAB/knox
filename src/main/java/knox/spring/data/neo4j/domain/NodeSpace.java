package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import knox.spring.data.neo4j.domain.Node.NodeType;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NodeEntity
public class NodeSpace {
	@GraphId
    Long id;
	
	int idIndex;
	
	@Relationship(type = "CONTAINS") 
    Set<Node> nodes;
	
	public NodeSpace() {
		
	}
	
	public NodeSpace(int idIndex) {
		this.idIndex = idIndex;
	}
	
	public void addNode(Node node) {
		if (nodes == null) {
			nodes = new HashSet<Node>();
		}
		nodes.add(node);
	}
	
	public void clearNodes() {
    	if (hasNodes()) {
    		nodes = null;
    		
    		idIndex = 0;
    	}
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
		if (node.hasNodeType()) {
			return createTypedNode(node.getNodeID(), node.getNodeType());
		} else {
			return createNode(node.getNodeID());
		} 
	}
	
	public Node copyNode(Node node) {
		if (node.hasNodeType()) {
			return createTypedNode(node.getNodeType());
		} else {
			return createNode();
		}
	}
	
	public Node createNode() {
		Node node = new Node("n" + idIndex++);
		addNode(node);
		return node;
	}
	
	public Node createNode(String nodeID) {
		Node node = new Node(nodeID);
		addNode(node);
		return node;
	}
	
	public Node createTypedNode(String nodeType) {
		Node node = new Node("n" + idIndex++, nodeType);
		addNode(node);
		return node;
	}
	
	public Node createTypedNode(String nodeID, String nodeType) {
		Node node = new Node(nodeID, nodeType);
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
		if (hasNodes()) {
    		return nodes.removeAll(deletedNodes);
    	} else {
    		return false;
    	}
	}
	
	public boolean detachDeleteNodes(Set<Node> deletedNodes) {	
		if (hasNodes()) {
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
	
	public Set<Edge> getMinimizableEdges(Node node, HashMap<String, Set<Edge>> nodeIDsToIncomingEdges) {
		 Set<Edge> minimizableEdges = new HashSet<Edge>();

		 if (nodeIDsToIncomingEdges.containsKey(node.getNodeID())) {
			 Set<Edge> incomingEdges = nodeIDsToIncomingEdges.get(node.getNodeID());

			 if (incomingEdges.size() == 1) {
				 Edge incomingEdge = incomingEdges.iterator().next();

				 Node predecessor = incomingEdge.getTail();

				 if (!incomingEdge.hasComponents() && !incomingEdge.isCyclic()
						 && (predecessor.getNumEdges() == 1 || !node.hasConflictingNodeType(predecessor))) {
					 minimizableEdges.add(incomingEdge);
				 }
			 } else if (incomingEdges.size() > 1) {
				 for (Edge incomingEdge : incomingEdges) {
					 Node predecessor = incomingEdge.getTail();

					 if (!incomingEdge.hasComponents() && !incomingEdge.isCyclic() 
							 && predecessor.getNumEdges() == 1 && !predecessor.hasConflictingNodeType(node)) {
						 minimizableEdges.add(incomingEdge);
					 }
				 }
			 }
		 }

		 return minimizableEdges;
	 }
	
	public int getIdIndex() {
		return idIndex;
	}
	
	public Node getNode(String nodeID) {
		if (hasNodes()) {
			for (Node node : nodes) {
				if (node.getNodeID().equals(nodeID)) {
					return node;
				}
			}
			return null;
		} else {
			return null;
		}
	}
    
    public Set<Node> getNodes() {
    	return nodes;
    }
    
    public int getSize() {
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
            return null;
        } else {
            return null;
        }
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
                if (node.hasNodeType()) {
                    typedNodes.add(node);
                }
            }
        }

        return typedNodes;
    }

    public boolean hasNodes() {
        if (nodes == null) {
            return false;
        } else {
            return nodes.size() > 0;
        }
    }

    public HashMap<String, Set<Edge>> mapNodeIDsToIncomingEdges() {
        HashMap<String, Set<Edge>> nodeIDToIncomingEdges =
            new HashMap<String, Set<Edge>>();
        if (hasNodes()) {
            for (Node node : nodes) {
                if (node.hasEdges()) {
                    for (Edge edge : node.getEdges()) {
                        Node successor = edge.getHead();
                        if (!nodeIDToIncomingEdges.containsKey(
                                successor.getNodeID())) {
                            nodeIDToIncomingEdges.put(successor.getNodeID(),
                                                      new HashSet<Edge>());
                        }
                        nodeIDToIncomingEdges.get(successor.getNodeID())
                            .add(edge);
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
    
    public void labelAcceptNodes() {
    	if (hasNodes()) {
    		for (Node node : nodes) {
    			if (!node.hasEdges()) {
    				node.setNodeType(Node.NodeType.ACCEPT.getValue());
    			}
    		}
    	}
    }
    
    public void labelStartNodes() {
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
    			if (!successorIDs.contains(node.getNodeID())) {
    				node.setNodeType(Node.NodeType.START.getValue());
    			}
    		}
    	}
    }
    
    public void mergeStartNodes() {
    	if (hasNodes()) {
    		Iterator<Node> startNodes = getStartNodes().iterator();
    		
    		Node primaryStart = startNodes.next();
    		
    		while (startNodes.hasNext()) {
    			Node secondaryStart = startNodes.next();
    			
    			if (secondaryStart.hasEdges()) {
    				for (Edge secondaryEdge : secondaryStart.getEdges()) {
    					primaryStart.copyEdge(secondaryEdge);
    				}
    			}
    			
    			nodes.remove(secondaryStart);
    		}
    	}
    }
    
    public List<Node> orderNodes() {
    	List<Node> orderedNodes;
    	
    	if (hasNodes()) {
    		orderedNodes = new ArrayList<Node>(nodes.size());
        	
        	Stack<Node> nodeStack = new Stack<Node>();
        	
        	for (Node startNode : getStartNodes()) {
        		nodeStack.push(startNode);
        	}
        	
        	Set<String> visitedIDs = new HashSet<String>();
        	
        	while (!nodeStack.isEmpty()) {
        		Node node = nodeStack.pop();
        		
        		orderedNodes.add(node);
        		
        		visitedIDs.add(node.getNodeID());
        		
        		if (node.hasEdges()) {
        			for (Edge edge : node.getEdges()) {
        				if (!visitedIDs.contains(edge.getHead().getNodeID())) {
        					nodeStack.push(edge.getHead());
        				}
        			}
        		}
        	}
    	} else {
    		orderedNodes = new ArrayList<Node>(0);
    	}
    	
    	return orderedNodes;
    }
    
    public Set<Node> getOtherNodes(Set<Node> nodes) {
        Set<Node> diffNodes = new HashSet<Node>();

        if (hasNodes()) {
            for (Node node : this.nodes) {
                if (!nodes.contains(node)) {
                    diffNodes.add(node);
                }
            }
        }

        return diffNodes;
    }

    public Set<Node> retainNodes(Set<Node> retainedNodes) {
        Set<Node> diffNodes = getOtherNodes(retainedNodes);

        if (diffNodes.size() > 0) {
            deleteNodes(diffNodes);
        }

        return diffNodes;
    }

    public Set<Edge> getOtherEdges(Set<Edge> edges) {
        Set<Edge> diffEdges = new HashSet<Edge>();

        if (hasNodes()) {
            for (Node node : nodes) {
                diffEdges.addAll(getOtherEdges(node, edges));
            }
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
    
    public void setIDIndex(int idIndex) {
    	this.idIndex = idIndex;
    }
    
    public void deleteUnreachableNodes() {
    	deleteNodesWithoutSameIDs(orderNodes());
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
    	
    	if (nodes.isEmpty()) {
    		nodes = null;
    	}
    	
    	return removedNodes;
    }
    
    public void deleteNodesWithoutSameIDs(Collection<Node> nodes) {
    	Set<String> nodeIDs = new HashSet<String>();
    	
    	for (Node node : nodes) {
    		nodeIDs.add(node.getNodeID());
    	}
    	
    	retainNodesByID(nodeIDs);
    }
    
    public Set<Node> retainNodesByID(Set<String> nodeIDs) {
    	Set<Node> removedNodes = new HashSet<Node>();
    	
    	for (Node node : nodes) {
    		if (!nodeIDs.contains(node.getNodeID())) {
    			removedNodes.add(node);
    		}
    	}
    	
    	nodes.removeAll(removedNodes);
    	
    	if (nodes.isEmpty()) {
    		nodes = null;
    	}
    	
    	return removedNodes;
    }
    
    public void union(NodeSpace space) {
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
			
			idIndex = Math.max(idIndex, space.getIdIndex());
		}
    }
}
