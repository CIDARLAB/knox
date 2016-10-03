package knox.spring.data.neo4j.eugene;

import java.util.HashSet;
import java.util.Set;

public class Ruleset {
	
	private Set<Rule> rules;
	
	public Ruleset() {
		this.rules = new HashSet<Rule>();
	}
	
	public Ruleset(Set<Rule> rules) {
		this.rules = rules;
	}
	
	public void addRule(Rule rule) {
		rules.add(rule);
	}
	
//	public boolean containsAdjacencyRules() {
//		for (Rule rule : rules) {
//			if (rule.isAdjacencyRule()) {
//				return true;
//			}
//		}
//		
//		return false;
//	}
	
	public Set<Part> getImplicantParts() {
		Set<Part> implicantParts = new HashSet<Part>();
		
		for (Rule rule : rules) {
			if (rule.isPrecedenceRule()) {
				implicantParts.add(rule.getImplicantPart());
				
				if (rule.isAdjacencyRule() || rule.isNonStrictPrecedenceRule()) {
					implicantParts.add(rule.getImpliedPart());
				}
			}
		}
		
		return implicantParts;
	}
	
	public int getNumRules() {
		return rules.size();
	}
	
	public Set<Rule> getRules() {
		return rules;
	}
	
	public Ruleset getAdjacencyRuleset() {
		Set<Rule> adjacencyRules = new HashSet<Rule>();
		
		for (Rule rule : rules) {
			if (rule.isAdjacencyRule()) {
				adjacencyRules.add(rule);
			}
		}
		
		return new Ruleset(adjacencyRules);
	}
	
	public Ruleset getApplicableRuleset(Set<String> implicant) {
		Set<Rule> applicableRules = new HashSet<Rule>();
		
		for (Rule rule : rules) {
			if (!rule.isNonStrictPrecedenceRule() 
					|| rule.isAdjacencyRule()
					|| implicant.contains(rule.getImpliedPart())) {
				applicableRules.add(rule);
			}
		}
		
		return new Ruleset(applicableRules);
	}
	
	public boolean hasAdjacencyRule() {
		for (Rule rule : rules) {
			if (rule.isAdjacencyRule()) {
				return true;
			}
		}
		
		return false;
	}
	
	public Set<String> inferSuccessorPartIDs(Part implicantPart) {
		Set<String> successorPartIDs = new HashSet<String>();
		
		for (Rule rule : rules) {
			if (rule.isAdjacencyRule()) {
				if (implicantPart.isIdenticalTo(rule.getImplicantPart())) {
					successorPartIDs.add(rule.getImpliedPart().getID());
				} else if (implicantPart.isIdenticalTo(rule.getImpliedPart())) {
					successorPartIDs.add(rule.getImplicantPart().getID());
				}
			}
		}
		
		return successorPartIDs;
	}
	
	public Set<String> inferForbiddenPartIDs(Part implicantPart) {
		Set<String> forbiddenPartIDs = new HashSet<String>();
		
		for (Rule rule : rules) {
			if (rule.isNonStrictPrecedenceRule()) {
				if (implicantPart.isIdenticalTo(rule.getImplicantPart())) {
					forbiddenPartIDs.add(rule.getImpliedPart().getID());
				}
			} else if (rule.isStrictPrecedenceRule()) {
				forbiddenPartIDs.add(rule.getImpliedPart().getID());
			}
		}
		
		return forbiddenPartIDs;
	}
}
