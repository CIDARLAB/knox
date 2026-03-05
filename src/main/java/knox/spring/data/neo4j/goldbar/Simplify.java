package knox.spring.data.neo4j.goldbar;

import java.util.*;

public class Simplify {

    // -------------------------------------------------------------------------
    // Factory methods (mirror JS factory functions)
    // -------------------------------------------------------------------------

    public static Map<String, Object> Or(Object exp1, Object exp2) {
        List<Object> args = new ArrayList<>();
        args.add(exp1);
        args.add(exp2);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Or", args);
        return m;
    }

    public static Map<String, Object> Then(Object exp1, Object exp2) {
        List<Object> args = new ArrayList<>();
        args.add(exp1);
        args.add(exp2);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Then", args);
        return m;
    }

    public static Map<String, Object> OneOrMore(Object exp) {
        List<Object> args = new ArrayList<>();
        args.add(exp);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("OneOrMore", args);
        return m;
    }

    public static Map<String, Object> ZeroOrMore(Object exp) {
        List<Object> args = new ArrayList<>();
        args.add(exp);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ZeroOrMore", args);
        return m;
    }

    public static Map<String, Object> ZeroOrOne(Object exp) {
        List<Object> args = new ArrayList<>();
        args.add(exp);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ZeroOrOne", args);
        return m;
    }

    public static Map<String, Object> ForwardOrReverse(Object exp) {
        List<Object> args = new ArrayList<>();
        args.add(exp);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ForwardOrReverse", args);
        return m;
    }

    // -------------------------------------------------------------------------
    // Equality check (mirrors JSON.stringify comparison)
    // -------------------------------------------------------------------------

    public static boolean equalExps(Object exp1, Object exp2) {
        if (exp1 == null && exp2 == null) return true;
        if (exp1 == null || exp2 == null) return false;
        return exp1.toString().equals(exp2.toString());
    }

    // -------------------------------------------------------------------------
    // Main simplify entry point
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplify(Map<String, Object> parsed) {
        parsed = simplifyRevComp(parsed);
        parsed = simplifyTree(parsed);
        return parsed;
    }

    // -------------------------------------------------------------------------
    // simplifyTree
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplifyTree(Map<String, Object> parsed) {
        if (parsed.containsKey("Atom")) {
            return parsed;
        } else if (parsed.containsKey("Or")) {
            List<Object> parts = (List<Object>) parsed.get("Or");
            Map<String, Object> exp1 = simplifyTree((Map<String, Object>) parts.get(0));
            Map<String, Object> exp2 = simplifyTree((Map<String, Object>) parts.get(1));
            return simplifyOr(exp1, exp2);
        } else if (parsed.containsKey("Then")) {
            List<Object> parts = (List<Object>) parsed.get("Then");
            Map<String, Object> exp1 = simplifyTree((Map<String, Object>) parts.get(0));
            Map<String, Object> exp2 = simplifyTree((Map<String, Object>) parts.get(1));
            return simplifyThen(exp1, exp2);
        } else if (parsed.containsKey("OneOrMore")) {
            List<Object> parts = (List<Object>) parsed.get("OneOrMore");
            Map<String, Object> exp = simplifyTree((Map<String, Object>) parts.get(0));
            return simplifyOneOrMore(exp);
        } else if (parsed.containsKey("ZeroOrMore")) {
            List<Object> parts = (List<Object>) parsed.get("ZeroOrMore");
            Map<String, Object> exp = simplifyTree((Map<String, Object>) parts.get(0));
            return simplifyZeroOrMore(exp);
        } else if (parsed.containsKey("ZeroOrOne")) {
            List<Object> parts = (List<Object>) parsed.get("ZeroOrOne");
            Map<String, Object> exp = simplifyTree((Map<String, Object>) parts.get(0));
            return simplifyZeroOrOne(exp);
        }
        return parsed;
    }

