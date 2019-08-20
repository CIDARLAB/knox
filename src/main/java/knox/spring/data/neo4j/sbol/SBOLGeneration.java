package knox.spring.data.neo4j.sbol;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.exception.SBOLException;
import org.sbolstandard.core2.*;
import org.sbolstandard.core2.Collection;
import org.sbolstandard.core2.SystemsBiologyOntology;
//import sun.security.util.ArrayUtil;
import org.apache.commons.lang3.ArrayUtils;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class SBOLGeneration {

    private String namespace = "http://knox.org";
    private String DELIMITER = "/";
    private String COLLECTION = "_collection";
    private String VERSION = "1";
    private String DNA_REGION = "http://www.biopax.org/release/biopax-level3.owl#DnaRegion";
    private String OR_REGION = "http://identifiers.org/so/SO:0000804";
    private String filepath = "/Users/vidyaakavoor/Documents/Knox_base/knox/src/main/java/knox/spring/data/neo4j/sbol/";


    private DesignSpace targetSpace;


    public SBOLGeneration(DesignSpace targetSpace, String namespace) {
        if (namespace != null && namespace.length() != 0) {
            this.namespace = namespace;
        }
        this.targetSpace = targetSpace;
    }

    // makes component definitions for operations
    private ComponentDefinition makeOperationComponentDefinition(SBOLDocument document, String displayID) throws SBOLValidationException, URISyntaxException {
        URI typeURI = new URI(OR_REGION);

        if (displayID.equals("or_unit")){
            //eventually put the actual definition of the typeURI here and not outside of the IF
            return document.createComponentDefinition(displayID, VERSION, typeURI);
        }
        return document.createComponentDefinition(displayID, VERSION, typeURI);
    }

    // makes component definitions for atoms (specific because of type URI)
    private ComponentDefinition makeAtomComponentDefinition(SBOLDocument document, String displayID, String role) throws SBOLValidationException, URISyntaxException {
        URI typeURI = new URI(DNA_REGION);
        URI roleURI = new URI(role);
        ComponentDefinition compDef = document.createComponentDefinition(displayID, VERSION, typeURI);
        compDef.addRole(roleURI);
        return compDef;
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


    // makes collections for each atom and
    private void makeCollections(SBOLDocument document, Map<String,ArrayList<String>> rolesToIDs) throws SBOLValidationException, URISyntaxException {
        SequenceOntology sequenceOntology = new SequenceOntology();

        // last part of role URI is an SO number, use it to find the name of the atom
        // then create collection with name and version for each atom
        for (String role : rolesToIDs.keySet()) {
            ArrayList<String> ids = rolesToIDs.get(role);
            String[] s = role.split("/");
            String displayID = sequenceOntology.getName(s[s.length -1]);
            Collection collection = document.createCollection(displayID+COLLECTION, VERSION);

            // create generic component definition for atom and component definitions for each member
            makeAtomComponentDefinition(document, displayID, role);
            for (String id : ids) {
                // second to last part of id URI is the name of the type of atom (use as displayID)
                String[] s1 = id.split("/");
                String name = s1[s1.length -2];
                // make component definition and add as member
                collection.addMember(makeAtomComponentDefinition(document, name, role).getIdentity());
            }
        }
    }


    private String getRoleName(String uri) {
        SequenceOntology sequenceOntology = new SequenceOntology();
        String[] s = uri.split("/");
        return sequenceOntology.getName(s[s.length -1]).toLowerCase();
    }


    private String formatOr(ArrayList<String> parts) {
        String path = getRoleName(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            path = path + ", " + getRoleName(parts.get(i));
        }
        System.out.println(path);
        return path;
    }


//    public SBOLDocument createSBOLDocument () throws SBOLValidationException, IOException, SBOLConversionException, SBOLException, URISyntaxException {
//        SBOLDocument document = new SBOLDocument();
//        document.setDefaultURIprefix(namespace);
//
//        // TODO convert targetSpace to sbol doc
//
//        // get all NodeSpace info from targetSpace
//        Set<Edge> edges = targetSpace.getEdges();
//        Set<Node> nodes = targetSpace.getNodes();
//
//        System.out.println("THIS IS THE DESIGN SPACE --> " + targetSpace.toString());
//
//        // get all the roles and ids from the edges, in order to make hashMap below
//        ArrayList<String> roles = new ArrayList<>();
//        ArrayList<String> ids = new ArrayList<>();
//        Map<String,ArrayList<String>> rolesToIDs = new HashMap<>();
//
//        for (Edge e : edges) {
//            roles.addAll(e.getComponentRoles());
//            ids.addAll(e.getComponentIDs());
//        }
//
//        // make hashMap to map atoms to their members (atoms are in "roles" and members are in "ids")
//        for (int i = 0; i < roles.size(); i++) {
//            String r = roles.get(i);
//            if (rolesToIDs.containsKey(r)){
//                ArrayList<String> vals = rolesToIDs.get(r);
//                vals.add(ids.get(i));
//                rolesToIDs.put(r, vals);
//            } else {
//                ArrayList<String> val = new ArrayList<>();
//                val.add(ids.get(i));
//                rolesToIDs.put(r, val);
//            }
//        }
//
//        // MAKE COLLECTIONS for each atom (e.g. promoter, cds, etc)
//        // once this is done, there will be collections and component definitions for each atom and their members,
//        // and those members will be added as members of their respective collections
//        makeCollections(document, rolesToIDs);
//
//
///* ***** THIS IS WHERE THE GOLDBAR ATTEMPT STARTS ***** */
//
//
//        // create empty dictionary to be the transition function input
//        Map<String,Map<String, String>> transitionFunction = new HashMap<>();
//        for (Node n : nodes) {
//            String id = n.getNodeID();
//            Map<String, String> transition = new HashMap<>();
//            for (Node n1 : nodes) {
//                String id1 = n1.getNodeID();
//                transition.put(id1, "_");
//            }
//            transitionFunction.put(id, transition);
//        }
//        // to use as list of states
//        ArrayList<String> allNodeIDs = new ArrayList<>();
//
//        // fill in empty transition function with the roles on the edges
//        for (Node n : nodes) {
//            String key = n.getNodeID();
//            allNodeIDs.add(key);
//            System.out.println("working with node --> " + key);
//            Set<Edge> nodeEdges = n.getEdges();
//            for (Edge e : nodeEdges) {
//                String head = e.getHeadID();
//                System.out.println("\thead node --> " + head);
//                HashSet<String> edgeRoles = new HashSet<>(e.getComponentRoles());
//                ArrayList<String> uniqueRoles = new ArrayList<>(edgeRoles);
//                String path;
//                if (edgeRoles.size() > 1) {
//                    path = formatOr(uniqueRoles);
//                } else if (edgeRoles.size() == 0) {
//                    path = "e";
//                } else {
//                    path = getRoleName(uniqueRoles.get(0));
//                }
//                transitionFunction.get(key).put(head, path);
//            }
//        }
//
//
//        // build string of node ids
//        StringBuilder sb = new StringBuilder();
//        for (String s : transitionFunction.keySet())
//        {
//            sb.append(s + " ");
//        }
//        sb.delete(sb.length()-1, sb.length());
//        String states = sb.toString();
//
//        // get start and accept nodes and turn transition function into string
//        String start = targetSpace.getStartNode().getNodeID();
//        String accept = targetSpace.getAcceptNodes().iterator().next().getNodeID();
//        String transitionString = mapToString(transitionFunction);
//
//
//        try {
//            // get start and accept nodes and turn transition function into string
//            BufferedWriter writer = new BufferedWriter(new FileWriter(filepath+"args.txt"));
//            writer.write("-states\n" + states + "\n-start\n" + start + "\n-accept\n" + accept + "\n-transition\n" + transitionString);
//
//            writer.close();
//        } catch (IOException e) {
//            System.out.println(e.toString());
//        }
//
//
//        ScriptPython scriptPython = new ScriptPython();
//        String out = scriptPython.runScript();
//
//        System.out.println(out);
//
//        return document;
//    }


    public String createSBOLDocument () throws SBOLValidationException, IOException, SBOLConversionException, SBOLException, URISyntaxException {
//        SBOLDocument document = new SBOLDocument();
//        document.setDefaultURIprefix(namespace);

        // TODO convert targetSpace to sbol doc

        // get all NodeSpace info from targetSpace
        Set<Edge> edges = targetSpace.getEdges();
        Set<Node> nodes = targetSpace.getNodes();

        System.out.println("THIS IS THE DESIGN SPACE --> " + targetSpace.toString());

//        // get all the roles and ids from the edges, in order to make hashMap below
//        ArrayList<String> roles = new ArrayList<>();
//        ArrayList<String> ids = new ArrayList<>();
//        Map<String,ArrayList<String>> rolesToIDs = new HashMap<>();
//
//        for (Edge e : edges) {
//            roles.addAll(e.getComponentRoles());
//            ids.addAll(e.getComponentIDs());
//        }
//
//        // make hashMap to map atoms to their members (atoms are in "roles" and members are in "ids")
//        for (int i = 0; i < roles.size(); i++) {
//            String r = roles.get(i);
//            if (rolesToIDs.containsKey(r)){
//                ArrayList<String> vals = rolesToIDs.get(r);
//                vals.add(ids.get(i));
//                rolesToIDs.put(r, vals);
//            } else {
//                ArrayList<String> val = new ArrayList<>();
//                val.add(ids.get(i));
//                rolesToIDs.put(r, val);
//            }
//        }
//
//        // MAKE COLLECTIONS for each atom (e.g. promoter, cds, etc)
//        // once this is done, there will be collections and component definitions for each atom and their members,
//        // and those members will be added as members of their respective collections
//        makeCollections(document, rolesToIDs);


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

        System.out.println(out);

        return out;
    }



//    make compDefs for each part, generic ones and ones for each member (e.g. promoter gets one and each of the types of promoters get one)
//      looks at each generic one and then finds its members in the categories list (is there a category list?)
//    make a state stack from the node space info, looks like:
//      Or: [[{atom:promoter}], [{atom:cds}]]
//    use state stack to make SBOL
//      this is where you make the compDefs for operations
//    make rootCD to be template for combinatorial derivation
//    make combinatorial derivation based on rootCD
//      create components to add to the rootCD (for the biggest operation)
//    create a variable component for the biggest operation and add it to the combDev
//    add sequence constraints if needed


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
