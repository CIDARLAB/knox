package knox.spring.data.neo4j.eugene;

public class Rule {
	
	private RuleType type;
	
	private Part subject;
	
	private Part object;
	
	private int subjectIndex;
	
	private int objectIndex;
	
	private int count;
	
	public Rule(RuleType type) {
		this.type = type;
	}
	
	public Rule(RuleType type, Part subject) {
		this.type = type;
		
		this.subject = subject;
	}
	
	public Rule(RuleType type, int subjectIndex, Part object) {
		this.type = type;
		
		this.subjectIndex = subjectIndex;
		
		this.object = object;
	}
	
	public Rule(RuleType type, Part subject, int count) {
		this.type = type;
		
		this.subject = subject;
		
		this.count = count;
	}
	
	public Rule(RuleType type, int subjectIndex, int objectIndex) {
		this.type = type;
		
		this.subjectIndex = subjectIndex;
		
		this.objectIndex = objectIndex;
	}
	
	public Rule(RuleType type, Part subject, Part object) {
		this.type = type;
		
		this.subject = subject;
		
		this.object = object;
	}
	
	public RuleType getType() {
		return type;
	}
	
	public Part getSubject() {
		return subject;
	}
	
	public Part getObject() {
		return object;
	}
	
	public Part getImplicant() {
		if (isBeforeRule()) {
			return object;
		} else if (isAfterRule()
				|| isAdjacencyRule()) {
			return subject;
		} else {
			return null;
		}
	}
	
	public Part getImplied() {
		if (isBeforeRule()) {
			return subject;
		} else if (isAfterRule()
				|| isAdjacencyRule()) {
			return object;
		} else {
			return null;
		}
	}
	
	public int getSubjectIndex() {
		return subjectIndex;
	}
	
	public int getObjectIndex() {
		return objectIndex;
	}
	
	public int getCount() {
		return count;
	}
	
	public boolean isAdjacencyRule() {
		return type.equals(RuleType.NEXTTO)
				|| type.equals(RuleType.ALL_NEXTTO)
				|| type.equals(RuleType.SOME_NEXTTO);
	}
	
	public boolean isAfterRule() {
		return type.equals(RuleType.AFTER)
				|| type.equals(RuleType.ALL_AFTER)
				|| type.equals(RuleType.SOME_AFTER);
	}
	
	public boolean isBeforeRule() {
		return type.equals(RuleType.BEFORE)
				|| type.equals(RuleType.ALL_BEFORE)
				|| type.equals(RuleType.SOME_BEFORE);
	}
	
	public boolean isPrecedenceRule() {
		return isStrictPrecedenceRule()
				|| isNonStrictPrecedenceRule();
	}
	
	public boolean isStrictPrecedenceRule() {
		return type.equals(RuleType.BEFORE)
				|| type.equals(RuleType.ALL_BEFORE)
				|| type.equals(RuleType.AFTER)
				|| type.equals(RuleType.ALL_AFTER)
				|| type.equals(RuleType.NEXTTO)
				|| type.equals(RuleType.ALL_NEXTTO);
	}
	
	public boolean isNonStrictPrecedenceRule() {
		return type.equals(RuleType.SOME_BEFORE)
				|| type.equals(RuleType.SOME_AFTER)
				|| type.equals(RuleType.SOME_NEXTTO);
	}
	
	public enum RuleType {
		ALL_BEFORE ("ALL_BEFORE"),
		ALL_AFTER ("ALL_AFTER"),
		ALL_NEXTTO ("ALL_NEXTTO"),
    	BEFORE ("BEFORE"),
    	AFTER ("AFTER"),
    	NEXTTO ("NEXTTO"),
    	SOME_BEFORE ("SOME_BEFORE"),
    	SOME_AFTER ("SOME_AFTER"),
    	SOME_NEXTTO ("SOME_NEXTTO");
    	
    	private final String value;
    	
    	RuleType(String value) {
    		this.value = value;
    	}
    	
    	public String getValue() {
    		return value;
    	}
    }
	
}
