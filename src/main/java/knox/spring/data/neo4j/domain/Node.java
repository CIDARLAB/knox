package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NodeEntity
public class Node {
    @GraphId Long id;

    String nodeID;

    @Relationship(type = "PRECEDES") Set<Edge> edges = new HashSet<>();

//    String nodeType;
    
    ArrayList<String> nodeTypes;

    public Node() {}
    
    public Node(String nodeID) {
    	this.nodeID = nodeID;
    }
    
    public Node(String nodeID, ArrayList<String> nodeTypes) {
        this.nodeID = nodeID;
        
        this.nodeTypes = nodeTypes;
    }

    public void addEdge(Edge edge) {
    	if (edges == null) {
    		edges = new HashSet<Edge>();
    	}
    	
        edges.add(edge);
    }

    public void clearNodeTypes() { 
    	nodeTypes = null; 
    }
    
    public Node copy() {
    	return new Node(nodeID, new ArrayList<String>(nodeTypes));
    }

    public Edge copyEdge(Edge edge) {
    	return copyEdge(edge, edge.getHead());
    }

    public Edge copyEdge(Edge edge, Node head) {
    	Edge parallelEdge = getEdge(head);
    	
    	if (parallelEdge != null) {
			parallelEdge.unionWithEdge(edge);
			
			return parallelEdge;
		} else if (edge.hasComponentIDs() && edge.hasComponentRoles()) {
    		return createEdge(head, new ArrayList<String>(edge.getComponentIDs()), new ArrayList<String>(edge.getComponentRoles()));
    	} else {
    		return createEdge(head);
    	}
    }

    public Edge createEdge(Node head) {
        Edge edge = new Edge(this, head);
        addEdge(edge);
        return edge;
    }

    public Edge createEdge(Node head, ArrayList<String> compIDs, ArrayList<String> compRoles) {
        Edge edge = new Edge(this, head, compIDs, compRoles);
        addEdge(edge);
        return edge;
    }

    public Long getGraphID() { 
    	return id; 
    }

    public String getNodeID() { 
    	return nodeID; 
    }
    
    public void setNodeID(String nodeID) {
    	this.nodeID = nodeID;
    }

    public int getNumEdges() { 
    	return edges.size(); 
    }

    public Set<Edge> getEdges() { 
    	return edges; 
    }
    
    public Edge[] getEdgeArray() {
    	int numEdges = getNumEdges();
    	
    	if (numEdges > 0) {
    		return edges.toArray(new Edge[numEdges]);
    	} else {
    		return new Edge[0];
    	}
    }
    
    public void setEdges(Set<Edge> edges) {
    	this.edges = edges;
    }

    public ArrayList<String> getNodeTypes() {
    	return nodeTypes;
    }

    public boolean hasComponentID(String compID) {
        if (hasEdges()) {
            for (Edge edge : edges) {
                if (edge.hasComponentID(compID)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean hasEdges() {
        if (edges == null) {
            return false;
        } else {
            return edges.size() > 0;
        }
    }
    
    public Edge getEdge(Node head) {
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead().isIdenticalTo(head)) {
    				return edge;
    			}
    		}
    		
    		return null;
    	} else {
    		return null;
    	}
    }
    
    public boolean hasEdge(Node head) {
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead().isIdenticalTo(head)) {
    				return true;
    			}
    		}
    		
    		return false;
    	} else {
    		return false;
    	}
    }
    
    public boolean hasMatchingEdge(Edge edge, int tolerance) {
    	if (hasEdges()) {
    		for (Edge e : edges) {
    			if (edge.isMatchingTo(e, tolerance)) {
    				return true;
    			}
    		}
    		
    		return false;
    	} else {
    		return false;
    	}
    }
    
    public Set<Edge> getMatchingEdges(Edge edge, int tolerance) {
    	Set<Edge> matchingEdges = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge e : edges) {
    			if (edge.isMatchingTo(e, tolerance)) {
    				matchingEdges.add(e);
    			}
    		}
    	}
    	
    	return matchingEdges;
    }
    
    public boolean hasEdge(Edge edge) {
        if (hasEdges()) {
            for (Edge e : edges) {
                if (edge.isIdenticalTo(e)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

//    public boolean hasConflictingNodeType(Node node) {
//        return hasNodeType() &&
//            (!node.hasNodeType() || !nodeType.equals(node.getNodeType()));
//    }

    public boolean hasNodeTypes() {
    	return nodeTypes != null && !nodeTypes.isEmpty(); 
    }

    public boolean isAcceptNode() {
        return hasNodeTypes() && nodeTypes.contains(NodeType.ACCEPT.getValue());
    }

    public boolean isStartNode() {
        return hasNodeTypes() && nodeTypes.contains(NodeType.START.getValue());
    }

    public boolean isIdenticalTo(Node node) {
    	return node.getNodeID().equals(nodeID);
    }
    
    public boolean deleteEdges(Set<Edge> edges) {
    	if (hasEdges()) {
    		boolean success = this.edges.removeAll(edges);
    		
    		if (this.edges.isEmpty()) {
    			this.edges = null;
    		}
    		
    		return success;
    	} else {
    		return false;
    	}
    }

    public void clearEdges() {
        if (hasEdges()) {
            edges = null;
        }
    }

    public enum NodeType {
        START("start"),
        ACCEPT("accept");

        private final String value;
        
        NodeType(String value) { 
        	this.value = value;
        }

        public String getValue() {
        	return value; 
        }
    }

    public void addNodeType(String nodeType) { 
    	if (!hasNodeTypes()) {
    		nodeTypes = new ArrayList<String>();
    	}
    	
    	nodeTypes.add(nodeType);
    }
    
    public void mergeNodes(Set<Node> mergedNodes, HashMap<String, Set<Edge>> idToIncomingEdges) {
    	mergedNodes.remove(this);
    	
    	for (Node mergedNode : mergedNodes) {
			if (mergedNode.hasEdges()) {
				for (Edge edge : mergedNode.getEdges()) {
					addEdge(edge);
					
					edge.setTail(this);
				}
			}
			
			if (idToIncomingEdges.containsKey(mergedNode.getNodeID())) {
				Set<Edge> reassignedEdges = new HashSet<Edge>();
				
				for (Edge edge : idToIncomingEdges.get(mergedNode.getNodeID())) {
					edge.setHead(this);
					
					if (!idToIncomingEdges.containsKey(nodeID)) {
						idToIncomingEdges.put(nodeID, new HashSet<Edge>());
					}
					
					idToIncomingEdges.get(nodeID).add(edge);
					
					reassignedEdges.add(edge);
				}
				
				idToIncomingEdges.get(mergedNode.getNodeID()).removeAll(reassignedEdges);
			}
			
			if (mergedNode.isAcceptNode() && !isAcceptNode()) {
				addNodeType(NodeType.ACCEPT.getValue());
			}
			
			if (mergedNode.isStartNode()) {
				addNodeType(NodeType.START.getValue());
			}
		}
    }
}
