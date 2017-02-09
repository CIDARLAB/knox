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
		
		HashMap<Set<String>, SubSpace> blah = new HashMap<Set<String>, SubSpace>();
		
		branch(rootSpace, rulesets, new HashSet<String>(), blah, 
				new HashSet<Part>(), new HashSet<Part>(), new Ruleset());
		
		System.out.println("*********");
		for (Set<String> boo : blah.keySet()) {
			System.out.println("-------");
			System.out.println(boo.toString() + " " + blah.get(boo).getNumNodes());
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
	
	private Set<Part> collectImplicants(Ruleset ruleset, Set<Part> implicated, Set<Part> forbidden) {
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
	
	public void branch(SubSpace subSpace, List<Ruleset> rulesets, Set<String> implicatedIDs, 
			HashMap<Set<String>, SubSpace> implicatedIDsToSubSpaces, Set<Part> implicated,
			Set<Part> forbidden, Ruleset priorRuleset) {
		System.out.println("++++++++++");
		System.out.println(implicatedIDs.toString());
		
		Stack<Ruleset> adjacencyStack = new Stack<Ruleset>();
		
		Stack<Integer> indexStack = new Stack<Integer>();
		
		for (int i = 0; i < rulesets.size(); i++) {
			Ruleset ruleset = rulesets.remove(i);
			
			System.out.println("---------");
			System.out.println(ruleset.getImplicant().getID() + " " + ruleset.getIndex());
			
			if (!forbidden.contains(ruleset.getImplicant())
					|| ruleset.isAdjacentTo(priorRuleset)) {
				if (ruleset.isAdjacency()) {
					implicatedIDs.add(ruleset.getImplicant().getID() 
							+ "@" + ruleset.getIndex());
				} else {
					implicatedIDs.add(ruleset.getImplicant().getID());
				}
				
				if (!implicatedIDsToSubSpaces.containsKey(implicatedIDs)) {
					System.out.println("create");
					
					SubSpace branchSpace;
					
					if (ruleset.isAdjacency()) {
						branchSpace = subSpace.copyByRuleset(ruleset);
					} else {
						branchSpace = subSpace.copyFromPart(ruleset.getImplicant());
					}
					
					implicatedIDsToSubSpaces.put(copyIDs(implicatedIDs), branchSpace);
					
					Set<Part> implicants = collectImplicants(ruleset, implicated, forbidden);
					
					Set<Part> implied = collectImplied(ruleset, implicated);
					
					branchSpace.addParts(implicants);
					
					branchSpace.deleteParts(implied);
						
					implicated.add(ruleset.getImplicant());

					forbidden.addAll(implied);
					
					if (ruleset.isAdjacentTo(priorRuleset)) {
						if (ruleset.isStrongAdjacency()) {
							branchSpace.deletePart(priorRuleset.getImplicant());
							
							forbidden.remove(ruleset.getImplicant());
							
							forbidden.remove(priorRuleset.getImplicant());
						} else {
							branchSpace.addPart(ruleset.getImplicant());
						}
					} else if (ruleset.isAdjacency()) {
						forbidden.add(ruleset.getImplicant());
						
						forbidden.addAll(ruleset.getAdjacent());
					}
					
					System.out.println("branch");
					branch(branchSpace, rulesets, implicatedIDs, implicatedIDsToSubSpaces, 
							implicants, forbidden, ruleset);
					System.out.println("back");
					
					if (ruleset.isAdjacentTo(priorRuleset)) {
						if (ruleset.isStrongAdjacency()) {
							forbidden.add(ruleset.getImplicant());

							forbidden.add(priorRuleset.getImplicant());
						}
					} else if (ruleset.isAdjacency()) {
						forbidden.remove(ruleset.getImplicant());
						
						forbidden.removeAll(ruleset.getAdjacent());
					}
					
					forbidden.removeAll(implied);
						
					implicated.remove(ruleset.getImplicant());
				}
				
				if (ruleset.isAdjacency()) {
					subSpace.connectByRuleset(implicatedIDsToSubSpaces.get(implicatedIDs), 
							ruleset);
					
					implicatedIDs.remove(ruleset.getImplicant().getID() 
							+ "@" + ruleset.getIndex());
				} else {
					subSpace.connectByPart(implicatedIDsToSubSpaces.get(implicatedIDs), 
							ruleset.getImplicant());
					
					implicatedIDs.remove(ruleset.getImplicant().getID());
				}
				
				
			} else {
				System.out.println("forbidden!");
			}
			
			if (ruleset.isAdjacency()) {
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
		HashMap<Part, Ruleset> implicantToRuleset = new HashMap<Part, Ruleset>();

		for (Rule rule : rules) {
			if (rule.isPrecedenceRule()) {
				Part implicant = rule.getImplicant();

				if (!implicantToRuleset.containsKey(implicant)) {
					implicantToRuleset.put(implicant, new Ruleset(implicant));
				} 

				implicantToRuleset.get(implicant).addRule(rule);

				if (rule.isAdjacencyRule() || rule.isNonStrictPrecedenceRule()) {
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
			if (ruleset.isAdjacency()) {
				for (Integer nodeIndex : subSpace.getNodeIndices(ruleset.getImplicant())) {
					if (ruleset.hasIndex()) {
						Ruleset rulesetCopy = ruleset.copy();

						rulesetCopy.setIndex(nodeIndex);

						rulesetCopies.add(rulesetCopy);
					} else {
						ruleset.setIndex(nodeIndex);
					}
				}
			} else {
				List<Integer> nodeIndices = subSpace.getNodeIndices(ruleset.getImplicant());
				
				if (nodeIndices.size() > 0) {
					ruleset.setIndex(nodeIndices.get(0).intValue());
				}
			}
		}
		
		rulesets.addAll(rulesetCopies);
		
		Collections.sort(rulesets, new Ruleset());
	}
}
