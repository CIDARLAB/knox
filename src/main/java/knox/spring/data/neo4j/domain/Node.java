package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
    
    public boolean deleteStartNodeType() {
    	return deleteNodeType(NodeType.START.getValue());
    }
    
    public boolean deleteAcceptNodeType() {
    	return deleteNodeType(NodeType.ACCEPT.getValue());
    }
    
    public Node copy() {
    	return new Node(nodeID, new ArrayList<String>(nodeTypes));
    }

    public Edge copyEdge(Edge edge) {
    	return copyEdge(edge, edge.getHead());
    }
    
    public Edge copyEdge(Edge edge, Node head) {
    	return createEdge(head, new ArrayList<String>(edge.getComponentIDs()),
    			new ArrayList<String>(edge.getComponentRoles()), edge.getOrientation());
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
    
    public void setEdges(Set<Edge> edges) {
    	this.edges = edges;
    }

    public ArrayList<String> getNodeTypes() {
    	return nodeTypes;
    }

    public boolean hasEdges() {
        return edges != null && !edges.isEmpty();
    }
    
    public boolean hasIncomingEdges(HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	return !nodeIDToIncomingEdges.get(nodeID).isEmpty();
    }
    
    public boolean hasEdge(Node head) {
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead() == head) {
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }
    
    public boolean hasBlankEdge() {  
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.isBlank()) {
    				return true;
    			}
    		}
    	}

		return false;
    }
    
    public boolean hasBlankEdge(Node head) {  
    	if (hasEdges()) {
    		for (Edge edge : getEdges(head)) {
    			if (edge.isBlank()) {
    				return true;
    			}
    		}
    	}

		return false;
    }
    
    public Set<Edge> getEdges(Set<Edge> targetEdges) {
    	Set<Edge> edges = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : this.edges) {
    			if (targetEdges.contains(edge))  {
    				edges.add(edge);
    			}
    		}
    	}
    	
    	return edges;
    }
    
    public Set<Edge> getIncomingEdges(HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	Set<Edge> incomingEdges = new HashSet<Edge>(nodeIDToIncomingEdges.get(nodeID));
    	
    	return incomingEdges;
    }
    
    public Set<Edge> getIncomingEdges(Set<Edge> targetEdges, HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
    	Set<Edge> incomingEdges = new HashSet<Edge>();
    	
    	for (Edge incomingEdge : nodeIDToIncomingEdges.get(nodeID)) {
    		if (targetEdges.contains(incomingEdge))  {
    			incomingEdges.add(incomingEdge);
    		}
    	}
    	
    	return incomingEdges;
    }
    
    @SuppressWarnings("unchecked")
	public Set<Edge> getOtherEdges(Set<Edge>... edges) {
    	Set<Edge> otherEdges = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		otherEdges.addAll(this.edges);
    		
    		for (Set<Edge> e : edges) {
    			otherEdges.removeAll(e);
    		}
    	}
    	
    	return otherEdges;
    }
    
    @SuppressWarnings("unchecked")
	public Set<Edge> getOtherIncomingEdges(HashMap<String, Set<Edge>> nodeIDToIncomingEdges, Set<Edge>... incomingEdges) {
    	Set<Edge> otherIncomingEdges = new HashSet<Edge>(nodeIDToIncomingEdges.get(nodeID));
    	
    	for (Set<Edge> e : incomingEdges) {
    		otherIncomingEdges.removeAll(e);
    	}
    	
    	return otherIncomingEdges;
    }
    
    @SuppressWarnings("unchecked")
	public boolean hasOtherEdges(Set<Edge>... edges) {
    	Set<Edge> otherEdges = new HashSet<Edge>();

    	if (hasEdges()) {
    		otherEdges.addAll(this.edges);

    		for (Set<Edge> e : edges) {
    			otherEdges.removeAll(e);
    		}
    	}

    	return !otherEdges.isEmpty();
    }
    
    @SuppressWarnings("unchecked")
	public boolean hasOtherIncomingEdges(HashMap<String, Set<Edge>> nodeIDToIncomingEdges, Set<Edge>... incomingEdges) {
    	Set<Edge> otherIncomingEdges = new HashSet<Edge>(nodeIDToIncomingEdges.get(nodeID));

    	for (Set<Edge> e : incomingEdges) {
    		otherIncomingEdges.removeAll(e);
    	}

    	return !otherIncomingEdges.isEmpty();
    }
    
    public Set<Edge> getEdges(Node head) {
    	Set<Edge> edgesWithHead = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.getHead() == head) {
    				edgesWithHead.add(edge);
    			}
    		}
    	} 
    	
    	return edgesWithHead;
    }
    
    public Set<Edge> getEdges(String orientation) {
    	Set<Edge> edgesWithHead = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.hasOrientation(orientation)) {
    				edgesWithHead.add(edge);
    			}
    		}
    	} 
    	
    	return edgesWithHead;
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

    public boolean hasNodeType() {
    	return nodeTypes != null && !nodeTypes.isEmpty(); 
    }
    
    public boolean hasDiffNodeType(Node node) {
    	if (!hasNodeType() || !node.hasNodeType()) {
    		return false;
    	} else {
    		Set<String> sharedNodeTypes = new HashSet<String>(nodeTypes);

    		sharedNodeTypes.retainAll(node.getNodeTypes());

    		return sharedNodeTypes.isEmpty();
    	}
    }

    public boolean isAcceptNode() {
        return nodeTypes.contains(NodeType.ACCEPT.getValue());
    }

    public boolean isStartNode() {
        return nodeTypes.contains(NodeType.START.getValue());
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
    
    public boolean deleteBlankEdges() {
    	return deleteEdges(getBlankEdges());
    }
    
    public boolean deleteEdges(Node head) {
    	return deleteEdges(getEdges(head));
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
    
    public void mergeEdges() {
    	if (hasEdges()) {
    		HashMap<String, Set<Edge>> codeToIncomingEdges = new HashMap<String, Set<Edge>>();
    		HashMap<String, Set<List<Edge>>> codeToIncomingPaths = new HashMap<String, Set<List<Edge>>>();
    		
    		for (Edge edge : edges) {
    			if (!edge.isBlank()) {
    				String code = edge.getHeadID() + edge.getOrientation();

    				if (!codeToIncomingEdges.containsKey(code)) {
    					codeToIncomingEdges.put(code, new HashSet<Edge>());
    				}

    				codeToIncomingEdges.get(code).add(edge);
    			} else {
    				List<List<Edge>> blankPaths = edge.getBlankPaths();

    				for (List<Edge> blankPath : blankPaths) {
    					String code = blankPath.get(blankPath.size() - 1).getHead().getNodeID() + edge.getOrientation();

    					if (!codeToIncomingPaths.containsKey(code)) {
    						codeToIncomingPaths.put(code, new HashSet<List<Edge>>());
    					}

    					codeToIncomingPaths.get(code).add(blankPath);
    				}
    			}
    		}
    		
    		for (String code : codeToIncomingEdges.keySet()) {
    			if (codeToIncomingEdges.get(code).size() > 1) {
    				
    				Iterator<Edge> edgerator = codeToIncomingEdges.get(code).iterator();
    				
    				Edge edge = edgerator.next();
    				
    				while (edgerator.hasNext()) {
    					Edge deletedEdge = edgerator.next();
    					
    					edge.unionWithEdge(deletedEdge);
    					
    					deleteEdge(deletedEdge);
    				}
    			}
    		}
    		
    		for (String code : codeToIncomingPaths.keySet()) {
    			if (codeToIncomingPaths.get(code).size() > 1) {
    				Set<List<Edge>> paths = codeToIncomingPaths.get(code);
    				
    				List<Edge> longestPath = paths.iterator().next();
    				
    				for (List<Edge> path : paths) {
    					if (path.size() > longestPath.size()) {
    						longestPath = path;
    					}
    				}
    				
    				if (longestPath.size() == 1) {
    					for (List<Edge> path : paths) {
        					if (path != longestPath) {
            					deleteEdge(path.get(0));
        					}
        				}
    				} else if (longestPath.size() > 1){
    					for (List<Edge> path : paths) {
    						if (path.size() == 1) {
    							deleteEdge(path.get(0));
    						}
    					}
    				}
    			}
    		}
    	}
    }
    
    public Set<Edge> removeEdges() {
    	Set<Edge> removedEdges = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			removedEdges.add(edge);
    		}
    		
    		edges.removeAll(removedEdges);
    	}
    	
    	return removedEdges;
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
    
    public Set<Edge> copyEdges(Node node) {
    	Set<Edge> edgeCopies = new HashSet<Edge>();
    	
    	if (node.hasEdges()) {
			for (Edge edge : node.getEdges()) {
				edgeCopies.add(copyEdge(edge));
			}
		}
    	
    	return edgeCopies;
    }
    
    public void copyEdges(Set<Node> nodes) {
    	for (Node node : nodes) {
			copyEdges(node);
		}
    }
    
    public void copyNodeType(Node node) {
    	nodeTypes = new ArrayList<String>();
    	
    	nodeTypes.addAll(node.getNodeTypes());
    }
    
    public Set<Edge> getOtherBlankEdges(Set<Edge> blankEdges) {
    	Set<Edge> otherBlankEdges = getBlankEdges();
    	
    	otherBlankEdges.removeAll(blankEdges);
    	
    	return otherBlankEdges;
    }
    
    public Set<Edge> getBlankEdges() {
    	Set<Edge> blankEdges = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (edge.isBlank()) {
    				blankEdges.add(edge);
    			}
    		}
    	}
    	
    	return blankEdges;
    }
    
    public Set<Edge> getNonBlankEdges() {
    	Set<Edge> nonBlankEdges = new HashSet<Edge>();

    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (!edge.isBlank()) {
    				nonBlankEdges.add(edge);
    			}
    		}
    	}

    	return nonBlankEdges;
    }
    
    public Set<Edge> getOtherNonBlankEdges(Set<Edge> edges) {
    	Set<Edge> otherNonBlankEdges = getNonBlankEdges();
    	
    	otherNonBlankEdges.removeAll(edges);
    	
    	return otherNonBlankEdges;
    }
    
    public Set<Edge> getNonBlankEdges(Set<Node> heads) {
    	Set<Edge> nonBlankEdges = new HashSet<Edge>();
    	
    	if (hasEdges()) {
    		for (Edge edge : edges) {
    			if (!edge.isBlank() && heads.contains(edge.getHead())) {
    				nonBlankEdges.add(edge);
    			}
    		}
    	}
    	
    	return nonBlankEdges;
    }
}
