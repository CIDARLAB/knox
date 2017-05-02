package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.eugene.Part.PartType;

public class EugeneConverter {
    private HashMap<PartType, Set<Part>> partsLibrary;

    public EugeneConverter(Set<Part> parts) {
        partsLibrary = new HashMap<PartType, Set<Part>>();

        for (Part part : parts) {
            if (!partsLibrary.containsKey(part.getType())) {
                partsLibrary.put(part.getType(), new HashSet<Part>());
            }

            partsLibrary.get(part.getType()).add(part);
        }
    }

    public DesignSpace convertDevice(Device device) {
        DesignSpace space = new DesignSpace(device.getID());

        SubSpace rootSpace = new SubSpace(space, device, partsLibrary);

        List<Ruleset> rulesets = composeRulesets(device.getRules());

        sortRulesets(rulesets, rootSpace);

        for (Ruleset ruleset : rulesets) {
            rootSpace.deletePart(ruleset.getImplicant());
        }

        HashMap<Set<String>, SubSpace> blah =
            new HashMap<Set<String>, SubSpace>();

        branch(rootSpace, rulesets, new HashSet<String>(), blah,
               new HashSet<Part>(), new HashSet<Part>(),
               new HashMap<Part, Set<Part>>(), new Ruleset());

        System.out.println("*********");
        for (Set<String> boo : blah.keySet()) {
            System.out.println("-------");
            System.out.println(boo.toString() + " " +
                               blah.get(boo).getNumNodes());
            System.out.println();
        }

        return space;
    }

    private Set<String> copyIDs(Set<String> ids) {
        Set<String> idCopies = new HashSet<String>();

        for (String id : ids) {
            idCopies.add(id);
        }

        return idCopies;
    }

    private Set<Part> collectImplicants(Ruleset ruleset, Set<Part> implicated,
                                        Set<Part> forbidden) {
        Set<Part> implicants = new HashSet<Part>();

        implicants.add(ruleset.getImplicant());

        for (Part part : ruleset.getCoImplicants()) {
            if (implicated.contains(part) && !forbidden.contains(part)) {
                implicants.add(part);
            }
        }

        return implicants;
    }

    private Set<Part> collectImplied(Ruleset ruleset, Set<Part> implicated) {
        Set<Part> implied = new HashSet<Part>();

        implied.addAll(ruleset.getImplied());

        for (Part part : ruleset.getWeaklyImplied()) {
            if (!implicated.contains(part)) {
                implied.add(part);
            }
        }

        return implied;
    }

    private boolean isAdjacencySatisfied(Part part, int partIndex,
                                         SubSpace subSpace, Set<Part> forbidden,
                                         HashMap<Part, Set<Part>> adjacencies,
                                         Part priorPart) {
        Set<Part> expected = new HashSet<Part>();

        expected.addAll(adjacencies.get(part));

        expected.remove(priorPart);

        if (expected.size() == 0) {
            System.out.println("true");

            return true;
        } else if (expected.size() == 1) {
            Part adjacent = expected.iterator().next();

            if (forbidden.contains(adjacent)) {
                System.out.println("forbiddenx2 " + adjacent.getID());

                return false;
            } else {
                List<Integer> nodeIndices = subSpace.getNodeIndices(adjacent);

                for (Integer nodeIndex : nodeIndices) {
                    if (nodeIndex == partIndex + 1) {
                        return isAdjacencySatisfied(adjacent, nodeIndex,
                                                    subSpace, forbidden,
                                                    adjacencies, part);
                    }
                }

                System.out.println("noAdjacent " + partIndex + ":" +
                                   nodeIndices.toString());

                return false;
            }
        } else {
            System.out.println("expected " + expected.size());

            return false;
        }
    }

    private boolean isAdjacencySatisfied(Ruleset ruleset, SubSpace subSpace,
                                         Set<Part> implicated,
                                         Set<Part> forbidden,
                                         HashMap<Part, Set<Part>> adjacencies,
                                         Ruleset priorRuleset) {
        if (adjacencies.containsKey(ruleset.getImplicant())) {
            Set<Part> expected = new HashSet<Part>();

            expected.addAll(adjacencies.get(ruleset.getImplicant()));

            if (ruleset.isAdjacentTo(priorRuleset)) {
                expected.remove(priorRuleset.getImplicant());
            }

            //			&&
            // adjacencies.get(expected.iterator().next()).contains(ruleset.getImplicant())

            if (expected.size() == 1) {
                Part adjacent = expected.iterator().next();

                if (forbidden.contains(adjacent)) {
                    return false;
                } else {
                    List<Integer> nodeIndices =
                        subSpace.getNodeIndices(adjacent);

                    int ruleIndex = ruleset.getIndex() -
                                    subSpace.getNumRootNodes() +
                                    subSpace.getNumNodes();

                    for (Integer nodeIndex : nodeIndices) {
                        if (nodeIndex == ruleIndex + 1) {
                            System.out.println("boh");

                            return isAdjacencySatisfied(
                                adjacent, nodeIndex, subSpace, forbidden,
                                adjacencies, ruleset.getImplicant());
                        }
                    }

                    return false;
                }
            } else {
                return false;
            }
        } else if (implicated.contains(ruleset.getImplicant())) {
            return false;
        } else {
            for (Part adjacent : ruleset.getAdjacent()) {
                if (implicated.contains(adjacent) &&
                    (!priorRuleset.getImplicant().equals(adjacent) ||
                     !ruleset.isAdjacentTo(priorRuleset))) {
                    return false;
                }
            }

            return true;
        }
    }

