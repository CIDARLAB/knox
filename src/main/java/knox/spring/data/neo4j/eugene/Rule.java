package knox.spring.data.neo4j.eugene;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rule {
	
	String constraint;
	
	private static final Logger LOG = LoggerFactory.getLogger(Rule.class);

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
	
	public boolean hasOperands() {
		return operands != null && !operands.isEmpty();
	}
	
	public int getNumOperands() {
		if (hasOperands()) {
			return operands.size();
		} else {
			return 0;
		}
	}
	
	public boolean isStartsWith() {
		return getNumOperands() == 1 && constraint.equals(Constraint.STARTSWITH.getValue());
	}
	
	public boolean isEndsWith() {
		return getNumOperands() == 1 && constraint.equals(Constraint.ENDSWITH.getValue());
	}
	
	public boolean isEquals() {
		return getNumOperands() == 2 && isIndex(operands.get(0))
				&& constraint.equals(Constraint.EQUALS.getValue());
	}
	
	public boolean isNextTo() {
		return getNumOperands() == 2 && constraint.equals(Constraint.NEXTTO.getValue());
	}
	
	public boolean isBefore() {
		return getNumOperands() == 2 && (constraint.equals(Constraint.BEFORE.getValue()) 
				|| constraint.equals(Constraint.SOME_BEFORE.getValue()));
	}
	
	public boolean isAfter() {
		return getNumOperands() == 2 && (constraint.equals(Constraint.AFTER.getValue()) 
				|| constraint.equals(Constraint.SOME_AFTER.getValue()));
	}
	
	public boolean isSome() {
		return constraint.equals(Constraint.SOME_BEFORE.getValue()) 
				|| constraint.equals(Constraint.SOME_AFTER.getValue());
	}
	
	public static boolean isIndex(String s) {
		if (s.startsWith("[") && s.endsWith("]")) {
			return isInteger(s.substring(1, s.length() - 1));
		} else {
			return false;
		}
	}
	
	private static boolean isInteger(String s) {
	    return isInteger(s, 10);
	}

	private static boolean isInteger(String s, int radix) {
	    if(s.isEmpty()) {
	    	return false;
	    }
	    
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) {
	            	return false;
	            } else {
	            	continue;
	            }
	        }
	        
	        if(Character.digit(s.charAt(i),radix) < 0) {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	public enum Constraint {
        BEFORE("BEFORE"),
        SOME_BEFORE("SOME_BEFORE"),
        AFTER("AFTER"),
        SOME_AFTER("SOME_AFTER"),
        NEXTTO("NEXTTO"),
        STARTSWITH("STARTSWITH"),
        ENDSWITH("ENDSWITH"),
        EQUALS("EQUALS");

        private final String value;
        
        Constraint(String value) { 
        	this.value = value;
        }

        public String getValue() {
        	return value; 
        }
    }
}
