package knox.spring.data.neo4j.sbol;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.exception.SBOLException;
import org.sbolstandard.core2.*;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.util.*;

public class SBOLGeneration {

    private String namespace = "http://knox.org";
    private String filepath = "/Users/vidyaakavoor/Documents/Knox_base/knox/src/main/java/knox/spring/data/neo4j/sbol/";


    private DesignSpace targetSpace;


    public SBOLGeneration(DesignSpace targetSpace, String namespace) {
        if (namespace != null && namespace.length() != 0) {
            this.namespace = namespace;
        }
        this.targetSpace = targetSpace;
    }

    private String mapToString(Map<String, Map<String, String>> map) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String key : map.keySet()) {
//            mapAsString.append(key + "=" + map.get(key) + ", ");
            mapAsString.append("\"" + key + "\" : {");
            for (String vKey : map.get(key).keySet()) {
                mapAsString.append("\"" + vKey + "\" : \"" + map.get(key).get(vKey) + "\", ");
            }
            mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}, ");
        }
        mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    private String listMapToString(Map<String, ArrayList<String>> map) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String key : map.keySet()) {
            mapAsString.append("\"" + key + "\": [");
            for (String val : map.get(key)) {
                mapAsString.append("\"" + val + "\", ");
            }
            mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("], ");
        }
        mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    private String getRoleName(String uri) {
        SequenceOntology sequenceOntology = new SequenceOntology();
        String[] s = uri.split("/");
        return sequenceOntology.getName(s[s.length -1]).toLowerCase();
    }

    private String getMemberName(String uri) {
        String[] s = uri.split("/");
        return s[s.length -2];
    }


    private String formatOr(ArrayList<String> parts) {
        String path = getRoleName(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            path = path + ", " + getRoleName(parts.get(i));
        }
        System.out.println(path);
        return path;
    }



    public List<String> createSBOLDocument () throws SBOLValidationException, IOException, SBOLConversionException, SBOLException, URISyntaxException {

        // get all NodeSpace info from targetSpace
        Set<Edge> edges = targetSpace.getEdges();
        Set<Node> nodes = targetSpace.getNodes();

        System.out.println("THIS IS THE DESIGN SPACE --> " + targetSpace.toString());

//        get all the roles and ids from the edges, in order to make hashMap below
        ArrayList<String> roles = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        Map<String,ArrayList<String>> categories = new HashMap<>();

        for (Edge e : edges) {
            roles.addAll(e.getComponentRoles());
            ids.addAll(e.getComponentIDs());
        }

        // make hashMap to map atoms to their members (atoms are in "roles" and members are in "ids")
        for (int i = 0; i < roles.size(); i++) {
            String r = getRoleName(roles.get(i));
            if (categories.containsKey(r)){
                ArrayList<String> vals = categories.get(r);
                vals.add(getMemberName(ids.get(i)));
                categories.put(r, vals);
            } else {
                ArrayList<String> val = new ArrayList<>();
                val.add(getMemberName(ids.get(i)));
                categories.put(r, val);
            }
        }

        /* ***** THIS IS WHERE THE GOLDBAR ATTEMPT STARTS ***** */


        // create empty dictionary to be the transition function input
        Map<String,Map<String, String>> transitionFunction = new HashMap<>();
        for (Node n : nodes) {
            String id = n.getNodeID();
            Map<String, String> transition = new HashMap<>();
            for (Node n1 : nodes) {
                String id1 = n1.getNodeID();
                transition.put(id1, "_");
            }
            transitionFunction.put(id, transition);
        }
        // to use as list of states
        ArrayList<String> allNodeIDs = new ArrayList<>();

        // fill in empty transition function with the roles on the edges
        for (Node n : nodes) {
            String key = n.getNodeID();
            allNodeIDs.add(key);
            System.out.println("working with node --> " + key);
            Set<Edge> nodeEdges = n.getEdges();
            for (Edge e : nodeEdges) {
                String head = e.getHeadID();
                System.out.println("\thead node --> " + head);
                HashSet<String> edgeRoles = new HashSet<>(e.getComponentRoles());
                ArrayList<String> uniqueRoles = new ArrayList<>(edgeRoles);
                String path;
                if (edgeRoles.size() > 1) {
                    path = formatOr(uniqueRoles);
                } else if (edgeRoles.size() == 0) {
                    path = "e";
                } else {
                    path = getRoleName(uniqueRoles.get(0));
                }
                transitionFunction.get(key).put(head, path);
            }
        }


        // build string of node ids
        StringBuilder sb = new StringBuilder();
        for (String s : transitionFunction.keySet())
        {
            sb.append(s + " ");
        }
        sb.delete(sb.length()-1, sb.length());
        String states = sb.toString();

        // get start and accept nodes and turn transition function into string
        String start = targetSpace.getStartNode().getNodeID();
        String accept = targetSpace.getAcceptNodes().iterator().next().getNodeID();
        String transitionString = mapToString(transitionFunction);


        try {
            // get start and accept nodes and turn transition function into string
            BufferedWriter writer = new BufferedWriter(new FileWriter(filepath+"args.txt"));
            writer.write("-states\n" + states + "\n-start\n" + start + "\n-accept\n" + accept + "\n-transition\n" + transitionString);

            writer.close();
        } catch (IOException e) {
            System.out.println(e.toString());
        }


        ScriptPython scriptPython = new ScriptPython();
        String out = scriptPython.runScript();

        return Arrays.asList(out, listMapToString(categories));
    }

}


class ScriptPython {
    private Process mProcess;
    private String filepath = "/Users/vidyaakavoor/Documents/Knox_base/knox/src/main/java/knox/spring/data/neo4j/sbol/";

    public String runScript() {
        Process process;
        try {
            String cmd = "python3 " + filepath + "start_with_dict.py ";
            System.out.println("cmd --> " + cmd);
            String line;
            String out = "";

            process = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while((line = in.readLine()) != null) {
                out = line;
            }

            mProcess = process;
            return out;

        } catch(Exception e) {
            System.out.println("Exception Raised" + e.toString());
            return "";
        }
    }
}
