package knox.spring.data.neo4j.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;


import knox.spring.data.neo4j.services.DesignSpaceService;

import java.util.*;

public class GoldbarTools {

    final DesignSpaceService designSpaceService;

    public GoldbarTools(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @Tool(description = "Generate a GOLDBAR expression from natural-language criteria - Call this tool every time GOLDBAR expression generation is needed")
    String generateGoldbar(@ToolParam(description = "Natural-language criteria describing sequence constraints") String criteria) {

        System.out.println("\nAI: generateGoldbar\n");
        System.out.println(criteria);

        // Core grammar (kept for grounding, but model must not echo it)
        String grammarDefinition =
                "GOLDBAR Grammar:\n" +
                "Seq  → Exp '.' Seq | Exp 'then' Seq | Exp\n" +
                "Exp  → Term 'or' Exp | Term 'and0' Exp | Term 'and1' Exp | Term 'and2' Exp | Term 'merge' Exp | Term\n" +
                "Term → 'one-or-more' Term | 'zero-or-more' Term | 'zero-or-one' Term | 'reverse-comp' Term | 'forward-or-reverse' Term |\n" +
                "        'zero-or-one-sbol' Term | 'zero-or-more-sbol' Term | '{' Seq '}' | '(' Seq ')' | Atom\n" +
                "Atom → RegExp \"([A-Za-z0-9]|-|_)+\"\n";

        // Canonical NL → GOLDBAR mapping rules
        String canonicalMappings =
                "Canonical NL→GOLDBAR mappings:\n" +
                "- \"X cannot/does not repeat\" → \"(zero-or-more(any_except_X) then X then zero-or-more(any_except_X)) or (zero-or-more(any_except_X))\"\n" +
                "- \"X only before Y\" → \"(zero-or-more(any_except_Y) then X then zero-or-more(any_except_X)) or (zero-or-more(any_except_X))\"\n" +
                "- \"X not followed by Y\" → \"(zero-or-more((any_except_X) or (X then any_except_XandY))) then zero-or-more(X)\"\n" +
                "- \"must include/have/use X\" → \"(one-or-more(zero-or-more(any_except_X) then X) then zero-or-more(any_except_X))\"\n" +
                "- \"X and Y not together\" → \"(zero-or-more(any_except_XandY)) or (zero-or-more(any_except_XandY or X) then X then zero-or-more(any_except_XandY)) or (zero-or-more(any_except_XandY or Y) then Y then zero-or-more(any_except_XandY))\"\n" +
                "- \"X and Y together\" → \"((zero-or-more(any_part) then X then zero-or-more(any_except_XandY) then Y then zero-or-more(any_except_X)) or (zero-or-more(any_part) then Y then zero-or-more(any_except_XandY) then X then zero-or-more(any_except_Y)) or (zero-or-more(any_except_XandY)))\"\n" +
                "- \"X appears at most once\" → \"zero-or-one(X)\"\n" +
                "- \"X is optional\" → \"zero-or-one(X)\"\n" +
                "- \"X appears zero or more times\" → \"zero-or-more(X)\"\n" +
                "- \"X appears one or more times\" → \"one-or-more(X)\"\n" +
                "- \"X appears exactly once\" → \"X\"\n" +
                "- \"X followed by Y\" → \"X then Y\"\n" +
                "- \"either X or Y\" → \"(X or Y)\"\n" +
                "- \"ends with X\" → \"(zero-or-more(any_part) then X)\"\n" +
                "- \"starts with X\" → \"(X then zero-or-more(any_part))\"\n" +
                //"- \"X and Y\" → \"(X and1 Y)\"\n" +
                "- \"group containing X then Y\" → \"{ X then Y }\"\n" +
                "- \"reverse complement of X\" → \"reverse-comp(X)\"\n" +
                "- \"X in either orientation\" → \"forward-or-reverse(X)\"\n" +
                "- \"any part\" → \"any_part\"\n" +
                "- \"any except X\" → \"any_except_X\"\n" +
                "- \"any except X and Y\" → \"any_except_XandY\"\n";

        // Output contract: GOLDBAR only
        String outputContract =
                "Your task:\n" +
                "- Translate the criteria into a valid GOLDBAR expression.\n" +
                "- Output ONLY the GOLDBAR expression.\n" +
                "- No English. No explanations. No JSON. No grammar restatement.\n" +
                "- If the criteria cannot be expressed, output an empty string.\n";

        return canonicalMappings + "\n" + outputContract + "\nCriteria: " + criteria;
    }
}

