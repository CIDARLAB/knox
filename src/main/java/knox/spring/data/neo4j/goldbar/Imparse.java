package knox.spring.data.neo4j.goldbar;

import java.util.*;
import java.util.regex.*;

public class Imparse {

    // Token class
    public static class Token {
        public String str;
        public int row, col;
        public Token(String str, int row, int col) {
            this.str = str; this.row = row; this.col = col;
        }
        @Override public String toString() { return "Token('" + str + "', " + row + "," + col + ")"; }
    }

    // Grammar types
    // Grammar is List<Map<String, List<Map<String, List<Object>>>>>
    // Each entry: nonterminal -> list of alternatives -> {constructorName -> sequence}
    // Sequence entries: String (literal), Map with "RegExp" key, or List<String> (nonterminal ref)

    @SuppressWarnings("unchecked")
    public static List<String> extractTerminals(List<Map<String, List<Map<String, List<Object>>>>> grammar) {
        List<String> terminals = new ArrayList<>();
        for (var prod : grammar) {
            for (var nt : prod.values()) {
                for (var option : nt) {
                    for (var seq : option.values()) {
                        for (var entry : seq) {
                            if (!(entry instanceof List)) {
                                terminals.add(entryToString(entry));
                            }
                        }
                    }
                }
            }
        }
        return terminals;
    }

    private static String entryToString(Object entry) {
        if (entry instanceof String) return (String) entry;
        if (entry instanceof Map) {
            Map<?,?> m = (Map<?,?>) entry;
            if (m.containsKey("RegExp")) return (String) m.get("RegExp");
        }
        return entry.toString();
    }

    @SuppressWarnings("unchecked")
    public static List<Token> tokenize(List<Map<String, List<Map<String, List<Object>>>>> grammar, String s) {
        List<String> terminals = extractTerminals(grammar);
        List<Token> tokens = new ArrayList<>();
        int row = 0, col = 0;

        while (!s.isEmpty()) {
            // Skip whitespace
            while (!s.isEmpty() && (s.charAt(0) == ' ' || s.charAt(0) == '\n')) {
                if (s.charAt(0) == '\n') { row++; col = 0; }
                else col++;
                s = s.substring(1);
            }
            if (s.isEmpty()) break;

            String bestMatch = "";
            for (String terminal : terminals) {
                if (terminal == null) continue;
                // Check if it's a RegExp terminal (stored as "RegExp:..." marker won't work - 
                // we need to differentiate, so terminals list stores raw regex or literal)
                // We'll re-extract properly below
            }

            // Re-extract properly with type info
            bestMatch = findBestMatch(grammar, s);

            if (!bestMatch.isEmpty()) {
                tokens.add(new Token(bestMatch, row, col));
                col += bestMatch.length();
                s = s.substring(bestMatch.length());
            } else {
                if (!s.isEmpty()) System.out.println("Did not tokenize entire string.");
                break;
            }
        }
        return tokens;
    }

