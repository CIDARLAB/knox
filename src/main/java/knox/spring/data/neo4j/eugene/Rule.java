package knox.spring.data.neo4j.eugene;

import java.util.List;

public class Rule {
	
	String constraint;

	List<String> operands;

	public Rule() {

	}

	public Rule(String constraint, List<String> operands) {
		this.constraint = constraint;

		this.operands = operands;
	}

	public String getConstraint() {
		return constraint;
	}

	public List<String> getOperands() {
		return operands;
	}
	
	public boolean isNextTo() {
		return constraint.equals(Constraint.NEXTTO.getValue());
	}
	
	public boolean isBefore() {
		return constraint.equals(Constraint.BEFORE.getValue()) 
				|| constraint.equals(Constraint.SOME_BEFORE.getValue());
	}
	
	public boolean isAfter() {
		return constraint.equals(Constraint.AFTER.getValue()) 
				|| constraint.equals(Constraint.SOME_AFTER.getValue());
	}
	
	public boolean isSome() {
		return constraint.equals(Constraint.SOME_BEFORE.getValue()) 
				|| constraint.equals(Constraint.SOME_AFTER.getValue());
	}
	
	public enum Constraint {
        BEFORE("BEFORE"),
        SOME_BEFORE("SOME_BEFORE"),
        AFTER("AFTER"),
        SOME_AFTER("SOME_AFTER"),
        NEXTTO("NEXTTO");

        private final String value;
        
        Constraint(String value) { 
        	this.value = value;
        }

        public String getValue() {
        	return value; 
        }
    }
}
