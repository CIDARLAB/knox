package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@RelationshipEntity(type = "PRECEDES")
public class Edge {
    @GraphId Long id;

    @StartNode Node tail;

    @EndNode Node head;

    ArrayList<String> componentIDs;
    
    ArrayList<String> componentRoles;
    
    String orientation;

    double weight;
    
    private static final Logger LOG = LoggerFactory.getLogger(Edge.class);

    public Edge() {
    	
    }

    public Edge(Node tail, Node head) {
        this.tail = tail;
        
        this.head = head;
        
        componentIDs = new ArrayList<String>();
        
        componentRoles = new ArrayList<String>();
        
        orientation = Orientation.NONE.getValue();
        
        weight = 1.0;
    }
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, 
    		ArrayList<String> componentRoles) {
		this.tail = tail;
		
		this.head = head;
		
		this.componentIDs = componentIDs;
		
		this.componentRoles = componentRoles;
		
		if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
			orientation = Orientation.INLINE.getValue();
		} else {
			orientation = Orientation.NONE.getValue();
		}
		
        weight = 1.0;
	}
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, 
    		ArrayList<String> componentRoles, String orientation) {
		this.tail = tail;
		
		this.head = head;
		
		this.componentIDs = componentIDs;
		
		this.componentRoles = componentRoles;
		
		if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
			this.orientation = orientation;
		} else {
			this.orientation = Orientation.NONE.getValue();
		}
        
        weight = 1.0;
	}
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, 
    		ArrayList<String> componentRoles, String orientation, double weight) {
    	this.tail = tail;

    	this.head = head;

    	this.componentIDs = componentIDs;

    	this.componentRoles = componentRoles;

    	if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
			this.orientation = orientation;
		} else {
			this.orientation = Orientation.NONE.getValue();
		}

    	this.weight = weight;
    }
    
    public Edge(ArrayList<String> componentIDs, ArrayList<String> componentRoles, String orientation, 
    		double weight) {
    	this.componentIDs = componentIDs;

    	this.componentRoles = componentRoles;

    	if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
			this.orientation = orientation;
		} else {
			this.orientation = Orientation.NONE.getValue();
		}

    	this.weight = weight;
    }
    
    public Edge copy() {
    	return new Edge(new ArrayList<String>(componentIDs),
    			new ArrayList<String>(componentRoles), orientation, weight);
    }
    
    public Edge copy(Node head) {
    	return new Edge(tail, head, new ArrayList<String>(componentIDs),
    			new ArrayList<String>(componentRoles), orientation, weight);
    }

    public Edge copy(Node tail, Node head) {
    	return new Edge(tail, head, new ArrayList<String>(componentIDs),
    			new ArrayList<String>(componentRoles), orientation, weight);
    }
    
    public void delete() {
    	tail.deleteEdge(this);
    }
    
    public void diffWithEdge(Edge edge, int tolerance) {
    	Set<String> diffComponentIDs = new HashSet<String>(edge.getComponentIDs());
    	Set<String> diffComponentRoles = new HashSet<String>(edge.getComponentRoles());

    	for (int i = 0; i < componentRoles.size(); i++) {
    		if (i < componentIDs.size()) {
    			if (diffComponentIDs.contains(componentIDs.get(i))
    					|| tolerance >= 2 && diffComponentRoles.contains(componentRoles.get(i))) {
    				componentIDs.remove(i);
    				componentRoles.remove(i);

    				i = i - 1;
    			}
    		} else if (!diffComponentRoles.contains(componentRoles.get(i))) {
    			componentRoles.remove(i);

    			i = i - 1;
    		}
    	}
    }
    
    public List<Edge> depthFirstTraversal(boolean includeStart, boolean blankOnly) {
    	Stack<Edge> edgeStack = new Stack<Edge>();

    	edgeStack.push(this);

    	List<Edge> traversalEdges = new LinkedList<Edge>();
    	Set<Edge> visitedEdges = new HashSet<Edge>();

    	while (!edgeStack.isEmpty()) {
    		Edge edge = edgeStack.pop();
    		
    		if (includeStart || edge != this || !traversalEdges.isEmpty()) {
    			traversalEdges.add(edge);
    			visitedEdges.add(edge);
    		}
    		
    		if (edge.getHead().hasEdges()) {
    			if (blankOnly) {
    				for (Edge headEdge : edge.getHead().getBlankEdges()) {
        				if (!visitedEdges.contains(headEdge)) {
        					edgeStack.push(headEdge);
        				}
        			}
    			} else {
    				for (Edge headEdge : edge.getHead().getEdges()) {
        				if (!visitedEdges.contains(headEdge)) {
        					edgeStack.push(headEdge);
        				}
        			}
    			}
    			
    		}
    	}
    	
    	return traversalEdges;
    }
    
    public List<Edge> reverseDepthFirstTraversal(HashMap<String, Set<Edge>> idToIncomingEdges, boolean blankOnly) {
    	Stack<Edge> edgeStack = new Stack<Edge>();

    	edgeStack.push(this);

    	List<Edge> traversalEdges = new LinkedList<Edge>();
    	Set<Edge> visitedEdges = new HashSet<Edge>();

    	while (!edgeStack.isEmpty()) {
    		Edge edge = edgeStack.pop();
    		
    		traversalEdges.add(edge);
        	visitedEdges.add(edge);

    		if (idToIncomingEdges.containsKey(edge.getTail().getNodeID())) {
    			if (blankOnly) {
    				for (Edge tailEdge : idToIncomingEdges.get(edge.getTail().getNodeID())) {
        				if (!visitedEdges.contains(tailEdge) && tailEdge.isBlank()) {
        					edgeStack.push(tailEdge);
        				}
        			}
    			} else {
    				for (Edge tailEdge : idToIncomingEdges.get(edge.getTail().getNodeID())) {
        				if (!visitedEdges.contains(tailEdge)) {
        					edgeStack.push(tailEdge);
        				}
        			}
    			}
    		}
    	}

    	return traversalEdges;
    }
    
    public List<Edge> getCycle(HashMap<String, Set<Edge>> nodeIDToIncomingEdges, boolean blankOnly) {
    	List<Edge> traversalEdges = this.depthFirstTraversal(false, blankOnly);
		
    	traversalEdges.retainAll(this.reverseDepthFirstTraversal(nodeIDToIncomingEdges, blankOnly));

    	return traversalEdges;
    }
    
    public List<List<Edge>> getOrthogonalBlankPaths() {
    	List<List<Edge>> blankPaths = new LinkedList<List<Edge>>();
    	blankPaths.add(new LinkedList<Edge>());
    	
    	List<Set<Node>> visitedNodes = new LinkedList<Set<Node>>();
    	visitedNodes.add(new HashSet<Node>());
    	
    	Stack<Edge> edgeStack = new Stack<Edge>();
    	edgeStack.push(this);
    	
    	int k = 0;
    	
    	while (!edgeStack.isEmpty()) {
    		int edgeCount = edgeStack.size();
    		
    		Edge tempEdge = edgeStack.pop();
    		
    		blankPaths.get(k).add(tempEdge);
    		
    		visitedNodes.get(k).add(tempEdge.getTail());
    		visitedNodes.get(k).add(tempEdge.getHead());

    		Set<Edge> blankHeadEdges = tempEdge.getHead().getBlankEdgesWithoutHeads(visitedNodes.get(k));

    		for (int i = 1; i < blankHeadEdges.size(); i++) {
    			blankPaths.add(new LinkedList<Edge>(blankPaths.get(k)));
    			
    			visitedNodes.add(new HashSet<Node>(visitedNodes.get(k)));
    		}

    		for (Edge blankHeadEdge : blankHeadEdges) {
    			edgeStack.add(blankHeadEdge);
    		}

    		if (edgeStack.size() < edgeCount) {
    			k = k + 1;
    		}
    	}
    	
    	return blankPaths;
    }

    public Node getTail() {
    	return tail; 
    }

    public Node getHead() { 
    	return head; 
    }
    
    public String getTailID() {
    	return tail.getNodeID();
    }
    
    public String getHeadID() {
    	return head.getNodeID();
    }
    
    public ArrayList<String> getComponentIDs() {
    	return componentIDs; 
    }

    public ArrayList<String> getComponentRoles() { 
    	return componentRoles; 
    }

    public boolean hasComponentIDs() {
    	return componentIDs != null && !componentIDs.isEmpty();
    }

    public boolean hasComponentRoles(Set<String> roles) {
    	if (roles.isEmpty()) {
    		return true;
    	} else if (hasComponentRoles()) {
    		Set<String> compRoles = new HashSet<String>(componentRoles);

    		compRoles.retainAll(roles);

    		return !compRoles.isEmpty();
    	} else {
    		return false;
    	}
    }

    public boolean hasComponentRoles() {
    	return componentRoles != null && !componentRoles.isEmpty();
    }
    
    public boolean hasOrientation() {
    	return isInline() || isReverseComplement();
    }
    
    public boolean hasOrientation(String orientation) {
    	return hasOrientation() && this.orientation.equals(orientation);
    }
    
    public void intersectWithEdge(Edge edge, int tolerance) {
    	ArrayList<String> otherComponentIDs = edge.getComponentIDs();
    	ArrayList<String> otherComponentRoles = edge.getComponentRoles();
    	
    	Set<String> diffComponentIDs = new HashSet<String>(otherComponentIDs);
    	Set<String> diffComponentRoles = new HashSet<String>(otherComponentRoles);

    	for (int i = 0; i < componentRoles.size(); i++) {
    		if (i < componentIDs.size()) {
    			if (diffComponentIDs.contains(componentIDs.get(i))) {
    				if (!componentRoles.get(i).equals(otherComponentRoles.get(i))) {
    					componentIDs.add(0, otherComponentIDs.get(i));
    					componentRoles.add(0, otherComponentRoles.get(i));

    					i = i + 1;
    				}
    			} else if (tolerance >= 2 && diffComponentRoles.contains(componentRoles.get(i))) {
    				componentIDs.add(0, otherComponentIDs.get(i));
    				componentRoles.add(0, otherComponentRoles.get(i));

    				i = i + 1;
    			} else {
    				componentIDs.remove(i);
    				componentRoles.remove(i);

    				i = i - 1;
    			}
    		} else if (!diffComponentRoles.contains(componentRoles.get(i))) {
    			componentRoles.remove(i);

    			i = i - 1;
    		}
    	}
    }
    
    public void unionWithEdge(Edge edge) {
    	ArrayList<String> otherComponentIDs = edge.getComponentIDs();
    	ArrayList<String> otherComponentRoles = edge.getComponentRoles();

    	Set<String> unionedIDs = new HashSet<String>(componentIDs);
    	Set<String> unionedRoles = new HashSet<String>(componentRoles);

    	for (int i = 0; i < otherComponentRoles.size(); i++) {
    		if (i < otherComponentIDs.size()) {
    			if (!unionedIDs.contains(otherComponentIDs.get(i))) {
    				componentIDs.add(0, otherComponentIDs.get(i));
    				componentRoles.add(0, otherComponentRoles.get(i));
    				
    				unionedIDs.add(otherComponentIDs.get(i));
    				unionedRoles.add(otherComponentRoles.get(i));
    			} else if (!unionedRoles.contains(otherComponentRoles.get(i))) {
    				componentIDs.add(0, otherComponentIDs.get(i));
    				componentRoles.add(0, otherComponentRoles.get(i));
    				
    				unionedRoles.add(otherComponentRoles.get(i));
    			}
    		} else if (!unionedRoles.contains(otherComponentRoles.get(i))) {
    			componentRoles.add(otherComponentRoles.get(i));
				
				unionedRoles.add(otherComponentRoles.get(i));
    		}
    	}
    }
    
    public boolean isMatching(Edge edge, int tolerance, Set<String> roles) {
    	return hasSameOrientation(edge) && (roles.isEmpty() || hasSharedComponentRoles(edge, roles))
    			&& (hasSharedComponentIDs(edge) || tolerance >= 2 && hasSharedComponentRoles(edge, roles));
    }
    
    public boolean hasSameOrientation(Edge edge) {
    	return hasOrientation(edge.getOrientation());
    }
    
    public boolean hasSharedComponentIDs(Edge edge) {
    	if (hasComponentIDs() && edge.hasComponentIDs()) {
    		Set<String> compIDs1 = new HashSet<String>(componentIDs);

    		Set<String> compIDs2 = new HashSet<String>(edge.getComponentIDs());

    		compIDs1.retainAll(compIDs2);

    		return !compIDs1.isEmpty();
    	} else {
    		return false;
    	}
   }

    public boolean hasSharedComponentRoles(Edge edge, Set<String> roles) {
    	if (hasComponentRoles() && edge.hasComponentRoles()) {
    			Set<String> compRoles1 = new HashSet<String>(componentRoles);
    			
    			Set<String> compRoles2 = new HashSet<String>(edge.getComponentRoles());

    			compRoles1.retainAll(compRoles2);
    			
    			if (!roles.isEmpty()) {
    				compRoles1.retainAll(roles);
    			}

    			return !compRoles1.isEmpty();
    	} else {
    		return false;
    	}
    }
    
    public boolean isInline() {
    	return orientation.equals(Orientation.INLINE.getValue());
    }
    
    public boolean isReverseComplement() {
    	return orientation.equals(Orientation.REVERSE_COMPLEMENT.getValue());
    }
    
    public boolean isBlank() {
    	return !hasComponentIDs() && !hasComponentRoles() && !hasOrientation();
    }
    
    public void setComponentIDs(ArrayList<String> compIDs) {
    	componentIDs = compIDs;
    }

    public void setHead(Node head) {
    	this.head = head; 
    }

    public void setTail(Node tail) { 
    	this.tail = tail; 
    }
    
    public String getOrientation() {
    	return orientation;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() { 
    	return weight; 
    }
    
    public String toString() {
    	if (hasComponentIDs()) {
    		return tail.getNodeID() + " -" + componentIDs.toString() + "-> " + head.getNodeID();
    	} else {
    		return tail.getNodeID() + " --> " + head.getNodeID();
    	}
    	
    }
    
    public enum Orientation {
        INLINE("inline"),
        REVERSE_COMPLEMENT("reverseComplement"),
        NONE("none");

        private final String value;
        
        Orientation(String value) { 
        	this.value = value;
        }

        public String getValue() {
        	return value; 
        }
    }
}