    // -------------------------------------------------------------------------
    // simplifyOr
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplifyOr(Map<String, Object> exp1, Map<String, Object> exp2) {

        if (exp1.containsKey("Atom") && exp2.containsKey("Atom") && equalExps(exp1, exp2)) {
            return exp1;
        } else if (exp1.containsKey("OneOrMore") && exp2.containsKey("OneOrMore")
                && equalExps(exp1.get("OneOrMore"), exp2.get("OneOrMore"))) {
            // one-or-more a or one-or-more a --> one-or-more a
            return exp1;
        } else if (exp1.containsKey("OneOrMore") && exp2.containsKey("ZeroOrMore")
                && equalExps(exp1.get("OneOrMore"), exp2.get("ZeroOrMore"))) {
            // one-or-more a or zero-or-more a --> zero-or-more a
            return exp2;
        } else if (exp1.containsKey("OneOrMore") && exp2.containsKey("ZeroOrOne")
                && equalExps(exp1.get("OneOrMore"), exp2.get("ZeroOrOne"))) {
            // one-or-more a or zero-or-one a --> zero-or-more a
            List<Object> inner = (List<Object>) exp2.get("ZeroOrOne");
            return ZeroOrMore(inner.get(0));
        } else if (exp1.containsKey("OneOrMore")
                && equalExps(((List<Object>) exp1.get("OneOrMore")).get(0), exp2)) {
            // one-or-more a or a --> one-or-more a
            return exp1;
        } else if (exp2.containsKey("OneOrMore")
                && equalExps(exp1, ((List<Object>) exp2.get("OneOrMore")).get(0))) {
            // a or one-or-more a --> one-or-more a
            return exp2;
        } else if (exp1.containsKey("ZeroOrMore") && exp2.containsKey("OneOrMore")
                && equalExps(exp1.get("ZeroOrMore"), exp2.get("OneOrMore"))) {
            // zero-or-more a or one-or-more a --> zero-or-more a
            return exp1;
        } else if (exp1.containsKey("ZeroOrMore") && exp2.containsKey("ZeroOrMore")
                && equalExps(exp1.get("ZeroOrMore"), exp2.get("ZeroOrMore"))) {
            // zero-or-more a or zero-or-more a --> zero-or-more a
            return exp1;
        } else if (exp1.containsKey("ZeroOrMore") && exp2.containsKey("ZeroOrOne")
                && equalExps(exp1.get("ZeroOrMore"), exp2.get("ZeroOrOne"))) {
            // zero-or-more a or zero-or-one a --> zero-or-more a
            return exp1;
        } else if (exp1.containsKey("ZeroOrMore")
                && equalExps(((List<Object>) exp1.get("ZeroOrMore")).get(0), exp2)) {
            // zero-or-more a or a --> zero-or-more a
            return exp1;
        } else if (exp2.containsKey("ZeroOrMore")
                && equalExps(exp1, ((List<Object>) exp2.get("ZeroOrMore")).get(0))) {
            // a or zero-or-more a --> zero-or-more a
            return exp2;
        } else if (exp1.containsKey("ZeroOrOne") && exp2.containsKey("OneOrMore")
                && equalExps(exp1.get("ZeroOrOne"), exp2.get("OneOrMore"))) {
            // zero-or-one a or one-or-more a --> zero-or-more a
            List<Object> inner = (List<Object>) exp1.get("ZeroOrOne");
            return ZeroOrMore(inner.get(0));
        } else if (exp1.containsKey("ZeroOrOne") && exp2.containsKey("ZeroOrMore")
                && equalExps(exp1.get("ZeroOrOne"), exp2.get("ZeroOrMore"))) {
            // zero-or-one a or zero-or-more a --> zero-or-more a
            return exp2;
        } else if (exp1.containsKey("ZeroOrOne") && exp2.containsKey("ZeroOrOne")
                && !equalExps(exp1, exp2)) {
            // zero-or-one a or zero-or-one b --> zero-or-one (a or b)
            List<Object> inner1 = (List<Object>) exp1.get("ZeroOrOne");
            List<Object> inner2 = (List<Object>) exp2.get("ZeroOrOne");
            return ZeroOrOne(Or(inner1, inner2));
        } else if (exp1.containsKey("ZeroOrOne") && exp2.containsKey("ZeroOrOne")
                && equalExps(exp1, exp2)) {
            // zero-or-one a or zero-or-one a --> zero-or-one a
            return exp1;
        } else if (exp1.containsKey("ZeroOrOne")
                && equalExps(((List<Object>) exp1.get("ZeroOrOne")).get(0), exp2)) {
            // zero-or-one a or a --> zero-or-one a
            return exp1;
        } else if (exp2.containsKey("ZeroOrOne")
                && equalExps(((List<Object>) exp2.get("ZeroOrOne")).get(0), exp1)) {
            // a or zero-or-one a --> zero-or-one a
            return exp2;
        }

        return Or(exp1, exp2);
    }

