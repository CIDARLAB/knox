package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
		
		Set<Part> implicants = new HashSet<Part>();
		
		for (Ruleset ruleset : rulesets) {
			implicants.add(ruleset.getImplicant());
		}
		
		branch(rootSpace, rulesets, new HashSet<String>(), new HashMap<Set<String>, SubSpace>(), 
				implicants);
		
		return space;
	}
	
	private Set<String> copyIDs(Set<String> ids) {
		Set<String> idCopies = new HashSet<String>();

		for (String id : ids) {
			idCopies.add(id);
		}
		
		return idCopies;
	}
	
	public void branch(SubSpace subSpace, List<Ruleset> rulesets, Set<String> implicatedIDs, 
			HashMap<Set<String>, SubSpace> implicatedIDsToSubSpaces, Set<Part> implicants) {
		for (int i = 0; i < rulesets.size(); i++) {
			if (implicants.contains(rulesets.get(i).getImplicant())) {
				implicatedIDs.add(rulesets.get(i).getImplicant().getID());
				
				if (!implicatedIDsToSubSpaces.containsKey(implicatedIDs)) {
					Ruleset ruleset = rulesets.remove(i);
					
					SubSpace branchSpace = subSpace.copyFromPart(ruleset.getImplicant());
					
					implicatedIDsToSubSpaces.put(copyIDs(implicatedIDs), branchSpace);
					
					branchSpace.addPart(ruleset.getImplicant());
					
					implicants.remove(ruleset.getImplicant());
					
					for (Part part : ruleset.getCoImplicants()) {
						if (!implicants.contains(part)) {
							branchSpace.addPart(part);
						}
					}
					
					for (Part part : ruleset.getImplied()) {
						branchSpace.deletePart(part);
					}
					
					implicants.removeAll(ruleset.getImplied());
					
					Set<Part> weaklyImplied = new HashSet<Part>();
					
					for (Part part : ruleset.getWeaklyImplied()) {
						if (implicants.contains(part)) {
							branchSpace.deletePart(part);
							
							implicants.remove(part);
							
							weaklyImplied.add(part);
						}
					}
					
					branch(branchSpace, rulesets, implicatedIDs, implicatedIDsToSubSpaces, implicants);
					
					implicants.add(ruleset.getImplicant());
					
					implicants.addAll(ruleset.getImplied());
					
					implicants.addAll(weaklyImplied);
					
					rulesets.add(i, ruleset);
				}
				
				subSpace.connectToSubSpace(implicatedIDsToSubSpaces.get(implicatedIDs), 
						rulesets.get(i).getImplicant());
				
				implicatedIDs.remove(rulesets.get(i).getImplicant().getID());
			}
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
		for (Ruleset ruleset : rulesets) {
			List<Integer> nodeIndices = subSpace.getNodeIndices(ruleset.getImplicant().getType());

			int i = 0;

			while (i < nodeIndices.size() && !ruleset.hasRank()) {
				for (Edge edge : subSpace.getNode(nodeIndices.get(i)).getEdges()) {
					if (edge.hasComponentID(ruleset.getImplicant().getID())) {
						ruleset.setRank(nodeIndices.get(i).intValue());
					}
				}
			}
		}
		
		Collections.sort(rulesets, new Ruleset());
	}
}
