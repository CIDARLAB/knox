package knox.spring.data.neo4j.eugene;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.eugene.Part.PartType;

public class Conversion {
	
	private DesignSpace space;
	
	private Set<Part> implicantParts;
	
	private HashMap<Set<String>, SubSpace> implicantToSubSpace;
	
	private HashMap<Set<String>, Set<String>> implicantToForbiddenPartIDs;
	
	private HashMap<Set<String>, Set<String>> implicantToSuccessorPartIDs;
	
	private HashMap<Set<String>, String> successorPartIDsToPredecessorID;
	
	private HashMap<String, Ruleset> implicantPartIDToRuleset;
	
	public Conversion(Device device, HashMap<PartType, Set<Part>> partsLibrary) {
		space = new DesignSpace(device.getID());
		
		Set<String> rootImplicant = new HashSet<String>();
		
		implicantToSubSpace = new HashMap<Set<String>,  SubSpace>();
		
		SubSpace rootSubSpace = new SubSpace(space, device, partsLibrary);
		
		implicantToSubSpace.put(rootImplicant, rootSubSpace);
		
		implicantToForbiddenPartIDs = new HashMap<Set<String>, Set<String>>();
		
		implicantToForbiddenPartIDs.put(rootImplicant, new HashSet<String>());
		
		implicantToSuccessorPartIDs = new HashMap<Set<String>, Set<String>>();
		
		implicantToSuccessorPartIDs.put(rootImplicant, new HashSet<String>());
		
		successorPartIDsToPredecessorID = new HashMap<Set<String>, String>();
    	
    	composeRulesets(device.getRules());
    	
    	for (String implicantPartID : implicantPartIDToRuleset.keySet()) {
    		System.out.println(implicantPartID);
    		for (Rule rule : implicantPartIDToRuleset.get(implicantPartID).getRules()) {
    			System.out.println(rule.getSubjectPart().getID() + " " + rule.getType().getValue() + " " + rule.getObjectPart().getID());
    		}
    	}
    	
    	for (Ruleset ruleset : implicantPartIDToRuleset.values()) {
    		for (Part implicantPart : ruleset.getImplicantParts()) {
    			rootSubSpace.deletePart(implicantPart);
    		}
    	}
	}
	
	public void applyRuleset(Set<String> implicant, SubSpace subSpace, Part implicantPart, Ruleset ruleset, 
			Set<String> forbiddenPartIDs, Set<String> adjacentPartIDs, 
			HashMap<Set<String>, String> adjacentPartIDsToConnectionID) {
		applyRuleset(implicant, subSpace, implicantPart, ruleset, forbiddenPartIDs, adjacentPartIDs, 
				adjacentPartIDsToConnectionID, -1);
	}
	
	public void applyRuleset(Set<String> implicant, SubSpace subSpace, Part implicantPart, 
			Ruleset ruleset, Set<String> forbiddenPartIDs, Set<String> successorPartIDs, 
			HashMap<Set<String>, String> successorPartIDsToPredecessorID, int connectionIndex) {
		Set<String> nextImplicant = new HashSet<String>(implicant);
		
		nextImplicant.add(implicantPart.getID());
		
		if (successorPartIDs.size() > 0) {
			nextImplicant.remove(successorPartIDsToPredecessorID.get(successorPartIDs));
		}
		
		if (ruleset.hasAdjacencyRule()) {
			nextImplicant.add(implicantPart.getID() + "@" + connectionIndex);
		}
		
		SubSpace nextSubSpace;

		if (implicantToSubSpace.containsKey(nextImplicant)) {
			nextSubSpace = implicantToSubSpace.get(nextImplicant);
		} else {
			nextSubSpace = subSpace.copyFromPart(implicantPart);

			for (Rule rule : ruleset.getRules()) {
				nextSubSpace.applyRule(rule, implicantPart);
			}

			implicantToSubSpace.put(nextImplicant, nextSubSpace);
			
			implicantToForbiddenPartIDs.put(nextImplicant, ruleset.inferForbiddenPartIDs(implicantPart));
			
			implicantToForbiddenPartIDs.get(nextImplicant).addAll(forbiddenPartIDs);
			
			implicantToSuccessorPartIDs.put(nextImplicant, ruleset.inferSuccessorPartIDs(implicantPart));
			
			if (implicantToSuccessorPartIDs.get(nextImplicant).size() > 0) {
				successorPartIDsToPredecessorID.put(implicantToSuccessorPartIDs.get(nextImplicant), 
						implicantPart.getID() + "@" + connectionIndex);
			}
		}

		if (connectionIndex >= 0) {
			subSpace.connectToSubSpace(nextSubSpace, implicantPart, connectionIndex);
		} else {
			subSpace.connectToSubSpace(nextSubSpace, implicantPart);
		}
	}
	