    // -------------------------------------------------------------------------
    // simplifyThen
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplifyThen(Map<String, Object> exp1, Map<String, Object> exp2) {

        if (exp1.containsKey("OneOrMore") && exp2.containsKey("OneOrMore")
                && equalExps(exp1.get("OneOrMore"), exp2.get("OneOrMore"))) {
            // one-or-more a then one-or-more a --> a then one-or-more a
            List<Object> inner = (List<Object>) exp1.get("OneOrMore");
            return Then(inner.get(0), exp2);
        } else if (exp1.containsKey("OneOrMore") && exp2.containsKey("ZeroOrMore")
                && equalExps(exp1.get("OneOrMore"), exp2.get("ZeroOrMore"))) {
            // one-or-more a then zero-or-more a --> one-or-more a
            return exp1;
        } else if (exp1.containsKey("OneOrMore") && exp2.containsKey("ZeroOrOne")
                && equalExps(exp1.get("OneOrMore"), exp2.get("ZeroOrOne"))) {
            // one-or-more a then zero-or-one a --> one-or-more a
            return exp1;
        } else if (exp1.containsKey("ZeroOrMore") && exp2.containsKey("OneOrMore")
                && equalExps(exp1.get("ZeroOrMore"), exp2.get("OneOrMore"))) {
            // zero-or-more a then one-or-more a --> one-or-more a
            return exp2;
        } else if (exp1.containsKey("ZeroOrMore") && exp2.containsKey("ZeroOrMore")
                && equalExps(exp1.get("ZeroOrMore"), exp2.get("ZeroOrMore"))) {
            // zero-or-more a then zero-or-more a --> zero-or-more a
            return exp1;
        } else if (exp1.containsKey("ZeroOrMore") && exp2.containsKey("ZeroOrOne")
                && equalExps(exp1.get("ZeroOrMore"), exp2.get("ZeroOrOne"))) {
            // zero-or-more a then zero-or-one a --> zero-or-more a
            return exp1;
        } else if (exp1.containsKey("ZeroOrMore")
                && equalExps(((List<Object>) exp1.get("ZeroOrMore")).get(0), exp2)) {
            // zero-or-more a then a --> one-or-more a
            return OneOrMore(exp2);
        } else if (exp2.containsKey("ZeroOrMore")
                && equalExps(exp1, ((List<Object>) exp2.get("ZeroOrMore")).get(0))) {
            // a then zero-or-more a --> one-or-more a
            return OneOrMore(exp1);
        } else if (exp1.containsKey("ZeroOrOne") && exp2.containsKey("OneOrMore")
                && equalExps(exp1.get("ZeroOrOne"), exp2.get("OneOrMore"))) {
            return exp2;
        } else if (exp1.containsKey("ZeroOrOne") && exp2.containsKey("ZeroOrMore")
                && equalExps(exp1.get("ZeroOrOne"), exp2.get("ZeroOrMore"))) {
            return exp2;
        }

        return Then(exp1, exp2);
    }

    // -------------------------------------------------------------------------
    // simplifyOneOrMore
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplifyOneOrMore(Map<String, Object> exp) {
        if (exp.containsKey("ZeroOrMore") || exp.containsKey("OneOrMore")) {
            return exp;
        } else if (exp.containsKey("ZeroOrOne")) {
            List<Object> inner = (List<Object>) exp.get("ZeroOrOne");
            return ZeroOrMore(inner.get(0));
        }
        return OneOrMore(exp);
    }