    private List<Part> getAdjacentSequence(
        Part part, HashMap<Part, Set<Part>> adjacencies) {
        List<Part> adjacentSequence = new LinkedList<Part>();

        adjacentSequence.add(part);

        boolean isDone = false;

        while (adjacencies.containsKey(part) && !isDone) {
            Set<Part> expected = new HashSet<Part>();

            expected.addAll(adjacencies.get(
                adjacentSequence.get(adjacentSequence.size() - 1)));

            if (adjacentSequence.size() > 1) {
                expected.remove(
                    adjacentSequence.get(adjacentSequence.size() - 2));
            }

            if (expected.size() > 0) {
                adjacentSequence.add(expected.iterator().next());
            } else {
                isDone = true;
            }
        }

        return adjacentSequence;
    }

    public void branch(SubSpace subSpace, List<Ruleset> rulesets,
                       Set<String> implicatedIDs,
                       HashMap<Set<String>, SubSpace> implicatedIDsToSubSpaces,
                       Set<Part> implicated, Set<Part> forbidden,
                       HashMap<Part, Set<Part>> adjacencies,
                       Ruleset priorRuleset) {
        System.out.println("++++++++++");
        System.out.println(implicatedIDs.toString());

        Stack<Ruleset> adjacencyStack = new Stack<Ruleset>();

        Stack<Integer> indexStack = new Stack<Integer>();

        for (int i = 0; i < rulesets.size(); i++) {
            Ruleset ruleset = rulesets.remove(i);

            System.out.println("---------");
            System.out.println(ruleset.getImplicant().getID() + " " +
                               ruleset.getIndex());

            if (ruleset.getIndex() - subSpace.getNumRootNodes() +
                        subSpace.getNumNodes() >=
                    0 &&
                !forbidden.contains(ruleset.getImplicant()) &&
                (!ruleset.isAdjacencyRuleset() ||
                 isAdjacencySatisfied(ruleset, subSpace, implicated, forbidden,
                                      adjacencies, priorRuleset))) {
                if (ruleset.isAdjacencyRuleset()) {
                    implicatedIDs.add(ruleset.getImplicant().getID() + "@" +
                                      ruleset.getIndex());
                } else {
                    implicatedIDs.add(ruleset.getImplicant().getID());
                }

                if (!implicatedIDsToSubSpaces.containsKey(implicatedIDs)) {
                    System.out.println("create");

                    SubSpace branchSpace;

                    if (ruleset.isAdjacencyRuleset()) {
                        if (adjacencies.containsKey(ruleset.getImplicant())) {
                            branchSpace = subSpace.copyByRuleset(
                                ruleset,
                                getAdjacentSequence(ruleset.getImplicant(),
                                                    adjacencies)
                                    .size());
                        } else {
                            branchSpace = subSpace.copyByRuleset(ruleset);
                        }
                    } else {
                        branchSpace =
                            subSpace.copyFromPart(ruleset.getImplicant());
                    }

                    Set<Part> recentlyImplicated = new HashSet<Part>();

                    List<Part> recentlyAdjacent = new ArrayList<Part>(2);

                    Set<Part> implied;

                    if (implicated.contains(ruleset.getImplicant())) {
                        implied = new HashSet<Part>();
                    } else {
                        Set<Part> implicants =
                            collectImplicants(ruleset, implicated, forbidden);

                        implied = collectImplied(ruleset, implicated);

                        branchSpace.addParts(implicants);

                        branchSpace.deleteParts(implied);

                        forbidden.addAll(implied);

                        implicated.add(ruleset.getImplicant());

                        if (ruleset.isAdjacencyRuleset()) {
                            recentlyImplicated.add(ruleset.getImplicant());

                            if (ruleset.isAdjacentTo(priorRuleset)) {
                                branchSpace.deletePart(ruleset.getImplicant());

                                branchSpace.deletePart(
                                    priorRuleset.getImplicant());

                                if (!adjacencies.containsKey(
                                        ruleset.getImplicant())) {
                                    adjacencies.put(ruleset.getImplicant(),
                                                    new HashSet<Part>());

                                    adjacencies.get(ruleset.getImplicant())
                                        .add(priorRuleset.getImplicant());
                                }

                                if (adjacencies.containsKey(
                                        priorRuleset.getImplicant())) {
                                    adjacencies.get(priorRuleset.getImplicant())
                                        .add(ruleset.getImplicant());
                                } else {
                                    adjacencies.put(priorRuleset.getImplicant(),
                                                    new HashSet<Part>());

                                    adjacencies.get(priorRuleset.getImplicant())
                                        .add(ruleset.getImplicant());
                                }

                                recentlyAdjacent.add(ruleset.getImplicant());

                                recentlyAdjacent.add(
                                    priorRuleset.getImplicant());
                            }
                        }
                    }

                    implicatedIDsToSubSpaces.put(copyIDs(implicatedIDs),
                                                 branchSpace);

                    System.out.println("branch");
                    branch(branchSpace, rulesets, implicatedIDs,
                           implicatedIDsToSubSpaces, implicated, forbidden,
                           adjacencies, ruleset);
                    System.out.println("back");

                    forbidden.removeAll(implied);

                    if (ruleset.isAdjacencyRuleset()) {
                        if (recentlyImplicated.contains(
                                ruleset.getImplicant())) {
                            implicated.remove(ruleset.getImplicant());
                        }

                        if (recentlyAdjacent.size() == 2) {
                            adjacencies.get(recentlyAdjacent.get(0))
                                .remove(recentlyAdjacent.get(1));

                            adjacencies.get(recentlyAdjacent.get(1))
                                .remove(recentlyAdjacent.get(0));

                            if (adjacencies.get(recentlyAdjacent.get(0))
                                    .isEmpty()) {
                                adjacencies.remove(recentlyAdjacent.get(0));
                            }

                            if (adjacencies.get(recentlyAdjacent.get(1))
                                    .isEmpty()) {
                                adjacencies.remove(recentlyAdjacent.get(1));
                            }
                        }
                    } else {
                        implicated.remove(ruleset.getImplicant());
                    }
                }

                if (ruleset.isAdjacencyRuleset()) {
                    if (adjacencies.containsKey(ruleset.getImplicant())) {
                        subSpace.connectByRuleset(
                            implicatedIDsToSubSpaces.get(implicatedIDs),
                            ruleset, getAdjacentSequence(ruleset.getImplicant(),
                                                         adjacencies));
                    } else {
                        subSpace.connectByRuleset(
                            implicatedIDsToSubSpaces.get(implicatedIDs),
                            ruleset);
                    }

                    implicatedIDs.remove(ruleset.getImplicant().getID() + "@" +
                                         ruleset.getIndex());
                } else {
                    subSpace.connectByPart(
                        implicatedIDsToSubSpaces.get(implicatedIDs),
                        ruleset.getImplicant());

                    implicatedIDs.remove(ruleset.getImplicant().getID());
                }

            } else {
                System.out.println("forbidden!");
            }

            if (ruleset.isAdjacencyRuleset()) {
                adjacencyStack.push(ruleset);

                indexStack.push(new Integer(i));

                i--;
            } else {
                rulesets.add(i, ruleset);
            }
        }

        while (!adjacencyStack.isEmpty()) {
            rulesets.add(indexStack.pop().intValue(), adjacencyStack.pop());
        }
    }

