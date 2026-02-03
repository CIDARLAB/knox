package knox.spring.data.neo4j.goldbar;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

import knox.spring.data.neo4j.operations.JoinOperator;
import knox.spring.data.neo4j.operations.OROperator;
import knox.spring.data.neo4j.operations.ANDOperator;
import knox.spring.data.neo4j.operations.MergeOperator;
import knox.spring.data.neo4j.operations.ReverseOperator;

import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class GoldbarConversion {

    JSONObject goldbar;

    JSONObject categories;

    Double weight;

    Boolean verbose;

    NodeSpace outputSpace;

    ArrayList<Node> boundaryStack;
    
    public GoldbarConversion(JSONObject goldbar, JSONObject categories, Double weight, Boolean verbose) {

        this.goldbar = goldbar;

        this.categories = categories;

        this.weight = weight;

        this.verbose = verbose;

        this.outputSpace = new NodeSpace();

        this.boundaryStack = new ArrayList<>();

    }

    public void convert() throws JSONException {
        Map<String, JSONArray> operationToVariableList = new HashMap<>();

        // Map operation to variableList
        operationToVariableList = jsonObjectToMap(goldbar);

        // Add space to spaces
        for (String operation : operationToVariableList.keySet()) {   // Should only be one operation
            outputSpace = handleOp(operation, operationToVariableList.get(operation));
        }

        // Add new accept nodes if current accept nodes have outgoing edges
        Set<Node> acceptNodes = outputSpace.getAcceptNodes();
        for (Node acceptNode : acceptNodes) {
            if (acceptNode.hasEdges()) {
                Node newAcceptNode = outputSpace.createAcceptNode();
                Edge blankEdge = new Edge(acceptNode, newAcceptNode);
                acceptNode.addEdge(blankEdge);
                acceptNode.deleteAcceptNodeType();
            }
        }

        // add new start nodes if current start nodes have incoming edges
        Set<Node> startNodes = outputSpace.getStartNodes();
        HashMap<String, Set<Edge>> nodeIDToIncomingEdges = outputSpace.mapNodeIDsToIncomingEdges();
        for (Node startNode : startNodes) {
            if (startNode.hasIncomingEdges(nodeIDToIncomingEdges)) {
                Node newStartNode = outputSpace.createStartNode();
                Edge blankEdge = new Edge(newStartNode, startNode);
                newStartNode.addEdge(blankEdge);
                startNode.deleteStartNodeType();
            }
        }
    }

    private NodeSpace handleOp(String operation, JSONArray variableList) {
        NodeSpace space = new NodeSpace();

        if (operation.equals("Atom")) {
            try {
                debugPrint("\nAtom: " + variableList.getString(0));
                space = handleAtomAndReverseComp(variableList.getString(0), Edge.Orientation.INLINE);
                debugPrint("Atom Space Created");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        
        } else if (operation.equals("ReverseComp")) {
            try {
                JSONArray compIDs = (JSONArray) variableList.get(0);
                debugPrint("\nReverseComp: " + compIDs.getString(0));
                space = handleAtomAndReverseComp(compIDs.getString(0), Edge.Orientation.REVERSE_COMPLEMENT);
                debugPrint("Reverse Comp Space Created");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        
        } else if (operation.equals("Then")) {
            space = handleThen(variableList);
            debugPrint("\nThen Space Created");

        } else if (operation.equals("Or")) {
            space = handleOr(variableList);
            debugPrint("\nOr Space Created");

        } else if (operation.equals("ZeroOrMore")) {
            space = handleZeroOrMore(variableList);
            debugPrint("\nZeroOrMore Space Created");

        } else if (operation.equals("OneOrMore")) {
            space = handleOneOrMore(variableList);
            debugPrint("\nOneOrMore Space Created");

        } else if (operation.equals("ZeroOrOne")) {
            space = handleZeroOrOne(variableList);
            debugPrint("\nZeroOrOne Space Created");

        } else if (operation.equals("And0")) {
            space = handleAnd(variableList, 0);
            debugPrint("\nAnd0 Space Created");

        } else if (operation.equals("And1")) {
            space = handleAnd(variableList, 1);
            debugPrint("\nAnd1 Space Created");

        } else if (operation.equals("And2")) {
            space = handleAnd(variableList, 2);
            debugPrint("\nAnd2 Space Created");

        } else if (operation.equals("Merge")) {
            space = handleMerge(variableList);
            debugPrint("\nMerge Space Created");
        
        } else if (operation.equals("ForwardOrReverse")) {
            space = handleForwardOrReverse(variableList);
            debugPrint("\nForwardOrReverse Space Created");

        } else {
            debugPrint("\nError: (operation not valid) " + operation);
        }

        return space;
    }

    private NodeSpace handleAtomAndReverseComp(String variable, Edge.Orientation orientation) {
        ArrayList<String> componentIDs = new ArrayList<>();
        ArrayList<String> componentRoles = new ArrayList<>();
        ArrayList<Double> weights = new ArrayList<>();
        Map<String, JSONArray> rolesToCompIds = new HashMap<>();

        // Map Roles to CompIds
        try {
            rolesToCompIds = jsonObjectToMap(categories.getJSONObject(variable));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Map<String, Boolean> roleIsAbstract = new HashMap<>();

        // Fill in CompIDs and Roles
        for(String role : rolesToCompIds.keySet()) {
            roleIsAbstract.put(role, true);
            try {
                JSONArray compIDs = rolesToCompIds.get(role);
                for(int i = 0; i < compIDs.length(); i++) {
                    componentIDs.add(compIDs.getString(i));
                    componentRoles.add(convertRole(role));
                    weights.add(weight);
                    roleIsAbstract.put(role, false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (String role : roleIsAbstract.keySet()) {
            if (roleIsAbstract.get(role)) {
                componentRoles.add(convertRole(role));
            }
        }

        return new NodeSpace(componentIDs, componentRoles, orientation, weights);
    }

    private NodeSpace handleThen(JSONArray variableList) {
        NodeSpace outSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        JoinOperator.apply(spaces, outSpace);
        
        return outSpace;
    }

    private NodeSpace handleOr(JSONArray variableList) {
        NodeSpace outSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        OROperator.apply(spaces, outSpace);

        return outSpace;
    }

    private NodeSpace handleZeroOrMore(JSONArray variableList) {
        NodeSpace tempOutSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        OROperator.apply(spaces, tempOutSpace);

        Set<Node> startNodes = tempOutSpace.getStartNodes();
        Set<Node> stopNodes = tempOutSpace.getAcceptNodes();

        // Add Blank Edge from Stop Node to Start Node (or More option)
        for (Node startNode : startNodes) {
            for (Node stopNode : stopNodes) {
                Edge repeatEdge = new Edge(stopNode, startNode);
                stopNode.addEdge(repeatEdge);
            }
        }

        // Add Blank Edge from Start Node to Stop Node (Zero option)
        NodeSpace tempSpace = new NodeSpace();
        Node startNode = tempSpace.createStartNode();
        Node acceptNode = tempSpace.createAcceptNode();
        Edge blankEdge = new Edge(startNode, acceptNode);
        startNode.addEdge(blankEdge);

        NodeSpace outSpace = new NodeSpace();
        ArrayList<NodeSpace> tempSpaces = new ArrayList<>();
        tempSpaces.add(tempSpace);
        tempSpaces.add(tempOutSpace);
        OROperator.apply(tempSpaces, outSpace);

        return outSpace;
    }

    private NodeSpace handleOneOrMore(JSONArray variableList) {
        NodeSpace outSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        OROperator.apply(spaces, outSpace);

        Set<Node> startNodes = outSpace.getStartNodes();
        Set<Node> stopNodes = outSpace.getAcceptNodes();

        // Add Blank Edge from Stop Node to Start Node (or More option)
        for (Node startNode : startNodes) {
            for (Node stopNode : stopNodes) {
                Edge repeatEdge = new Edge(stopNode, startNode);
                stopNode.addEdge(repeatEdge);
            }
        }

        return outSpace;
    }

    private NodeSpace handleZeroOrOne(JSONArray variableList) {
        NodeSpace outSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        OROperator.apply(spaces, outSpace);

        Set<Node> startNodes = outSpace.getStartNodes();
        Set<Node> acceptNodes = outSpace.getAcceptNodes();

        // Add Blank Edge from Start Node to Stop Node (Zero option)

        if (outSpace.getNodes().size() > 2) {
            NodeSpace tempSpace = new NodeSpace();
            Node startNode = tempSpace.createStartNode();
            Node acceptNode = tempSpace.createAcceptNode();
            Edge blankEdge = new Edge(startNode, acceptNode);
            startNode.addEdge(blankEdge);

            NodeSpace newOutSpace = new NodeSpace();
            ArrayList<NodeSpace> tempSpaces = new ArrayList<>();
            tempSpaces.add(tempSpace);
            tempSpaces.add(outSpace);
            OROperator.apply(tempSpaces, newOutSpace);

            return newOutSpace;

        } else {
            for (Node startNode : startNodes) {
                for (Node acceptNode : acceptNodes) {
                    Node node1 = outSpace.createNode();
                    Node node2 = outSpace.createNode();
                    Edge edge1 = new Edge(startNode, node1);
                    Edge edge2 = new Edge(node1, node2);
                    Edge edge3 = new Edge(node2, acceptNode);
                    startNode.addEdge(edge1);
                    node1.addEdge(edge2);
                    node2.addEdge(edge3);
                }
            }
        }

        return outSpace;
    }

    private NodeSpace handleAnd(JSONArray variableList, int tolerance) {
        NodeSpace outSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        Set<String> roles = new HashSet<String>();
        ANDOperator.apply(spaces, outSpace, tolerance, true, roles, new ArrayList<String>());

        return outSpace;
    }

    private NodeSpace handleMerge(JSONArray variableList) {
        NodeSpace outSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        Set<String> roles = new HashSet<String>();
        MergeOperator.apply(spaces, outSpace, 0, 0, roles, new ArrayList<String>());

        return outSpace;
    }

    private NodeSpace handleForwardOrReverse(JSONArray variableList) {
        NodeSpace outSpace = new NodeSpace();
        NodeSpace tempOutSpace = new NodeSpace();
        ArrayList<NodeSpace> spaces = getSpacesForOperation(variableList);
        ReverseOperator.apply(spaces.get(0), tempOutSpace, true);
        spaces.add(tempOutSpace);
        OROperator.apply(spaces, outSpace);

        return outSpace;
    }

    private ArrayList<NodeSpace> getSpacesForOperation(JSONArray variableList) {
        ArrayList<NodeSpace> spaces = new ArrayList<>();
        
        for (int i = 0; i < variableList.length(); i++) {
            Map<String, JSONArray> operationToVariableList = new HashMap<>();

            try {
                // Map operation to variableList
                operationToVariableList = jsonObjectToMap(variableList.getJSONObject(i));

                for (String operation : operationToVariableList.keySet()) {
                    // Add space to spaces
                    spaces.add(handleOp(operation, operationToVariableList.get(operation)));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return spaces;
    }

    private Map<String, JSONArray> jsonObjectToMap(JSONObject object) {
        Map<String, JSONArray> map = new HashMap<>();

        // Map operation to variableList
        Map<String, Object> rawMap = object.toMap();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            // If the value is a JSONArray, cast directly; if it's a List, convert to JSONArray
            if (entry.getValue() instanceof JSONArray) {
                map.put(entry.getKey(), (JSONArray) entry.getValue());
            } else if (entry.getValue() instanceof java.util.List) {
                map.put(entry.getKey(), new JSONArray((java.util.List<?>) entry.getValue()));
            } 
        }

        return map;
    }

    private String convertRole(String csvRole) {
		switch (csvRole) {
		case "cds":
			return "http://identifiers.org/so/SO:0000316";
		case "promoter":
			return "http://identifiers.org/so/SO:0000167";
		case "ribosomeBindingSite":
			return "http://identifiers.org/so/SO:0000139";
		case "terminator":
			return "http://identifiers.org/so/SO:0000141";
		case "assemblyScar":
			return "http://identifiers.org/so/SO:0001953";
		case "spacer":
			return "http://identifiers.org/so/SO:0002223";
		default:
			return "http://knox.org/role/" + csvRole;
		}
	}

    private void debugPrint(Object x) {
        if (this.verbose) System.out.println(x);
    }

    public NodeSpace getSpace() {
        return outputSpace;
    }

}
