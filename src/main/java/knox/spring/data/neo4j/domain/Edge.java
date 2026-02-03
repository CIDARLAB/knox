package knox.spring.data.neo4j.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

//import javafx.geometry.Orientation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@RelationshipProperties
public class Edge {
    @RelationshipId
    @GeneratedValue
    private Long id;

    @TargetNode 
    Node head;

    Node tail;

    @Property 
    ArrayList<String> componentIDs;
    
    @Property 
    ArrayList<String> componentRoles;

    @Property 
    Orientation orientation;

    @Property 
    ArrayList<Double> weight;
    
    private static final Logger LOG = LoggerFactory.getLogger(Edge.class);

    public Edge() {}

    public Edge(Node tail, Node head) {
        this.tail = tail;
        
        this.head = head;
        
        componentIDs = new ArrayList<String>();
        
        componentRoles = new ArrayList<String>();
        
        orientation = Orientation.NONE;
        
        weight = new ArrayList<Double>();
    }
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, 
                ArrayList<String> componentRoles) {
        this.tail = tail;
        
        this.head = head;
        
        this.componentIDs = componentIDs;
        
        this.componentRoles = componentRoles;
        
        if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
            orientation = Orientation.INLINE;
        } else {
            orientation = Orientation.NONE;
        }
        
        weight = new ArrayList<Double>();
        for (String ID : componentIDs) {
            weight.add(0.0);
        }
    }

    public Edge(Node tail, Node head, ArrayList<String> componentIDs, 
                ArrayList<String> componentRoles, ArrayList<Double> weight) {
        this.tail = tail;
        
        this.head = head;
        
        this.componentIDs = componentIDs;
        
        this.componentRoles = componentRoles;
        
        if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
            orientation = Orientation.INLINE;
        } else {
            orientation = Orientation.NONE;
        }
        
        this.weight = weight;
    }
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, 
                ArrayList<String> componentRoles, Orientation orientation) {
        this.tail = tail;
        
        this.head = head;
        
        this.componentIDs = componentIDs;
        
        this.componentRoles = componentRoles;
        
        if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
            this.orientation = orientation;
        } else {
            this.orientation = Orientation.NONE;
        }
        
        weight = new ArrayList<Double>();
        for (String ID : componentIDs) {
            weight.add(0.0);
        }
    }
    
    public Edge(Node tail, Node head, ArrayList<String> componentIDs, 
            ArrayList<String> componentRoles, Orientation orientation, ArrayList<Double> weight) {
        this.tail = tail;

        this.head = head;

        this.componentIDs = componentIDs;

        this.componentRoles = componentRoles;

        if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
            this.orientation = orientation;
        } else {
            this.orientation = Orientation.NONE;
        }

        this.weight = weight;
    }
    
    public Edge(ArrayList<String> componentIDs, ArrayList<String> componentRoles,
                Orientation orientation, ArrayList<Double> weight) {
        this.componentIDs = componentIDs;

        this.componentRoles = componentRoles;

        if (!this.componentIDs.isEmpty() || !this.componentRoles.isEmpty()) {
            this.orientation = orientation;
        } else {
            this.orientation = Orientation.NONE;
        }

        this.weight = weight;
    }
    
    public Edge copy() {
        return new Edge(new ArrayList<String>(componentIDs),
                new ArrayList<>(componentRoles), orientation, weight);
    }
    
    public Edge copy(Node head) {
        return new Edge(tail, head, new ArrayList<String>(componentIDs),
                new ArrayList<>(componentRoles), orientation, weight);
    }

    public Edge copy(Node tail, Node head) {
        return new Edge(tail, head, new ArrayList<String>(componentIDs),
                new ArrayList<>(componentRoles), orientation, weight);
    }

    public Edge copy(Node tail, Node head, ArrayList<Double> weight) {
        return new Edge(tail, head, new ArrayList<String>(componentIDs),
                new ArrayList<>(componentRoles), orientation, weight);
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
    
    public ArrayList<String> getAbstractComponentRoles() { 
    	ArrayList<String> abstractComponentRoles = new ArrayList<String>();
    	
    	for (int i = componentIDs.size(); i < componentRoles.size(); i++) {
    		abstractComponentRoles.add(componentRoles.get(i));
    	}
    	
        return abstractComponentRoles; 
    }
    
    public ArrayList<String> getConcreteComponentRoles() { 
    	ArrayList<String> concreteComponentRoles = new ArrayList<String>();
    	
    	for (int i = 0; i < componentIDs.size(); i++) {
    		concreteComponentRoles.add(componentRoles.get(i));
    	}
    	
        return concreteComponentRoles; 
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
    
    public void intersectWithEdge(Edge edge, Edge thisEdge,
            HashMap<String, Set<Edge>> nodeIDToIncomingEdgesRowSpace, HashMap<String, Set<Edge>> nodeIDToIncomingEdgesColSpace,
            int tolerance, int weightTolerance, Boolean isStrongProduct, ArrayList<String> irrelevantParts) {
        
        // Map componentIDs to Weight

        HashMap<String, Double> componentIDstoWeight = componentIDtoWeight();
        HashMap<String, Double> otherComponentIDstoWeight = new HashMap<String, Double>(edge.componentIDtoWeight());

        // Map other component IDs to roles and other component roles to IDs
        
        ArrayList<String> otherComponentIDs = edge.getComponentIDs();
        ArrayList<String> otherComponentRoles = edge.getComponentRoles();
        
        HashMap<String, Set<String>> otherComponentIDToRoles = new HashMap<String, Set<String>>();
        HashMap<String, Set<String>> otherComponentRoleToIDs = new HashMap<String, Set<String>>();
        
        for (int i = 0; i < otherComponentIDs.size(); i++) {
        	if (!otherComponentIDToRoles.containsKey(otherComponentIDs.get(i))) {
        		otherComponentIDToRoles.put(otherComponentIDs.get(i), new HashSet<String>());
        	}

        	otherComponentIDToRoles.get(otherComponentIDs.get(i)).add(otherComponentRoles.get(i));
        		
        	if (!otherComponentRoleToIDs.containsKey(otherComponentRoles.get(i))) {
        		otherComponentRoleToIDs.put(otherComponentRoles.get(i), new HashSet<String>());
        	}

        	otherComponentRoleToIDs.get(otherComponentRoles.get(i)).add(otherComponentIDs.get(i));
        }
        
        ArrayList<String> abstractRoles = getAbstractComponentRoles();
        Set<String> otherAbstractRoles = new HashSet<String>(edge.getAbstractComponentRoles());
        
        // Remove component IDs and roles based on intersection
        
        for (int i = 0; i < componentRoles.size(); i++) {
            if (i < componentIDs.size()) {
                if (!otherComponentIDToRoles.containsKey(componentIDs.get(i)) 
                        && (tolerance < 1 || !otherAbstractRoles.contains(componentRoles.get(i)))) {
                    componentIDs.remove(i);
                    componentRoles.remove(i);

                    i = i - 1;
                }
            } else if (tolerance < 1 || otherComponentRoleToIDs.containsKey(componentRoles.get(i))
            			|| tolerance < 2 || !otherAbstractRoles.contains(componentRoles.get(i))) {
                componentRoles.remove(i);

                i = i - 1;
            }
        }
        
        // Add missing component roles associated with intersecting component IDs
        
        HashMap<String, Set<String>> componentIDToRoles = new HashMap<String, Set<String>>();
        
        for (int i = 0; i < componentIDs.size(); i++) {
        	if (!componentIDToRoles.containsKey(componentIDs.get(i))) {
        		componentIDToRoles.put(componentIDs.get(i), new HashSet<String>());
        	}

        	componentIDToRoles.get(componentIDs.get(i)).add(componentRoles.get(i));
        }
        
        for (String componentID : componentIDToRoles.keySet()) {
        	if (otherComponentIDToRoles.containsKey(componentID)) {
        		for (String otherComponentRole : otherComponentIDToRoles.get(componentID)) {
        			Set<String> concreteRoles = componentIDToRoles.get(componentID);
        			
        			if (!concreteRoles.contains(otherComponentRole)) {
        				componentIDs.add(0, componentID);
        				componentRoles.add(0, otherComponentRole);
        			}
        		}
        	}
        }
        
        // Add missing component IDs associated with intersecting component roles
        
        if (tolerance > 0) {
        	for (String abstractRole : abstractRoles) {
        		if (otherComponentRoleToIDs.containsKey(abstractRole)) { 
        			for (String otherComponentID : otherComponentRoleToIDs.get(abstractRole)) {
        				if (!componentIDToRoles.containsKey(otherComponentID)) {
        					componentIDs.add(0, otherComponentID);
        					componentRoles.add(0, abstractRole);
        				}
        			}
        		}
        	}
        }

        // Update Edge Weights
        ArrayList<Double> newWeights = new ArrayList<Double>();

        Boolean weightToleranceResult = false;
        if ((weightTolerance == 1) && isStrongProduct) {
            // Check if edges in same position
            if ((thisEdge.distanceToAcceptNode() == edge.distanceToAcceptNode()) || (thisEdge.distanceToStartNode(nodeIDToIncomingEdgesColSpace) == edge.distanceToStartNode(nodeIDToIncomingEdgesRowSpace))) {
                weightToleranceResult = true;
            } else {
                weightToleranceResult = false;
            }

        } else if ((weightTolerance == 2) && isStrongProduct) {
            // Check if edges next to same component
            if (thisEdge.sameNextParts(edge, nodeIDToIncomingEdgesRowSpace, nodeIDToIncomingEdgesColSpace, false)) {
                weightToleranceResult = true;
            } else {
                weightToleranceResult = false;
            }
        }

        for (String ID : componentIDs) {
            if (componentIDstoWeight.containsKey(ID) && otherComponentIDstoWeight.containsKey(ID)) {

                if (isStrongProduct){

                    if (weightTolerance > 0) {
                        if ((weightToleranceResult || weightTolerance > 2) && !irrelevantParts.contains(ID)) {
                            newWeights.add(componentIDstoWeight.get(ID) + otherComponentIDstoWeight.get(ID)); // sum weights
                        } else {
                            newWeights.add((componentIDstoWeight.get(ID) + otherComponentIDstoWeight.get(ID)) / 2); // average weights
                        }
                        
                    } else {
                        newWeights.add(componentIDstoWeight.get(ID) + otherComponentIDstoWeight.get(ID)); // sum weights
                    }

                } else {
                    newWeights.add(componentIDstoWeight.get(ID) + otherComponentIDstoWeight.get(ID)); // sum weights (AND Operator)
                }

            } else if (componentIDstoWeight.containsKey(ID)) {
                newWeights.add(componentIDstoWeight.get(ID));

            } else if (otherComponentIDstoWeight.containsKey(ID)) {
                newWeights.add(otherComponentIDstoWeight.get(ID));
            }
        }

        this.weight = newWeights;
    }
    
    public void unionWithEdge(Edge edge) {
        HashMap<String, Double> componentIDstoWeight = componentIDtoWeight();
        HashMap<String, Double> otherComponentIDstoWeight = new HashMap<String, Double>(edge.componentIDtoWeight());

        ArrayList<String> otherComponentIDs = edge.getComponentIDs();
        ArrayList<String> otherComponentRoles = edge.getComponentRoles();

        Set<String> unionedIDs = new HashSet<String>(componentIDs);
        Set<String> unionedRoles = new HashSet<String>(componentRoles);

        // Update IDs and Roles

        for (int i = 0; i < otherComponentRoles.size(); i++) {
            if (i < otherComponentIDs.size()) {
                if (!unionedIDs.contains(otherComponentIDs.get(i))) {
                    componentIDs.add(0, otherComponentIDs.get(i));
                    componentRoles.add(0, otherComponentRoles.get(i));
                    
                    unionedIDs.add(otherComponentIDs.get(i));
                    unionedRoles.add(otherComponentRoles.get(i));
                    
                    i = i + 1;
                } else if (!unionedRoles.contains(otherComponentRoles.get(i))) {
                    componentIDs.add(0, otherComponentIDs.get(i));
                    componentRoles.add(0, otherComponentRoles.get(i));
                    
                    unionedRoles.add(otherComponentRoles.get(i));
                    
                    i = i + 1;
                }
            } else if (!unionedRoles.contains(otherComponentRoles.get(i))) {
                componentRoles.add(otherComponentRoles.get(i));
                
                unionedRoles.add(otherComponentRoles.get(i));
                
                i = i + 1;
            }
        }

        // Update Edge Weights

        ArrayList<Double> newWeights = new ArrayList<Double>();

        for (String ID : componentIDs) {
            if (componentIDstoWeight.containsKey(ID) && otherComponentIDstoWeight.containsKey(ID)) {
                newWeights.add((componentIDstoWeight.get(ID) + otherComponentIDstoWeight.get(ID)) / 2);  // average weights if they are different

            } else if (componentIDstoWeight.containsKey(ID)) {
                newWeights.add(componentIDstoWeight.get(ID));

            } else if (otherComponentIDstoWeight.containsKey(ID)) {
                newWeights.add(otherComponentIDstoWeight.get(ID));
            }
        }

        this.weight = newWeights;
    }
    
    public boolean isMatching(Edge edge, int tolerance, Set<String> roles) {
        return hasSameOrientation(edge)
                && (hasSharedComponentIDs(edge) || tolerance >= 1 && hasSharedComponentRoles(edge, roles, tolerance));
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

    public boolean hasSharedComponentRoles(Edge edge, Set<String> roles, int tolerance) {
        if (hasComponentRoles() && edge.hasComponentRoles() && (tolerance == 1 || tolerance == 2)) {
        	Set<String> concreteRoles1 = new HashSet<String>(getConcreteComponentRoles());
        	Set<String> concreteRoles2 = new HashSet<String>(edge.getConcreteComponentRoles());

        	Set<String> abstractRoles1 = new HashSet<String>(getAbstractComponentRoles());
        	Set<String> abstractRoles2 = new HashSet<String>(edge.getAbstractComponentRoles());

        	concreteRoles1.retainAll(abstractRoles2);

        	if (!roles.isEmpty()) {
        		concreteRoles1.retainAll(roles);
        	}

        	concreteRoles2.retainAll(abstractRoles1);

        	if (!roles.isEmpty()) {
        		concreteRoles2.retainAll(roles);
        	}
        	
        	if (!concreteRoles1.isEmpty() || !concreteRoles2.isEmpty()) {
        		return true;
        	} else if (tolerance == 2) {
        		abstractRoles1.retainAll(abstractRoles2);

        		if (!roles.isEmpty()) {
        			abstractRoles1.retainAll(roles);
        		}

        		return !abstractRoles1.isEmpty();
        	} else {
                return false;
            }
        } else {
            return false;
        }
    }

    public HashMap<String, Double> componentIDtoWeight() {
        HashMap<String, Double> componentIDstoWeight = new HashMap<String, Double>();

        for (int i = 0; i < componentIDs.size(); i++) {
            componentIDstoWeight.put(componentIDs.get(i), weight.get(i));
        }

        return componentIDstoWeight;
    }

    public HashMap<String, Double> componentIDtoWeight(ArrayList<String> irrelevantParts) {
        HashMap<String, Double> componentIDstoWeight = new HashMap<String, Double>();

        for (int i = 0; i < componentIDs.size(); i++) {
            if (irrelevantParts.contains(componentIDs.get(i))) {
                componentIDstoWeight.put(componentIDs.get(i), 0.0);
            } else {
                componentIDstoWeight.put(componentIDs.get(i), weight.get(i));
            }
        }

        return componentIDstoWeight;
    }

    public HashMap<String, String> componentIDtoRole() {
        HashMap<String, String> componentIDstoRoles = new HashMap<String, String>();

        for (int i = 0; i < componentIDs.size(); i++) {
            componentIDstoRoles.put(componentIDs.get(i), componentRoles.get(i));
        }

        return componentIDstoRoles;
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

    public void setWeight(ArrayList<Double> weight) {
        this.weight = weight;
    }

    public void emptyWeight() {
        this.weight = new ArrayList<Double>();
    }

    public Double getMaxWeight() {
        return Collections.max(weight);
    }

    public ArrayList<Double> getWeight() { 
        return weight; 
    }
    
    public String toString() {
        if (hasComponentIDs()) {
            return tail.getNodeID() + " -" + componentIDs.toString() + "-> " + head.getNodeID();
        } else {
            return tail.getNodeID() + " --> " + head.getNodeID();
        }
        
    }

    public Integer distanceToAcceptNode() {
        Integer position = 0;

        Node currentNode = head;

        // dfs
        if (!currentNode.isAcceptNode()) {
            position = toAcceptNode(currentNode, position);
        }

        return position;
    }

    private Integer toAcceptNode(Node currentNode, Integer position) {

        for (Edge e : currentNode.getEdges()) {
            if (!e.isBlank()) {
                position++;
            }

            currentNode = e.getHead();

            if (!currentNode.isAcceptNode()) {
                position = toAcceptNode(currentNode, position);
            } else {
                return position;
            }
        }

        return position;
    }

    public Integer distanceToStartNode(HashMap<String, Set<Edge>> nodeIDToIncomingEdges) {
        Integer position = 0;

        Node currentNode = tail;

        // dfs
        if (!currentNode.isStartNode()) {
            position = toStartNode(nodeIDToIncomingEdges, currentNode, position);
        }

        return position;
    }

    private Integer toStartNode(HashMap<String, Set<Edge>> nodeIDToIncomingEdges, Node currentNode, Integer position) {

        for (Edge e : currentNode.getIncomingEdges(nodeIDToIncomingEdges)) {
            if (!e.isBlank()) {
                position++;
            }

            currentNode = e.getTail();

            if (!currentNode.isStartNode()) {
                position = toStartNode(nodeIDToIncomingEdges, currentNode, position);
            } else {
                return position;
            }
        }

        return position;
    }

    public Boolean sameNextParts(Edge edge, HashMap<String, Set<Edge>> nodeIDToIncomingEdgesRowSpace,
            HashMap<String, Set<Edge>> nodeIDToIncomingEdgesColSpace, boolean needBoth) {
        
        Boolean same1 = false;
        Boolean same2 = false;
        Boolean goodResult = true;
        Boolean badResult = false;
        Set<String> compIDsHead1 = new HashSet<>();
        Set<String> compIDsHead2 = new HashSet<>();
        Set<String> compIDsTail1 = new HashSet<>();
        Set<String> compIDsTail2 = new HashSet<>();

        // Check if edges have same up stream part
        compIDsHead1 = getNextPartsHead(compIDsHead1);
        compIDsHead2 = edge.getNextPartsHead(compIDsHead2);

        for (String compID : compIDsHead2) {
            if (compIDsHead1.contains(compID)) {
                same1 = true;

                if (!needBoth) {
                    return same1;
                }
            }
        }
        
        // Check if edges have same down stream part
        compIDsTail1 = getNextPartsTail(compIDsTail1, nodeIDToIncomingEdgesColSpace);
        compIDsTail2 = edge.getNextPartsTail(compIDsTail2, nodeIDToIncomingEdgesRowSpace);

        for (String compID : compIDsTail2) {
            if (compIDsTail1.contains(compID)) {
                same2 = true;

                if (!needBoth) {
                    return same2;
                }
            }
        }

        if (needBoth) {
            // return true if they have same up AND down stream part
            if (same1 && same2) {
                return goodResult;
            } else {
                return badResult;
            }
        } else {
            // return true if they have same up OR down stream part
            if (same1 || same2) {
                return goodResult;
            } else {
                return badResult;
            }
        }

    }

    public Boolean sameNextPartsOR(Edge edge, HashMap<String, Set<Edge>> nodeIDToIncomingEdgesRowSpace,
            HashMap<String, Set<Edge>> nodeIDToIncomingEdgesColSpace) {
        
        Boolean same = false;
        Set<String> compIDsHead1 = new HashSet<>();
        Set<String> compIDsHead2 = new HashSet<>();
        Set<String> compIDsTail1 = new HashSet<>();
        Set<String> compIDsTail2 = new HashSet<>();
        
        // Check if first edges have same up stream part
        if (this.tail.isStartNode() && edge.getTail().isStartNode()) {

            compIDsHead1 = getNextPartsHead(compIDsHead1);
            compIDsHead2 = edge.getNextPartsHead(compIDsHead2);
            //System.out.println(compIDsHead1);
            //System.out.println(compIDsHead2);

            for (String compID : compIDsHead2) {
                if (compIDsHead1.contains(compID)) {
                    same = true;
                }
            }
            //System.out.println(same);
            //System.out.println("\n");
            return same;

        }

        // Check if last edges have same down stream part
        if (this.head.isAcceptNode() && edge.getHead().isAcceptNode()) {

            compIDsTail1 = getNextPartsTail(compIDsTail1, nodeIDToIncomingEdgesColSpace);
            compIDsTail2 = edge.getNextPartsTail(compIDsTail2, nodeIDToIncomingEdgesRowSpace);
            //System.out.println(compIDsTail1);
            //System.out.println(compIDsTail2);

            for (String compID : compIDsTail2) {
                if (compIDsTail1.contains(compID)) {
                    same = true;
                }
            }
            //System.out.println(same);
            //System.out.println("\n");
            return same;

        }

        // Check if edges have same up stream part
        compIDsHead1 = getNextPartsHead(compIDsHead1);
        compIDsHead2 = edge.getNextPartsHead(compIDsHead2);
        //System.out.println(compIDsHead1);
        //System.out.println(compIDsHead2);

        for (String compID : compIDsHead2) {
            if (compIDsHead1.contains(compID)) {
                same = true;
                //System.out.println(same);
                //System.out.println("\n");
                return same;
            }
        }
        
        // Check if edges have same down stream part
        compIDsTail1 = getNextPartsTail(compIDsTail1, nodeIDToIncomingEdgesColSpace);
        compIDsTail2 = edge.getNextPartsTail(compIDsTail2, nodeIDToIncomingEdgesRowSpace);
        //System.out.println(compIDsTail1);
        //System.out.println(compIDsTail2);

        for (String compID : compIDsTail2) {
            if (compIDsTail1.contains(compID)) {
                same = true;
                //System.out.println(same);
                //System.out.println("\n");
                return same;
            }
        }

        //System.out.println(same);
        //System.out.println("\n");
        return same;

    }

    /*public Boolean sameNextParts(Edge edge, HashMap<String, Set<Edge>> nodeIDToIncomingEdgesRowSpace,
            HashMap<String, Set<Edge>> nodeIDToIncomingEdgesColSpace, boolean needBoth) {
        Boolean same = false;
        Boolean same1 = false;
        Boolean same2 = false;
        Set<String> compIDsHead1 = new HashSet<>();
        Set<String> compIDsHead2 = new HashSet<>();
        Set<String> compIDsTail1 = new HashSet<>();
        Set<String> compIDsTail2 = new HashSet<>();

        // Check if first edges have same up stream part
        if (this.tail.isStartNode() && edge.getTail().isStartNode()) {

            compIDsHead1 = getNextPartsHead(compIDsHead1);
            compIDsHead2 = edge.getNextPartsHead(compIDsHead2);

            for (String compID : compIDsHead2) {
                if (compIDsHead1.contains(compID)) {
                    same = true;
                    return same;
                }
            }

        } else if (this.head.isAcceptNode() && edge.getHead().isAcceptNode()) {
            // Check if last edges have same down stream part

            compIDsTail1 = getNextPartsTail(compIDsTail1, nodeIDToIncomingEdgesColSpace);
            compIDsTail2 = edge.getNextPartsTail(compIDsTail2, nodeIDToIncomingEdgesRowSpace);

            for (String compID : compIDsTail2) {
                if (compIDsTail1.contains(compID)) {
                    same = true;
                    return same;
                }
            }

        } else {
            // Check if edges have same down stream part
            compIDsTail1 = getNextPartsTail(compIDsTail1, nodeIDToIncomingEdgesColSpace);
            compIDsTail2 = edge.getNextPartsTail(compIDsTail2, nodeIDToIncomingEdgesRowSpace);

            for (String compID : compIDsTail2) {
                if (compIDsTail1.contains(compID)) {
                    same1 = true;
                }
            }

            // Check if edges have same up stream part
            compIDsHead1 = getNextPartsHead(compIDsHead1);
            compIDsHead2 = edge.getNextPartsHead(compIDsHead2);

            for (String compID : compIDsHead2) {
                if (compIDsHead1.contains(compID)) {
                    same2 = true;
                }
            }

            if (needBoth) {
                // return true if they have same up AND down stream part
                if (same1 && same2) {
                    return same1;
                } else {
                    return false;
                }
            } else {
                // return true if they have same up OR down stream part
                if (same1 || same2) {
                    return true;
                } else {
                    return false;
                }
            }

        }
        
        return same;
    } */

    public Boolean sameNextPartsTail(Edge edge, HashMap<String, Set<Edge>> nodeIDToIncomingEdgesRowSpace,
            HashMap<String, Set<Edge>> nodeIDToIncomingEdgesColSpace) {
        Boolean same = false;
        Set<String> compIDsHead1 = new HashSet<>();
        Set<String> compIDsHead2 = new HashSet<>();
        Set<String> compIDsTail1 = new HashSet<>();
        Set<String> compIDsTail2 = new HashSet<>();
        
        // Check if edges have same down stream part
        compIDsTail1 = getNextPartsTail(compIDsTail1, nodeIDToIncomingEdgesColSpace);
        compIDsTail2 = edge.getNextPartsTail(compIDsTail2, nodeIDToIncomingEdgesRowSpace);

        for (String compID : compIDsTail2) {
            if (compIDsTail1.contains(compID)) {
                same = true;
                return same;
            }
        }

        // Check if first edges have same up stream part
        if (this.tail.isStartNode() && edge.getTail().isStartNode()) {

            compIDsHead1 = getNextPartsHead(compIDsHead1);
            compIDsHead2 = edge.getNextPartsHead(compIDsHead2);

            for (String compID : compIDsHead2) {
                if (compIDsHead1.contains(compID)) {
                    same = true;
                    return same;
                }
            }

        }
        
        return same;
    }

    public Boolean sameNextPartsHead(Edge edge, HashMap<String, Set<Edge>> nodeIDToIncomingEdgesRowSpace,
            HashMap<String, Set<Edge>> nodeIDToIncomingEdgesColSpace) {
        Boolean same = false;
        Set<String> compIDsHead1 = new HashSet<>();
        Set<String> compIDsHead2 = new HashSet<>();
        Set<String> compIDsTail1 = new HashSet<>();
        Set<String> compIDsTail2 = new HashSet<>();
        
        // Check if edges have same down stream part
        compIDsHead1 = getNextPartsHead(compIDsHead1);
        compIDsHead2 = edge.getNextPartsHead(compIDsHead2);

        for (String compID : compIDsHead2) {
            if (compIDsHead1.contains(compID)) {
                same = true;
                return same;
            }
        }

        // Check if last edges have same down stream part
        if (this.head.isAcceptNode() && edge.getHead().isAcceptNode()) {

            compIDsTail1 = getNextPartsTail(compIDsTail1, nodeIDToIncomingEdgesColSpace);
            compIDsTail2 = edge.getNextPartsTail(compIDsTail2, nodeIDToIncomingEdgesRowSpace);

            for (String compID : compIDsTail2) {
                if (compIDsTail1.contains(compID)) {
                    same = true;
                    return same;
                }
            }

        }
        
        return same;
    }

    private Set<String> getNextPartsHead(Set<String> compIDs){
        for (Edge e : head.getEdges()) {
            
            if (e.isBlank()) {
                compIDs = getNextPartsHead(compIDs);

            } else {
                compIDs.addAll(e.getComponentIDs());

            }

        }

        return compIDs;
    }

    private Set<String> getNextPartsTail(Set<String> compIDs, HashMap<String, Set<Edge>> nodeIDToIncomingEdges){
        for (Edge e : tail.getIncomingEdges(nodeIDToIncomingEdges)) {
            
            if (e.isBlank()) {
                compIDs = getNextPartsTail(compIDs, nodeIDToIncomingEdges);

            } else {
                compIDs.addAll(e.getComponentIDs());

            }

        }

        return compIDs;
    }

    public enum Orientation {
        INLINE("inline"),
        REVERSE_COMPLEMENT("reverseComplement"),
        UNDECLARED("undeclared"), //means it can be inline or reverse
        NONE("none");

        private final String value;

        Orientation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation){
        this.orientation = orientation;
    }

    public boolean hasOrientation() {
        return isInline() || isReverseComplement();
    }


    public boolean hasOrientation(Orientation orientation) {
        return hasOrientation() && this.orientation.equals(orientation);
    }

    public boolean isInline() {
        return orientation.equals(Orientation.INLINE);
    }

    public boolean isReverseComplement() {
        return orientation.equals(Orientation.REVERSE_COMPLEMENT);
    }

    public void reverseOrientation(){
        if(isInline()){
            setOrientation(Orientation.REVERSE_COMPLEMENT);
        }

        else if(isReverseComplement()){
            setOrientation(Orientation.INLINE);
        }
    }
}
