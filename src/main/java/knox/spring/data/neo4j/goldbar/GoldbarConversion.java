package knox.spring.data.neo4j.goldbar;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

import knox.spring.data.neo4j.operations.JoinOperator;
import knox.spring.data.neo4j.operations.OROperator;

import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.json.JSONObject;

import com.google.gson.JsonArray;

import org.neo4j.ogm.json.JSONArray;
import org.neo4j.ogm.json.JSONException;

public class GoldbarConversion {

    JSONObject goldbar;

    JSONObject categories;

    Double weight;

    NodeSpace outputSpace;

    ArrayList<Node> boundaryStack;
    
    public GoldbarConversion(JSONObject goldbar, JSONObject categories, Double weight) {

        this.goldbar = goldbar;

        this.categories = categories;

        this.weight = weight;

        this.outputSpace = new NodeSpace();

        this.boundaryStack = new ArrayList<>();

    }

    public void convert() throws JSONException {
        Map<String, JSONArray> operationToVariableList = new HashMap<>();

        // Map operation to variableList
        operationToVariableList = goldbar.toMap();

        // Add space to spaces
        for (String operation : operationToVariableList.keySet()) {
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
                System.out.println("\nAtom: " + variableList.getString(0));
                space = handleAtomAndReverseComp(variableList.getString(0), Edge.Orientation.INLINE);
                System.out.println("Atom Space Created");
                System.out.println(space);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        
        } else if (operation.equals("ReverseComp")) {
            try {
                System.out.println("\nReverseComp: " + variableList.getString(0).substring(2,variableList.getString(0).length() - 2));
                space = handleAtomAndReverseComp(variableList.getString(0).substring(2,variableList.getString(0).length() - 2), Edge.Orientation.REVERSE_COMPLEMENT);
                System.out.println("Reverse Comp Space Created");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        
        } else if (operation.equals("Then")) {
            space = handleThen(variableList);
            System.out.println("\nThen Space Created");

        } else if (operation.equals("Or")) {
            space = handleOr(variableList);
            System.out.println("\nOr Space Created");

        } else if (operation.equals("ZeroOrMore")) {
            space = handleZeroOrMore(variableList);
            System.out.println("\nZeroOrMore Space Created");

        } else if (operation.equals("OneOrMore")) {
            space = handleOneOrMore(variableList);
            System.out.println("\nOneOrMore Space Created");

        } else if (operation.equals("ZeroOrOne")) {
            space = handleZeroOrOne(variableList);
            System.out.println("\nZeroOrOne Space Created");

        } else {
            System.out.println("\nError: (operation not valid)");
            System.out.println(operation);
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
            rolesToCompIds = categories.getJSONObject(variable).toMap();
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

        System.out.println(roleIsAbstract);

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

    private ArrayList<NodeSpace> getSpacesForOperation(JSONArray variableList) {
        ArrayList<NodeSpace> spaces = new ArrayList<>();
        
        for (int i = 0; i < variableList.length(); i++) {
            Map<String, JSONArray> operationToVariableList = new HashMap<>();

            try {
                // Map operation to variableList
                operationToVariableList = variableList.getJSONObject(i).toMap();

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
		default:
			return "http://knox.org/role/" + csvRole;
		}
	}

    public NodeSpace getSpace() {
        return outputSpace;
    }

}
