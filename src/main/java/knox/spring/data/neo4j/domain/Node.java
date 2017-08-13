package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import knox.spring.data.neo4j.operations.Product;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NodeEntity
public class Node {
    @GraphId Long id;

    String nodeID;

    @Relationship(type = "PRECEDES") Set<Edge> edges = new HashSet<>();
    
    ArrayList<String> nodeTypes;
    
    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    public Node() {}
    
    public Node(String nodeID) {
    	this.nodeID = nodeID;
    	
    	this.nodeTypes = new ArrayList<String>();
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
    
    public void addNodeType(String nodeType) { 
    	if (!nodeTypes.contains(nodeType)) {
    		nodeTypes.add(nodeType);
    	}
    }
    
    public boolean deleteNodeType(String nodeType) {
    	return nodeTypes.remove(nodeType);
    }

    public void clearNodeTypes() { 
    	nodeTypes.clear();
    }
    
    public Node copy() {
    	return new Node(nodeID, new ArrayList<String>(nodeTypes));
    }

    public Edge copyEdge(Edge edge) {
    	return copyEdge(edge, edge.getHead());
    }

    public Edge copyEdge(Edge edge, Node head) {
    	Set<Edge> parallelEdges = getEdges(head);
    	
    	if (edge.isBlank()) {
    		for (Edge parallelEdge : parallelEdges) {
    			if (parallelEdge.isBlank()) {
    				return parallelEdge;
    			}
    		}
    		
    		return createEdge(head);
		} else {
			for (Edge parallelEdge : parallelEdges) {
    			if (!parallelEdge.isBlank()) {
    				parallelEdge.unionWithEdge(edge);
    				
    				return parallelEdge;
    			}
    		}
			
			return createEdge(head, new ArrayList<String>(edge.getComponentIDs()),
					new ArrayList<String>(edge.getComponentRoles()));
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
    	if (hasEdges()) {
    		return edges.size(); 
    	} else {
    		return 0;
    	}
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
        return edges != null && !edges.isEmpty();
    }
    
    public Set<Edge> getEdges(Node head) {
    	Set<Edge> edgesWithHead = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead().isIdenticalTo(head)) {
    				edgesWithHead.add(edge);
    			}
    		}
    	} 
    	
    	return edgesWithHead;
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
    
    public boolean hasMatchingEdge(Edge edge, int tolerance, Set<String> roles) {
    	for (Edge e : edges) {
    		if (edge.isMatchingTo(e, tolerance, roles)) {
    			return true;
    		}
    	}

    	return false;
    }
    
    public Set<Edge> getMatchingEdges(Edge edge, int tolerance, Set<String> roles) {
    	Set<Edge> matchingEdges = new HashSet<Edge>();
    	
    	for (Edge e : edges) {
    		if (edge.isMatchingTo(e, tolerance, roles)) {
    			matchingEdges.add(e);
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

    public boolean hasNodeTypes() {
    	return nodeTypes != null && !nodeTypes.isEmpty(); 
    }
    
    public boolean hasConflictingType(Node node) {
    	return (nodeTypes.contains(NodeType.ACCEPT.getValue()) 
    					&& node.getNodeTypes().contains(NodeType.START.getValue())) 
    					|| (nodeTypes.contains(NodeType.START.getValue()) 
    								&& node.getNodeTypes().contains(NodeType.ACCEPT.getValue()));
    }

    public boolean isAcceptNode() {
        return nodeTypes.contains(NodeType.ACCEPT.getValue());
    }

    public boolean isStartNode() {
        return nodeTypes.contains(NodeType.START.getValue());
    }
    
    public boolean isSinkNode() {
    	return hasEdges();
    }

    public boolean isIdenticalTo(Node node) {
    	return node.getNodeID().equals(nodeID);
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
			
			if (mergedNode.isAcceptNode()) {
				addNodeType(NodeType.ACCEPT.getValue());
			}
			
			if (mergedNode.isStartNode()) {
				addNodeType(NodeType.START.getValue());
			}
		}
    }
    
    public boolean deleteEdges(Set<Edge> edges) {
    	if (hasEdges()) {
    		boolean isDeleted = this.edges.removeAll(edges);
    		
    		if (this.edges.isEmpty()) {
    			this.edges = null;
    		}
    		
    		return isDeleted;
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
}
