package knox.spring.data.neo4j.eugene;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class Ruleset implements Comparator<Ruleset> {
    private Part implicant;

    private Set<Part> adjacent = new HashSet<Part>();

    private Set<Part> weaklyAdjacent = new HashSet<Part>();

    private Set<Part> coImplicants = new HashSet<Part>();

    private Set<Part> implied = new HashSet<Part>();

    private Set<Part> weaklyImplied = new HashSet<Part>();

    private int index;

    public Ruleset() { adjacent = new HashSet<Part>(); }

    public Ruleset(Part implicant) {
        this.implicant = implicant;

        coImplicants = new HashSet<Part>();

        implied = new HashSet<Part>();

        weaklyImplied = new HashSet<Part>();

        adjacent = new HashSet<Part>();

        weaklyAdjacent = new HashSet<Part>();

        index = -1;
    }

    public Ruleset(Part implicant, Set<Part> coImplicants, Set<Part> implied,
                   Set<Part> weaklyImplied, Set<Part> adjacent,
                   Set<Part> weaklyAdjacent) {
        this.implicant = implicant;

        this.coImplicants = coImplicants;

        this.implied = implied;

        this.weaklyImplied = weaklyImplied;

        this.adjacent = adjacent;

        weaklyAdjacent = new HashSet<Part>();
    }

    public void addRule(Rule rule) {
        if (rule.isAdjacencyRule()) {
            if (rule.isStrictPrecedenceRule()) {
                if (implicant.isIdenticalTo(rule.getImplicant())) {
                    adjacent.add(rule.getImplied());
                } else if (implicant.isIdenticalTo(rule.getImplied())) {
                    adjacent.add(rule.getImplicant());
                }
            } else {
                if (implicant.isIdenticalTo(rule.getImplicant())) {
                    weaklyAdjacent.add(rule.getImplied());
                } else if (implicant.isIdenticalTo(rule.getImplied())) {
                    weaklyAdjacent.add(rule.getImplicant());
                }
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

    public int compareTo(Ruleset r) { return this.compareTo(r); }

    public int compare(Ruleset r1, Ruleset r2) {
        return r1.getIndex() - r2.getIndex();
    }

    public Ruleset copy() {
        //		Set<Part> adjacent = new HashSet<Part>();
        //
        //		adjacent.addAll(this.adjacent);
        //
        //		Set<Part> weaklyAdjacent = new HashSet<Part>();
        //
        //		weaklyAdjacent.addAll(this.weaklyAdjacent);
        //
        //		Set<Part> coImplicants = new HashSet<Part>();
        //
        //		coImplicants.addAll(this.coImplicants);
        //
        //		Set<Part> implied = new HashSet<Part>();
        //
        //		implied.addAll(this.implied);
        //
        //		Set<Part> weaklyImplied = new HashSet<Part>();
        //
        //		weaklyImplied.addAll(this.weaklyImplied);

        return new Ruleset(implicant, coImplicants, implied, weaklyImplied,
                           adjacent, weaklyAdjacent);
    }

    public Set<Part> getAdjacent() { return adjacent; }

    public Set<Part> getWeaklyAdjacent() { return weaklyAdjacent; }

    public Set<Part> getImplied() { return implied; }

    public Set<Part> getCoImplicants() { return coImplicants; }

    public Part getImplicant() { return implicant; }

    public boolean isAdjacencyRuleset() {
        return adjacent.size() > 0 || weaklyAdjacent.size() > 0;
    }

    public boolean isStrongAdjacencyRuleset() { return adjacent.size() > 0; }

    public boolean isAdjacentTo(Ruleset ruleset) {
        return adjacent.contains(ruleset.getImplicant()) &&
            ruleset.getAdjacent().contains(implicant) && hasIndex() &&
            ruleset.hasIndex() && (ruleset.getIndex() == index + 1 ||
                                   ruleset.getIndex() + 1 == index);
    }

    public boolean isWeaklyAdjacentTo(Ruleset ruleset) {
        return weaklyAdjacent.contains(ruleset.getImplicant()) &&
            ruleset.getWeaklyAdjacent().contains(implicant) && hasIndex() &&
            ruleset.hasIndex() && (ruleset.getIndex() == index + 1 ||
                                   ruleset.getIndex() + 1 == index);
    }

    public Set<Part> getWeaklyImplied() { return weaklyImplied; }

    public int getIndex() { return index; }

    public void setIndex(int index) { this.index = index; }

    public boolean hasIndex() { return index >= 0; }
}
