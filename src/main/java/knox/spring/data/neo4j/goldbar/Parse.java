package knox.spring.data.neo4j.goldbar;

import java.util.*;

public class Parse {

    public static Map<String, Object> parse(List<Map<String, List<Map<String, List<Object>>>>> grammar, String s) throws IllegalArgumentException {
        validateGoldbar(s);
        Map<String, Object> tree = (Map<String, Object>) Imparse.parse(grammar, s);
        checkTree(tree);
		tree = GoldbarUtils.propagateReverseComplements(tree);
        Map<String, Object> simplified = Simplify.simplify(tree);
        return simplified;
    }

    public static Map<String, Object> parse(String s) throws IllegalArgumentException {
        validateGoldbar(s);
        Map<String, Object> tree = (Map<String, Object>) Imparse.parse(s);
        checkTree(tree);
		tree = GoldbarUtils.propagateReverseComplements(tree);
        Map<String, Object> simplified = Simplify.simplify(tree);
        return simplified;
    }

    private static void checkTree(Map<String, Object> tree) {
        if (tree == null || (tree instanceof Map && ((Map<?,?>)tree).containsKey("Error"))) {
            System.out.println("Invalid GOLDBAR: unbalanced brackets/parentheses or syntax error.");
            throw new IllegalArgumentException("Invalid GOLDBAR: unbalanced brackets/parentheses or syntax error.");
        }
    }

    public static void validateGoldbar(String goldbar) throws IllegalArgumentException {
        if (goldbar == null || goldbar.isBlank()) {
            throw new IllegalArgumentException("GOLDBAR input cannot be null or empty.");
        }
        if (!goldbar.matches("[A-Za-z0-9_(){}\\s.\\-]+")) {
            String invalidChars = goldbar.replaceAll("[A-Za-z0-9_(){}\\s.\\-]", "");
            throw new IllegalArgumentException("GOLDBAR contains invalid characters: '" + invalidChars + "'");
        }
    }
}
