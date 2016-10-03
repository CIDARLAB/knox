package knox.spring.data.neo4j.eugene;

public class Rule {
	
	private RuleType type;
	
	private Part subjectPart;
	
	private Part objectPart;
	
	private int subjectIndex;
	
	private int objectIndex;
	
	private int count;
	
	public Rule(RuleType type) {
		this.type = type;
	}
	
	public Rule(RuleType type, Part subjectPart) {
		this.type = type;
		
		this.subjectPart = subjectPart;
	}
	
	public Rule(RuleType type, int subjectIndex, Part objectPart) {
		this.type = type;
		
		this.subjectIndex = subjectIndex;
		
		this.objectPart = objectPart;
	}
	
	public Rule(RuleType type, Part subjectPart, int count) {
		this.type = type;
		
		this.subjectPart = subjectPart;
		
		this.count = count;
	}
	
	public Rule(RuleType type, int subjectIndex, int objectIndex) {
		this.type = type;
		
		this.subjectIndex = subjectIndex;
		
		this.objectIndex = objectIndex;
	}
	
	public Rule(RuleType type, Part subjectPart, Part objectPart) {
		this.type = type;
		
		this.subjectPart = subjectPart;
		
		this.objectPart = objectPart;
	}
	
	public RuleType getType() {
		return type;
	}
	
	public Part getSubjectPart() {
		return subjectPart;
	}
	
	public Part getObjectPart() {
		return objectPart;
	}
	
//	public Set<Part> getImplicantParts() {
//		Set<Part> implcParts = new HashSet<Part>();
//		
//		if (isAdjacencyRule()) {
//			implcParts.add(subjectPart);
//			implcParts.add(objectPart);
//		} else if (isPrecedenceRule()) {
//			implcParts.add(getImplicantPart());
//		}
//		
//		return implcParts;
//	}
	
//	public Set<Part> getImpliedParts() {
//		Set<Part> impldParts = new HashSet<Part>();
//		
//		if (isAdjacencyRule()) {
//			impldParts.add(subjectPart);
//			impldParts.add(objectPart);
//		} else if (isPrecedenceRule()) {
//			impldParts.add(getImpliedPart());
//		}
//		
//		return impldParts;
//	}
	
	public Part getImplicantPart() {
		if (type.equals(RuleType.BEFORE)
				|| type.equals(RuleType.ALL_BEFORE)
				|| type.equals(RuleType.SOME_BEFORE)) {
			return objectPart;
		} else if (type.equals(RuleType.AFTER)
				|| type.equals(RuleType.ALL_AFTER)
				|| type.equals(RuleType.SOME_AFTER)
				|| isAdjacencyRule()) {
			return subjectPart;
		} else {
			return null;
		}
	}
	
	public Part getImpliedPart() {
		if (type.equals(RuleType.BEFORE)
				|| type.equals(RuleType.ALL_BEFORE)
				|| type.equals(RuleType.SOME_BEFORE)) {
			return subjectPart;
		} else if (type.equals(RuleType.AFTER)
				|| type.equals(RuleType.ALL_AFTER)
				|| type.equals(RuleType.SOME_AFTER)
				|| isAdjacencyRule()) {
			return objectPart;
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