    public List<Ruleset> composeRulesets(Set<Rule> rules) {
        HashMap<Part, Ruleset> implicantToRuleset =
            new HashMap<Part, Ruleset>();

        for (Rule rule : rules) {
            if (rule.isPrecedenceRule()) {
                Part implicant = rule.getImplicant();

                if (!implicantToRuleset.containsKey(implicant)) {
                    implicantToRuleset.put(implicant, new Ruleset(implicant));
                }

                implicantToRuleset.get(implicant).addRule(rule);

                if (rule.isAdjacencyRule() ||
                    rule.isNonStrictPrecedenceRule()) {
                    Part implied = rule.getImplied();

                    if (!implicantToRuleset.containsKey(implied)) {
                        implicantToRuleset.put(implied, new Ruleset(implied));
                    }

                    implicantToRuleset.get(implied).addRule(rule);
                }
            }
        }

        return new ArrayList<Ruleset>(implicantToRuleset.values());
    }

    public void sortRulesets(List<Ruleset> rulesets, SubSpace subSpace) {
        List<Ruleset> rulesetCopies = new LinkedList<Ruleset>();

        for (Ruleset ruleset : rulesets) {
            if (ruleset.isAdjacencyRuleset()) {
                for (Integer nodeIndex :
                     subSpace.getNodeIndices(ruleset.getImplicant())) {
                    if (ruleset.hasIndex()) {
                        Ruleset rulesetCopy = ruleset.copy();

                        rulesetCopy.setIndex(nodeIndex.intValue());

                        rulesetCopies.add(rulesetCopy);
                    } else {
                        ruleset.setIndex(nodeIndex.intValue());
                    }
                }
            } else {
                List<Integer> nodeIndices =
                    subSpace.getNodeIndices(ruleset.getImplicant());

                if (nodeIndices.size() > 0) {
                    ruleset.setIndex(nodeIndices.get(0).intValue());
                }
            }
        }

        rulesets.addAll(rulesetCopies);

        Collections.sort(rulesets, new Ruleset());
    }
}
