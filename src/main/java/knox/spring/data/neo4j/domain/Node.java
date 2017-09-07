package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
    
    public boolean deleteStartNodeType() {
    	return nodeTypes.remove(NodeType.START.getValue());
    }
    
    public boolean deleteAcceptNodeType() {
    	return nodeTypes.remove(NodeType.ACCEPT.getValue());
    }
    
    public void addStartNodeType() {
    	if (!isStartNode()) {
    		nodeTypes.add(NodeType.START.getValue());
    	}
    }
    
    public void addAcceptNodeType() {
    	if (!isAcceptNode()) {
    		nodeTypes.add(NodeType.ACCEPT.getValue());
    	}
    }
    
    public Node copy() {
    	return new Node(nodeID, new ArrayList<String>(nodeTypes));
    }

    public Edge copyEdge(Edge edge) {
    	return copyEdge(edge, edge.getHead());
    }

    public Edge copyEdge(Edge edge, Node head) {
    	return copyEdge(edge, head, edge.getComponentIDs());
    }
    
    public Edge copyEdge(Edge edge, Node head, ArrayList<String> compIDs) {
    	if (!edge.isLabeled()) {
    		Set<Edge> parallelEdges = getUnlabeledEdges(head);
    		
    		if (!parallelEdges.isEmpty()) {
    			return parallelEdges.iterator().next();
    		}
    		
    		return createEdge(head);
		} else {
			Set<Edge> parallelEdges = getLabeledEdges(head, edge.getOrientation());
			
			if (!parallelEdges.isEmpty()) {
				Edge parallelEdge = parallelEdges.iterator().next();
				
				parallelEdge.unionWithEdge(edge);
				
				return parallelEdge;
			}
			
			return createEdge(head, new ArrayList<String>(compIDs),
					new ArrayList<String>(edge.getComponentRoles()), edge.getOrientation());
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
    
    public Edge createEdge(Node head, ArrayList<String> compIDs, ArrayList<String> compRoles,
    		String orientation) {
        Edge edge = new Edge(this, head, compIDs, compRoles, orientation);
        
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
    
    public void deleteComponentIDs(Set<String> compIDs) {
    	if (hasEdges()) {
    		Set<Edge> deletedEdges = new HashSet<Edge>();

    		for (Edge edge : edges) {
    			edge.deleteComponentIDs(compIDs);

    			if (!edge.hasComponentIDs()) {
    				deletedEdges.add(edge);
    			}
    		}

    		deleteEdges(deletedEdges);
    	}
    }
    
    public void deleteComponentID(String compID) {
    	if (hasEdges()) {
    		Set<Edge> deletedEdges = new HashSet<Edge>();

    		for (Edge edge : edges) {
    			if (edge.deleteComponentID(compID) && !edge.hasComponentIDs()) {
    				deletedEdges.add(edge);
    			}
    		}

    		deleteEdges(deletedEdges);
    	}
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
    
    public Set<Edge> getEdgesWithComponentID(String compID) {
    	Set<Edge> edgesWithCompID = new HashSet<Edge>();
    	
    	if(hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.hasComponentID(compID)) {
    				edgesWithCompID.add(edge);
    			}
    		}
    	}
    	
    	return edgesWithCompID;
    }

    public boolean hasEdges() {
        return edges != null && !edges.isEmpty();
    }
    
    public Set<Edge> getEdges(Node head) {
    	Set<Edge> edgesWithHead = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead() == null) {
    				LOG.info("booga {}", "booga");
    			}
    			
    			if (edge.getHead().equals(head)) {
    				edgesWithHead.add(edge);
    			}
    		}
    	} 
    	
    	return edgesWithHead;
    }
    
    public Set<Edge> getLabeledEdges(String orientation) {
    	Set<Edge> labeledEdges = new HashSet<Edge>();
    	
    	if(hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.isLabeled() && edge.hasOrientation(orientation)) {
    				labeledEdges.add(edge);
    			}
    		}
    	}
    	
    	return labeledEdges;
    }
    
    public Set<Edge> getLabeledEdges(Node head, String orientation) {
    	Set<Edge> labeledEdgesWithHead = new HashSet<Edge>();
    	
    	for (Edge edge : getEdges(head)) {
    		if (edge.isLabeled() && edge.hasOrientation(orientation)) {
    			labeledEdgesWithHead.add(edge);
    		}
    	}
    	
    	return labeledEdgesWithHead;
    }
    
    public Set<Edge> getUnlabeledEdges() {
    	Set<Edge> unlabeledEdgesWithHead = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (!edge.isLabeled()) {
    				unlabeledEdgesWithHead.add(edge);
    			}
    		}
    	}
    	
    	return unlabeledEdgesWithHead;
    }
    
    public Set<Edge> getUnlabeledEdges(Node head) {
    	Set<Edge> unlabeledEdgesWithHead = new HashSet<Edge>();
    	
    	for (Edge edge : getEdges(head)) {
    		if (!edge.isLabeled()) {
    			unlabeledEdgesWithHead.add(edge);
    		}
    	}
    	
    	return unlabeledEdgesWithHead;
    }
    
    public boolean hasMatchingEdge(Edge edge, Node head, int tolerance, Set<String> roles) {
    	for (Edge e : getEdges(head)) {
    		if (edge.isMatching(e, tolerance, roles)) {
    			return true;
    		}
    	}

    	return false;
    }
    
    public boolean hasMatchingEdge(Edge edge, int tolerance, Set<String> roles) {
    	if (hasEdges()) {
    		for (Edge e : edges) {
    			if (edge.isMatching(e, tolerance, roles)) {
    				return true;
    			}
    		}
    		
    		return false;
    	} else {
    		return false;
    	}
    }
    
    public Set<Edge> getMatchingEdges(Edge edge, int tolerance, Set<String> roles) {
    	Set<Edge> matchingEdges = new HashSet<Edge>();
    	
    	for (Edge e : edges) {
    		if (edge.isMatching(e, tolerance, roles)) {
    			matchingEdges.add(e);
    		}
    	}

    	return matchingEdges;
    }

    public boolean hasNodeTypes() {
    	return nodeTypes != null && !nodeTypes.isEmpty(); 
    }
    
    public boolean hasConflictingType(Node node) {
    	return isAcceptNode() && node.isStartNode()
    			|| isStartNode() && node.isAcceptNode();
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
    
    public boolean deleteEdge(Edge edge) {
    	if (hasEdges()) {
    		return edges.remove(edge);
    	} else {
    		return false;
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
    
    public void unionEdges(Set<Node> nodes) {
    	for (Node node : nodes) {
			if (node.hasEdges()) {
				for (Edge edge : node.getEdges()) {
					copyEdge(edge);
				}
			}
		}
    }
}