    // -------------------------------------------------------------------------
    // simplifyZeroOrMore
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplifyZeroOrMore(Map<String, Object> exp) {
        if (exp.containsKey("OneOrMore")) {
            List<Object> inner = (List<Object>) exp.get("OneOrMore");
            return ZeroOrMore(inner.get(0));
        } else if (exp.containsKey("ZeroOrMore")) {
            return exp;
        } else if (exp.containsKey("ZeroOrOne")) {
            List<Object> inner = (List<Object>) exp.get("ZeroOrOne");
            return ZeroOrMore(inner.get(0));
        }
        return ZeroOrMore(exp);
    }

    // -------------------------------------------------------------------------
    // simplifyZeroOrOne
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplifyZeroOrOne(Map<String, Object> exp) {
        if (exp.containsKey("OneOrMore")) {
            List<Object> inner = (List<Object>) exp.get("OneOrMore");
            return ZeroOrMore(inner.get(0));
        } else if (exp.containsKey("ZeroOrMore") || exp.containsKey("ZeroOrOne")) {
            return exp;
        }
        return ZeroOrOne(exp);
    }

    // -------------------------------------------------------------------------
    // simplifyRevComp
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> simplifyRevComp(Map<String, Object> parsed) {
        if (parsed.containsKey("Atom")) {
            return parsed;
        }
        if (parsed.containsKey("ReverseComp")) {
            List<Object> inner = (List<Object>) parsed.get("ReverseComp");
            List<Object> ret = applyRevComp(inner);
            return (Map<String, Object>) ret.get(0);
        } else {
            String op = parsed.keySet().iterator().next();
            List<Object> parts = (List<Object>) parsed.get(op);
            List<Object> simplified = new ArrayList<>();
            for (Object part : parts) {
                simplified.add(simplifyRevComp((Map<String, Object>) part));
            }
            parsed.put(op, simplified);
            return parsed;
        }
    }

    // -------------------------------------------------------------------------
    // applyRevComp
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static List<Object> applyRevComp(List<Object> rcExp) {
        List<Object> applied = new ArrayList<>();
        for (Object part : rcExp) {
            Map<String, Object> partMap = (Map<String, Object>) part;
            if (partMap.containsKey("Atom")) {
                // RC of atom --> ReverseComp(atom)
                Map<String, Object> rc = new LinkedHashMap<>();
                List<Object> atomInner = new ArrayList<>();
                atomInner.add(partMap.get("Atom"));
                rc.put("ReverseComp", atomInner);
                applied.add(rc);
            } else if (partMap.containsKey("ReverseComp")) {
                // RC of RC --> cancel out
                List<Object> inner = (List<Object>) partMap.get("ReverseComp");
                applied.add(inner.get(0));
            } else if (partMap.containsKey("Then")) {
                // RC of Then --> swap order and apply RC to each part
                List<Object> thenParts = (List<Object>) partMap.get("Then");
                List<Object> swapped = new ArrayList<>();
                swapped.add(thenParts.get(1));
                swapped.add(thenParts.get(0));
                partMap.put("Then", applyRevComp(swapped));
                applied.add(partMap);
            } else {
                // apply RC recursively to sub-parts
                String op = partMap.keySet().iterator().next();
                partMap.put(op, applyRevComp((List<Object>) partMap.get(op)));
                applied.add(partMap);
            }
        }
        return applied;
    }

    // -------------------------------------------------------------------------
    // Main - quick smoke test
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        String[] tests = {
            "A then B then C",
            "one-or-more A or one-or-more A",
            "zero-or-more A then A",
            "A or zero-or-one A",
            "one-or-more A then zero-or-more A",
            "reverse-comp (A then B)"
        };

        for (String input : tests) {
            Map<String, Object> tree = (Map<String, Object>) Imparse.parse(input);
            Map<String, Object> simplified = simplify(tree);
            System.out.println("Input     : " + input);
            System.out.println("Parsed    : " + tree);
            System.out.println("Simplified: " + simplified);
            System.out.println();
        }
    }
}
