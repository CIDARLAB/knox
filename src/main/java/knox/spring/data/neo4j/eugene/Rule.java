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
	
	public int getSubjectIndex() {
		return subjectIndex;
	}
	
	public int getObjectIndex() {
		return objectIndex;
	}
	
	public int getCount() {
		return count;
	}
	
	public enum RuleType {
    	BEFORE ("BEFORE");
    	
    	private final String value;
    	
    	RuleType(String value) {
    		this.value = value;
    	}
    	
    	public String getValue() {
    		return value;
    	}
    }
	
}
