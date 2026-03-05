package knox.spring.data.neo4j.goldbar;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoldbarUtils {

    /**
     * Recursively propagates reverse complements in a GOLDBAR AST.
     * @param astNode The AST node (as a Map)
     * @param reverse Whether the current context is reversed
     * @return The transformed AST node
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> propagateReverseComplements(Map<String, Object> astNode, boolean reverse) {
        if (astNode.containsKey("ReverseComp")) {
            // Flip the reverse flag and recurse
            Object child = ((List<Object>) astNode.get("ReverseComp")).get(0);
            return propagateReverseComplements((Map<String, Object>) child, !reverse);
        } else if (astNode.containsKey("Then")) {
            List<Object> children = (List<Object>) astNode.get("Then");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            children.set(1, propagateReverseComplements((Map<String, Object>) children.get(1), reverse));
            if (reverse) {
                Collections.reverse(children);
            }
            return astNode;
        } else if (astNode.containsKey("Or")) {
            List<Object> children = (List<Object>) astNode.get("Or");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            children.set(1, propagateReverseComplements((Map<String, Object>) children.get(1), reverse));
            return astNode;
        } else if (astNode.containsKey("And0")) {
            List<Object> children = (List<Object>) astNode.get("And0");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            children.set(1, propagateReverseComplements((Map<String, Object>) children.get(1), reverse));
            return astNode;
        } else if (astNode.containsKey("And1")) {
            List<Object> children = (List<Object>) astNode.get("And1");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            children.set(1, propagateReverseComplements((Map<String, Object>) children.get(1), reverse));
            return astNode;
        } else if (astNode.containsKey("And2")) {
            List<Object> children = (List<Object>) astNode.get("And2");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            children.set(1, propagateReverseComplements((Map<String, Object>) children.get(1), reverse));
            return astNode;
        } else if (astNode.containsKey("Merge")) {
            List<Object> children = (List<Object>) astNode.get("Merge");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            children.set(1, propagateReverseComplements((Map<String, Object>) children.get(1), reverse));
            return astNode;
        } else if (astNode.containsKey("OneOrMore")) {
            List<Object> children = (List<Object>) astNode.get("OneOrMore");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            return astNode;
        } else if (astNode.containsKey("ZeroOrOne")) {
            List<Object> children = (List<Object>) astNode.get("ZeroOrOne");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            return astNode;
        } else if (astNode.containsKey("ZeroOrMore")) {
            List<Object> children = (List<Object>) astNode.get("ZeroOrMore");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            return astNode;
        } else if (astNode.containsKey("ZeroOrOneSBOL")) {
            List<Object> children = (List<Object>) astNode.get("ZeroOrOneSBOL");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            return astNode;
        } else if (astNode.containsKey("ZeroOrMoreSBOL")) {
            List<Object> children = (List<Object>) astNode.get("ZeroOrMoreSBOL");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            return astNode;
        } else if (astNode.containsKey("ForwardOrReverse")) {
            List<Object> children = (List<Object>) astNode.get("ForwardOrReverse");
            children.set(0, propagateReverseComplements((Map<String, Object>) children.get(0), reverse));
            return astNode;
        } else if (astNode.containsKey("Atom")) {
            if (reverse) {
                Map<String, Object> revComp = new HashMap<>();
                revComp.put("ReverseComp", Collections.singletonList(astNode));
                return revComp;
            } else {
                return astNode;
            }
        }
        return astNode;
    }

    // Overload for initial call with reverse = false
    public static Map<String, Object> propagateReverseComplements(Map<String, Object> astNode) {
        return propagateReverseComplements(astNode, false);
    }
}
