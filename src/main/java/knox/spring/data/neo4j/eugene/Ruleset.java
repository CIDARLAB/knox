package knox.spring.data.neo4j.eugene;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class Ruleset implements Comparator<Ruleset> {
	
	private Part implicant;
	
	private Set<Part> adjacent = new HashSet<Part>();
	
	private Set<Part> coImplicants = new HashSet<Part>();
	
	private Set<Part> implied = new HashSet<Part>();
	
	private Set<Part> weaklyImplied = new HashSet<Part>();
	
	private int rank = -1;
	
	public Ruleset() {
		adjacent = new HashSet<Part>();
	}
	
	public Ruleset(Part implicant) {
		this.implicant = implicant;
		
		adjacent = new HashSet<Part>();
		
		coImplicants = new HashSet<Part>();
		
		implied = new HashSet<Part>();
		
		weaklyImplied = new HashSet<Part>();
	}
	
	public Ruleset(Part implicant, Set<Part> coImplicants, Set<Part> implied, Set<Part> weaklyImplied, 
			Set<Part> adjacent) {
		this.implicant = implicant;
		
		this.coImplicants = coImplicants;
		
		this.implied = implied;
		
		this.weaklyImplied = weaklyImplied;
		
		this.adjacent = adjacent;
	}
	
	public void addRule(Rule rule) {
		if (rule.isAdjacencyRule()) {
			if (implicant.isIdenticalTo(rule.getImplicant())) {
				adjacent.add(rule.getImplied());
			} else if (implicant.isIdenticalTo(rule.getImplied())) {
				adjacent.add(rule.getImplicant());
			}
		} else if (rule.isNonStrictPrecedenceRule()) {
			if (implicant.isIdenticalTo(rule.getImplicant())) {
				weaklyImplied.add(rule.getImplied());
			} else if (implicant.isIdenticalTo(rule.getImplied())) {
				coImplicants.add(rule.getImplicant());
			}
		} else if (rule.isStrictPrecedenceRule()) {
			implied.add(rule.getImplied());
		}
	}
	
	public int compareTo(Ruleset r) {
		return this.compareTo(r);
	}

	public int compare(Ruleset r1, Ruleset r2) {
		return r1.rank - r2.rank;
	}
	
	public Ruleset copy() {
		Set<Part> adjacent = new HashSet<Part>();
		
		adjacent.addAll(this.adjacent);
		
		Set<Part> coImplicants = new HashSet<Part>();
		
		coImplicants.addAll(this.coImplicants);
		
		Set<Part> implied = new HashSet<Part>();
		
		implied.addAll(this.implied);
		
		Set<Part> weaklyImplied = new HashSet<Part>();
		
		weaklyImplied.addAll(this.weaklyImplied);
		
		return new Ruleset(implicant, coImplicants, implied, weaklyImplied, adjacent);
	}
	
	public Set<Part> getAdjacent() {
		return adjacent;
	}
	
	public Set<Part> getImplied() {
		return implied;
	}
	
	public Set<Part> getCoImplicants() {
		return coImplicants;
	}
	
	public Part getImplicant() {
		return implicant;
	}
	
	public Set<Part> getWeaklyImplied() {
		return weaklyImplied;
	}
	
	public int getRank() {
		return rank;
	}
	
	public boolean hasRank() {
		return rank >= 0;
	}
	
	public void setRank(int rank) {
		this.rank = rank;
	}
}