	public void convertNext() {
		HashMap<Set<String>, SubSpace> implicantToSubSpace = new HashMap<Set<String>, SubSpace>();
		
		HashMap<Set<String>, Set<String>> implicantToForbiddenPartIDs = new HashMap<Set<String>, Set<String>>();
		
		HashMap<Set<String>, Set<String>> implicantToSuccessorPartIDs = new HashMap<Set<String>, Set<String>>();
		
		HashMap<Set<String>, String> successorPartIDsToPredecessorID = new HashMap<Set<String>, String>();
		
		for (Set<String> implicant : this.implicantToSubSpace.keySet()) {
			implicantToSubSpace.put(implicant, this.implicantToSubSpace.get(implicant));
			
			implicantToForbiddenPartIDs.put(implicant, this.implicantToForbiddenPartIDs.get(implicant));
			
			implicantToSuccessorPartIDs.put(implicant, this.implicantToSuccessorPartIDs.get(implicant));
			
			if (successorPartIDsToPredecessorID.containsKey(implicant)) {
				successorPartIDsToPredecessorID.put(implicant, this.successorPartIDsToPredecessorID.get(implicant));
			}
		}
		
		this.implicantToSubSpace.clear();
		
		this.implicantToForbiddenPartIDs.clear();
		
		this.implicantToSuccessorPartIDs.clear();
		
		this.successorPartIDsToPredecessorID.clear();
		
		for (Set<String> implicant : implicantToSubSpace.keySet()) {
			SubSpace subSpace = implicantToSubSpace.get(implicant);

			Set<String> forbiddenPartIDs = implicantToForbiddenPartIDs.get(implicant);

			Set<String> successorPartIDs = implicantToSuccessorPartIDs.get(implicant);

			for (Part implicantPart : implicantParts) {
				if (!forbiddenPartIDs.contains(implicantPart.getID())
						&& subSpace.hasPartType(implicantPart)
						&& (!successorPartIDs.contains(implicantPart.getID()) 
								|| subSpace.beginsWithPartType(implicantPart))) {
					Ruleset ruleset = implicantPartIDToRuleset.get(implicantPart.getID());

					Ruleset applicableRuleset;

					if (implicant.contains(implicantPart.getID())) {
						applicableRuleset = ruleset.getAdjacencyRuleset();
					} else {
						applicableRuleset = ruleset.getApplicableRuleset(implicant);
					}

					if (applicableRuleset.getNumRules() > 0) {
						if (successorPartIDs.contains(implicantPart.getID())) {
							applyRuleset(implicant, subSpace, implicantPart, applicableRuleset,  
									forbiddenPartIDs, successorPartIDs, successorPartIDsToPredecessorID, 0);
						} else if (applicableRuleset.hasAdjacencyRule()) {
							int[] connectionIndices = subSpace.inferConnectionIndices(implicantPart);
							
							for (int i = 0; i < connectionIndices.length; i++) {
								applyRuleset(implicant, subSpace, implicantPart, applicableRuleset,  
										forbiddenPartIDs, successorPartIDs, successorPartIDsToPredecessorID, 
										connectionIndices[i]);
							}
						} else {
							applyRuleset(implicant, subSpace, implicantPart, applicableRuleset,  
									forbiddenPartIDs, successorPartIDs, successorPartIDsToPredecessorID);
						}
					}
				}
			}
		}
	}
	
//	public void applyRuleset(int i) {
//		appliedRules.addAll(rulesets.get(rulesetIndices[i]).getRules());
//		
//		for (Part impldPart : rulesets.get(rulesetIndices[i]).getImpliedParts()) {
//			if (forbiddenPartCounts.containsKey(impldPart.getID())) {
//				forbiddenPartCounts.put(impldPart.getID(), 
//						new Integer(forbiddenPartCounts.get(impldPart.getID()).intValue() + 1));
//			} else {
//				forbiddenPartCounts.put(impldPart.getID(), new Integer(1));
//			}
//		}
//		
//		if (subSpaces.containsKey(appliedRules)) {
//			currSubSpace = subSpaces.get(appliedRules);
//		} else {
//			SubSpace implSubSpace = currSubSpace.copyByRuleset(rulesets.get(rulesetIndices[i]));
//
//			implSubSpace.applyPrecedenceRuleset(rulesets.get(rulesetIndices[i]));
//			
//			subSpaces.put(new HashSet<Rule>(appliedRules), implSubSpace);
//			
//			currSubSpace.connectToSubSpace(implSubSpace, rulesets.get(rulesetIndices[i]));
//			
//			currSubSpace = implSubSpace;
//		}
//	}
//	
	private void composeRulesets(Set<Rule> rules) {
		implicantPartIDToRuleset = new HashMap<String, Ruleset>();
		
		implicantParts = new HashSet<Part>();
		
		for (Rule rule : rules) {
			if (rule.isPrecedenceRule()) {
				Part implicantPart = rule.getImplicantPart();
				
				if (!implicantPartIDToRuleset.containsKey(implicantPart.getID())) {
					implicantPartIDToRuleset.put(implicantPart.getID(), new Ruleset());
					
					implicantParts.add(implicantPart);
				} 
				
				implicantPartIDToRuleset.get(implicantPart.getID()).addRule(rule);
				
				if (rule.isAdjacencyRule() || rule.isNonStrictPrecedenceRule()) {
					Part impliedPart = rule.getImpliedPart();
					
					if (!implicantPartIDToRuleset.containsKey(impliedPart.getID())) {
						implicantPartIDToRuleset.put(impliedPart.getID(), new Ruleset());
						
						implicantParts.add(impliedPart);
					}
					
					implicantPartIDToRuleset.get(impliedPart.getID()).addRule(rule);
				}
			}
		}
	}
	
