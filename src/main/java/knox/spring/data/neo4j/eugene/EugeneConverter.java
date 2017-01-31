package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
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
		
//		Set<Part> implicants = new HashSet<Part>();
//		
//		for (Ruleset ruleset : rulesets) {
//			implicants.add(ruleset.getImplicant());
//		}
		
		branch(rootSpace, rulesets, new HashSet<String>(), new HashMap<Set<String>, SubSpace>(), 
				new HashSet<Part>(), new HashSet<Part>(), new Ruleset());
		
		return space;
	}
	
	private Set<String> copyIDs(Set<String> ids) {
		Set<String> idCopies = new HashSet<String>();

		for (String id : ids) {
			idCopies.add(id);
		}
		
		return idCopies;
	}
	
//	private Set<Part> collectAdjacent(Ruleset ruleset, Set<Part> implicants) {
//		Set<Part> adjacent = new HashSet<Part>();
//		
//		for (Part part : ruleset.getAdjacent()) {
//			if (implicants.contains(part)) {
//				adjacent.add(part);
//			}
//		}
//		
//		return adjacent;
//	}
	
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
		for (int i = 0; i < rulesets.size(); i++) {
			if (!forbidden.contains(rulesets.get(i).getImplicant())) {
				if (rulesets.get(i).getAdjacent().size() > 0) {
					implicatedIDs.add(rulesets.get(i).getImplicant().getID() 
							+ "@" + rulesets.get(i).getRank());
				} else {
					implicatedIDs.add(rulesets.get(i).getImplicant().getID());
				}
				
				if (!implicatedIDsToSubSpaces.containsKey(implicatedIDs)) {
					Ruleset ruleset = rulesets.remove(i);
					
					SubSpace branchSpace = subSpace.copyFromPart(ruleset.getImplicant());

					implicatedIDsToSubSpaces.put(copyIDs(implicatedIDs), branchSpace);
					
					Set<Part> implicants = collectImplicants(ruleset, implicated, forbidden);
					
					Set<Part> implied = collectImplied(ruleset, implicated);
					
					branchSpace.addParts(implicants);
					
					branchSpace.deleteParts(implied);
					
					if (ruleset.getAdjacent().contains(priorRuleset.getImplicant())
							&& !ruleset.getImplicant().equals(priorRuleset.getImplicant())) {
						branchSpace.deletePart(priorRuleset.getImplicant());
						
						branchSpace.deletePart(ruleset.getImplicant());
					} else {
						forbidden.add(ruleset.getImplicant());
					}
						
					implicated.add(ruleset.getImplicant());

					forbidden.addAll(implied);
						
					branch(branchSpace, rulesets, implicatedIDs, implicatedIDsToSubSpaces, 
							implicants, forbidden, ruleset);

					forbidden.removeAll(implied);
						
					implicated.remove(ruleset.getImplicant());
					
					rulesets.add(i, ruleset);
				}

				subSpace.connectToSubSpace(implicatedIDsToSubSpaces.get(implicatedIDs), 
						rulesets.get(i).getImplicant());
				
				if (rulesets.get(i).getAdjacent().size() > 0) {
					implicatedIDs.remove(rulesets.get(i).getImplicant().getID() 
							+ "@" + rulesets.get(i).getRank());
				} else {
					implicatedIDs.remove(rulesets.get(i).getImplicant().getID());
				}
			}
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

			if (ruleset.getAdjacent().size() > 0) {
				if (nodeIndices.size() > 0) {
					for (Edge edge : subSpace.getNode(nodeIndices.get(0)).getEdges()) {
						if (edge.hasComponentID(ruleset.getImplicant().getID())) {
							ruleset.setRank(nodeIndices.get(0).intValue());
						}
					}
					
					while (i + 1 < nodeIndices.size()) {
						i++;
						
						rulesetCopies.add(0, ruleset.copy());
						
						for (Edge edge : subSpace.getNode(nodeIndices.get(i)).getEdges()) {
							if (edge.hasComponentID(ruleset.getImplicant().getID())) {
								rulesetCopies.get(0).setRank(nodeIndices.get(i).intValue());
							}
						}
					}
				}
			} else {
				while (i < nodeIndices.size() && !ruleset.hasRank()) {
					for (Edge edge : subSpace.getNode(nodeIndices.get(i)).getEdges()) {
						if (edge.hasComponentID(ruleset.getImplicant().getID())) {
							ruleset.setRank(nodeIndices.get(i).intValue());
						}
					}
					
					i++;
				}
			}
		}
		
		rulesetCopies.addAll(rulesetCopies);
		
		Collections.sort(rulesets, new Ruleset());
	}
}
