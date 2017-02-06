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
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
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
		
//		for (Ruleset ruleset : rulesets) {
//			System.out.println(ruleset.getImplicant().getID() + " " + ruleset.getRank());
//		}
		
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
//		Set<String> tempIDs = new HashSet<String>(implicatedIDs);
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
						branchSpace = subSpace.copyFromIndex(ruleset.getIndex() + 1);
					} else {
						branchSpace = subSpace.copyFromPart(ruleset.getImplicant());
					}
					
//					System.out.println("-----------");
//					System.out.println(tempIDs.toString());
//					String n = "";
//					for (Node node : subSpace.getNodes()) {
//						n = n + ", " + node.getNodeID();
//					}
//					System.out.println(n);
//					System.out.println(implicatedIDs.toString());
//					String m = "";
//					for (Node node : branchSpace.getNodes()) {
//						m = m + ", " + node.getNodeID();
//					}
//					System.out.println(m);
//					System.out.println();

					implicatedIDsToSubSpaces.put(copyIDs(implicatedIDs), branchSpace);
					
					Set<Part> implicants = collectImplicants(ruleset, implicated, forbidden);
					
					Set<Part> implied = collectImplied(ruleset, implicated);
					
					branchSpace.addParts(implicants);
					
					branchSpace.deleteParts(implied);
						
					implicated.add(ruleset.getImplicant());

					forbidden.addAll(implied);
					
					if (ruleset.isAdjacency()) {
						forbidden.add(ruleset.getImplicant());
						
						forbidden.addAll(ruleset.getAdjacent());
					}
					
					if (ruleset.isAdjacentTo(priorRuleset)) {
						branchSpace.deletePart(priorRuleset.getImplicant());
						
						forbidden.remove(ruleset.getImplicant());
						
						forbidden.remove(priorRuleset.getImplicant());
					}
					
					System.out.println("branch");
					branch(branchSpace, rulesets, implicatedIDs, implicatedIDsToSubSpaces, 
							implicants, forbidden, ruleset);
					System.out.println("back");
					
					if (ruleset.isAdjacentTo(priorRuleset)) {
						forbidden.add(ruleset.getImplicant());
						
						forbidden.add(priorRuleset.getImplicant());
					}
					
					if (ruleset.isAdjacency()) {
						forbidden.remove(ruleset.getImplicant());
						
						forbidden.removeAll(ruleset.getAdjacent());
					}
					
					forbidden.removeAll(implied);
						
					implicated.remove(ruleset.getImplicant());
				}

				if (ruleset.isAdjacency()) {
//					System.out.println("sink " + implicatedIDs.toString());
					subSpace.connectToFirst(implicatedIDsToSubSpaces.get(implicatedIDs), 
							ruleset.getImplicant());
					
					implicatedIDs.remove(ruleset.getImplicant().getID() 
							+ "@" + ruleset.getIndex());
					
//					System.out.println("source " + implicatedIDs.toString());
				} else {
					subSpace.connectToSubSpace(implicatedIDsToSubSpaces.get(implicatedIDs), 
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
	
//	public void branch(SubSpace subSpace, List<Ruleset> rulesets, Set<String> implicatedIDs, 
//			HashMap<Set<String>, SubSpace> implicatedIDsToSubSpaces, Set<Part> implicants) {
//		for (int i = 0; i < rulesets.size(); i++) {
//			if (implicants.contains(rulesets.get(i).getImplicant())) {
//				implicatedIDs.add(rulesets.get(i).getImplicant().getID());
//				
//				if (!implicatedIDsToSubSpaces.containsKey(implicatedIDs)) {
//					Ruleset ruleset = rulesets.remove(i);
//					
//					SubSpace branchSpace = subSpace.copyFromPart(ruleset.getImplicant());
//					
//					implicatedIDsToSubSpaces.put(copyIDs(implicatedIDs), branchSpace);
//					
//					branchSpace.addPart(ruleset.getImplicant());
//					
//					implicants.remove(ruleset.getImplicant());
//					
//					for (Part part : ruleset.getCoImplicants()) {
//						if (!implicants.contains(part)) {
//							branchSpace.addPart(part);
//						}
//					}
//					
//					for (Part part : ruleset.getImplied()) {
//						branchSpace.deletePart(part);
//					}
//					
//					implicants.removeAll(ruleset.getImplied());
//					
//					Set<Part> weaklyImplied = new HashSet<Part>();
//					
//					for (Part part : ruleset.getWeaklyImplied()) {
//						if (implicants.contains(part)) {
//							branchSpace.deletePart(part);
//							
//							implicants.remove(part);
//							
//							weaklyImplied.add(part);
//						}
//					}
//					
//					branch(branchSpace, rulesets, implicatedIDs, implicatedIDsToSubSpaces, implicants);
//					
//					implicants.add(ruleset.getImplicant());
//					
//					implicants.addAll(ruleset.getImplied());
//					
//					implicants.addAll(weaklyImplied);
//					
//					rulesets.add(i, ruleset);
//				}
//				
//				subSpace.connectToSubSpace(implicatedIDsToSubSpaces.get(implicatedIDs), 
//						rulesets.get(i).getImplicant());
//				
//				implicatedIDs.remove(rulesets.get(i).getImplicant().getID());
//			}
//		}
//	}
	
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
			List<Integer> nodeIndices = subSpace.getNodeIndices(ruleset.getImplicant().getType());

			int i = 0;
			
			if (ruleset.isAdjacency()) {
				if (nodeIndices.size() > 0) {
					for (Edge edge : subSpace.getNode(nodeIndices.get(0)).getEdges()) {
						if (edge.hasComponentID(ruleset.getImplicant().getID())) {
							ruleset.setIndex(nodeIndices.get(0).intValue());
						}
					}
					
					while (i + 1 < nodeIndices.size()) {
						i++;
						
						rulesetCopies.add(ruleset.copy());
						
						for (Edge edge : subSpace.getNode(nodeIndices.get(i)).getEdges()) {
							if (edge.hasComponentID(ruleset.getImplicant().getID())) {
								rulesetCopies.get(rulesetCopies.size() - 1).setIndex(nodeIndices.get(i).intValue());
							}
						}
					}
				}
			} else {
				while (i < nodeIndices.size() && !ruleset.hasIndex()) {
					for (Edge edge : subSpace.getNode(nodeIndices.get(i)).getEdges()) {
						if (edge.hasComponentID(ruleset.getImplicant().getID())) {
							ruleset.setIndex(nodeIndices.get(i).intValue());
						}
					}
					
					i++;
				}
			}
		}
		
		rulesets.addAll(rulesetCopies);
		
		Collections.sort(rulesets, new Ruleset());
	}
}