    @SuppressWarnings("unchecked")
    private static String findBestMatch(List<Map<String, List<Map<String, List<Object>>>>> grammar, String s) {
        String best = "";
        for (var prod : grammar) {
            for (var nt : prod.values()) {
                for (var option : nt) {
                    for (var seq : option.values()) {
                        for (var entry : seq) {
                            if (entry instanceof List) continue; // nonterminal ref, skip
                            if (entry instanceof Map) {
                                Map<?,?> m = (Map<?,?>) entry;
                                if (m.containsKey("RegExp")) {
                                    String regex = (String) m.get("RegExp");
                                    Matcher matcher = Pattern.compile("^" + regex).matcher(s);
                                    if (matcher.find()) {
                                        String matched = matcher.group();
                                        if (matched.length() > best.length()) best = matched;
                                    }
                                }
                            } else if (entry instanceof String) {
                                String literal = (String) entry;
                                if (s.startsWith(literal) && literal.length() > best.length()) {
                                    best = literal;
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    public static String showTokens(List<Token> ts) {
        StringBuilder sb = new StringBuilder();
        int row = 0, col = 0;
        for (Token t : ts) {
            while (row < t.row) { sb.append('\n'); row++; col = 0; }
            while (col < t.col) { sb.append(' '); col++; }
            sb.append(t.str);
            col += t.str.length();
        }
        return sb.toString();
    }

    // Parse result: [tree, remainingTokens] or error map
    @SuppressWarnings("unchecked")
    public static Object[] parseTokens(
            List<Map<String, List<Map<String, List<Object>>>>> grammar,
            List<Token> tsOriginal,
            String nonterm) {

        for (var prod : grammar) {
            if (!prod.containsKey(nonterm)) continue;
            List<Map<String, List<Object>>> options = prod.get(nonterm);

            for (var option : options) {
                List<Token> ts = new ArrayList<>(tsOriginal);
                for (var entry : option.entrySet()) {
                    String con = entry.getKey();
                    List<Object> seq = entry.getValue();
                    boolean success = true;
                    List<Object> subtrees = new ArrayList<>();

                    for (Object seqEntry : seq) {
                        if (ts.isEmpty()) { success = false; break; }

                        if (seqEntry instanceof List) {
                            // Nonterminal reference
                            String ref = (String) ((List<?>) seqEntry).get(0);
                            Object[] result = parseTokens(grammar, ts, ref);
                            if (result != null && result.length == 2) {
                                subtrees.add(result[0]);
                                ts = (List<Token>) result[1];
                            } else {
                                return result; // propagate error
                            }
                        } else if (seqEntry instanceof Map) {
                            Map<?,?> m = (Map<?,?>) seqEntry;
                            if (m.containsKey("RegExp")) {
                                String regex = (String) m.get("RegExp");
                                Matcher matcher = Pattern.compile("^" + regex + "$").matcher(ts.get(0).str);
                                if (matcher.matches()) {
                                    subtrees.add(ts.get(0).str);
                                    ts = ts.subList(1, ts.size());
                                } else { success = false; break; }
                            }
                        } else if (seqEntry instanceof String) {
                            if (ts.get(0).str.equals(seqEntry)) {
                                ts = ts.subList(1, ts.size());
                            } else { success = false; break; }
                        }
                    }

                    if (success) {
                        if (!con.isEmpty()) {
                            Map<String, Object> node = new LinkedHashMap<>();
                            node.put(con, subtrees);
                            return new Object[]{node, new ArrayList<>(ts)};
                        } else {
                            // Pass-through
                            if (subtrees.size() != 1)
                                return new Object[]{Map.of("Error", "Improperly defined production rule.")};
                            return new Object[]{subtrees.get(0), new ArrayList<>(ts)};
                        }
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Object parse(List<Map<String, List<Map<String, List<Object>>>>> grammar, String s) {
        if (grammar.isEmpty()) return Map.of("Error", "Cannot use the supplied grammar object.");
        String nonterm = grammar.get(0).keySet().iterator().next();
        List<Token> tokens = tokenize(grammar, s);
        Object[] result = parseTokens(grammar, tokens, nonterm);
        if (result == null) return Map.of("Error", "Parse failed.");
        return result[0];
    }

    @SuppressWarnings("unchecked")
    public static Object parse(String s) {
        List<Map<String, List<Map<String, List<Object>>>>> grammar = buildGrammar();
        if (grammar.isEmpty()) return Map.of("Error", "Cannot use the supplied grammar object.");
        String nonterm = grammar.get(0).keySet().iterator().next();
        List<Token> tokens = tokenize(grammar, s);
        Object[] result = parseTokens(grammar, tokens, nonterm);
        if (result == null) return Map.of("Error", "Parse failed.");
        return result[0];
    }

    // -------------------------------------------------------------------------
    // Example: build the grammar from the JS definition and run a quick test
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        // Build GRAMMAR_DEF programmatically to mirror the JS constant.
        // Seq -> (Exp . Seq) | (Exp then Seq) | (Exp)
        // Exp -> (Term or Exp) | ... | (Term)
        // Term -> one-or-more Term | ... | Atom

        List<Map<String, List<Map<String, List<Object>>>>> grammar = buildGrammar();

        String input = "foo . bar then baz";
        Object tree = parse(grammar, input);
        System.out.println("Input : " + input);
        System.out.println("Tree  : " + tree);
    }

    /** Builds the GRAMMAR_DEF equivalent of the JS constant. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, List<Map<String, List<Object>>>>> buildGrammar() {
        Map<String, Object> regexpAtom = new LinkedHashMap<>();
        regexpAtom.put("RegExp", "([A-Za-z0-9]|-|_)+");

        // Helper lambdas
        // nt(name) -> List.of(name)  (nonterminal reference)
        // lit(s)   -> s              (literal)

        // --- Term ---
        List<Map<String, List<Object>>> termOptions = new ArrayList<>();
        termOptions.add(opt("OneOrMore",   List.of("one-or-more", List.of("Term"))));
        termOptions.add(opt("OneOrMore",   List.of("oom", List.of("Term"))));
        termOptions.add(opt("ZeroOrMore",  List.of("zero-or-more", List.of("Term"))));
        termOptions.add(opt("ZeroOrMore",  List.of("zom", List.of("Term"))));
        termOptions.add(opt("ZeroOrOne",   List.of("zero-or-one", List.of("Term"))));
        termOptions.add(opt("ZeroOrOne",   List.of("zoo", List.of("Term"))));
        termOptions.add(opt("ReverseComp", List.of("reverse-comp", List.of("Term"))));
        termOptions.add(opt("ReverseComp", List.of("rev", List.of("Term"))));
        termOptions.add(opt("ForwardOrReverse", List.of("forward-or-reverse", List.of("Term"))));
        termOptions.add(opt("ForwardOrReverse", List.of("frev", List.of("Term"))));
        termOptions.add(opt("ZeroOrOneSBOL",  List.of("zero-or-one-sbol", List.of("Term"))));
        termOptions.add(opt("ZeroOrMoreSBOL", List.of("zero-or-more-sbol", List.of("Term"))));
        termOptions.add(opt("", List.of("{", List.of("Seq"), "}")));
        termOptions.add(opt("", List.of("(", List.of("Seq"), ")")));
        termOptions.add(opt("Atom", List.of(regexpAtom)));

        // --- Exp ---
        List<Map<String, List<Object>>> expOptions = new ArrayList<>();
        expOptions.add(opt("Or",    List.of(List.of("Term"), "or",    List.of("Exp"))));
        expOptions.add(opt("And1",  List.of(List.of("Term"), "and",  List.of("Exp")))); // Default 'and' behavior
        expOptions.add(opt("And0",  List.of(List.of("Term"), "and0",  List.of("Exp"))));
        expOptions.add(opt("And1",  List.of(List.of("Term"), "and1",  List.of("Exp"))));
        expOptions.add(opt("And2",  List.of(List.of("Term"), "and2",  List.of("Exp"))));
        expOptions.add(opt("Merge", List.of(List.of("Term"), "merge", List.of("Exp"))));
        expOptions.add(opt("",      List.of(List.of("Term"))));

        // --- Seq ---
        List<Map<String, List<Object>>> seqOptions = new ArrayList<>();
        seqOptions.add(opt("Then", List.of(List.of("Exp"), ".", List.of("Seq"))));
        seqOptions.add(opt("Then", List.of(List.of("Exp"), "then", List.of("Seq"))));
        seqOptions.add(opt("",     List.of(List.of("Exp"))));

        List<Map<String, List<Map<String, List<Object>>>>> grammar = new ArrayList<>();
        grammar.add(Map.of("Seq",  seqOptions));
        grammar.add(Map.of("Exp",  expOptions));
        grammar.add(Map.of("Term", termOptions));
        return grammar;
    }

    /** Creates a single option: {constructor -> sequence}. */
    private static Map<String, List<Object>> opt(String con, List<Object> seq) {
        Map<String, List<Object>> m = new LinkedHashMap<>();
        m.put(con, seq);
        return m;
    }
}