	public boolean isFinished() {
		return implicantToSubSpace.isEmpty();
	}
	
////    	HashMap<String, Integer> partIDToRuleIndex = new HashMap<String, Integer>();
////    	
////    	int i = 0;
////    	
////    	List<Ruleset> precedenceRulesets = new LinkedList<Ruleset>();
////
////    	for (Rule rule : rules) {
////    		if (rule.isAdjacencyRule()) {
////    			Set<Part> implcParts = rule.getImplicantParts();
////    			
////    			for (Part implcPart : implcParts) {
////    				if (!partIDToRuleIndex.containsKey(implcPart.getID())) {
////        				partIDToRuleIndex.put(implcPart.getID(), new Integer(i));
////
////        				precedenceRulesets.add(new Ruleset());
////
////        				i++;
////        			}
////
////        			int ruleIndex = partIDToRuleIndex.get(implcPart.getID()).intValue();
////
////        			precedenceRulesets.get(ruleIndex).addRule(rule);
////    			}
////    		} else if (rule.isNonStrictPrecedenceRule()) {
////    			Part impldPart = rule.getImplicantPart();
////    			
////    			if (!partIDToRuleIndex.containsKey(impldPart.getID())) {
////    				partIDToRuleIndex.put(impldPart.getID(), new Integer(i));
////
////    				precedenceRulesets.add(new Ruleset());
////
////    				i++;
////    			}
////
////    			int ruleIndex = partIDToRuleIndex.get(impldPart.getID()).intValue();
////
////    			precedenceRulesets.get(ruleIndex).addRule(rule);
////    		} else if (rule.isPrecedenceRule()) {
////    			Part implcPart = rule.getImplicantPart();
////    			
////    			if (!partIDToRuleIndex.containsKey(implcPart.getID())) {
////    				partIDToRuleIndex.put(implcPart.getID(), new Integer(i));
////
////    				precedenceRulesets.add(new Ruleset());
////
////    				i++;
////    			}
////
////    			int ruleIndex = partIDToRuleIndex.get(implcPart.getID()).intValue();
////
////    			precedenceRulesets.get(ruleIndex).addRule(rule);
////    		}
//    	}
//    	
//    	return precedenceRulesets;
//    }
//	
	public DesignSpace getSpace() {
		return space;
	}
//	
//	public void incrementRuleset(int i) {
//		rulesetIndices[i]++;
//
//		while (rulesetIndices[i] < rulesets.size()
//				&& (appliedRules.containsAll(rulesets.get(rulesetIndices[i]).getRules())
//						|| rulesets.get(rulesetIndices[i]).isApplicable(forbiddenPartCounts.keySet()))) {
//			rulesetIndices[i]++;
//		}
//	}
//	
//	public boolean isRulesetInbounds(int i) {
//		return rulesetIndices[i] < rulesets.size();
//	}
//	
//	public boolean isLastRuleset(int i) {
//		return i == rulesets.size() - 1;
//	}
//	
//	public void resetRuleset(int i) {
//		rulesetIndices[i] = -1;
//	}
//	
//	public void unApplyRuleset(int i) {
//		appliedRules.removeAll(rulesets.get(rulesetIndices[i]).getRules());
//
//		for (Part impldPart : rulesets.get(rulesetIndices[i]).getImpliedParts()) {
//			if (forbiddenPartCounts.get(impldPart.getID()).intValue() > 1) {
//				forbiddenPartCounts.put(impldPart.getID(), 
//						new Integer(forbiddenPartCounts.get(impldPart.getID()).intValue() - 1));
//			} else {
//				forbiddenPartCounts.remove(impldPart.getID());
//			}
//		}
//
//		currSubSpace = subSpaces.get(appliedRules);
//	}
}
