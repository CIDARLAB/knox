package knox.spring.data.neo4j.eugene;

import java.util.Comparator;

public class Part {
	
	private String id;
	
	private PartType type;
	
	private int index;
	
	public Part(PartType type) {
		this.type = type;
	}
	
	public Part(String id, PartType type) {
		this.id = id;
		
		this.type = type;
	}
	
	public String getID() {
		return id;
	}
	
	public PartType getType() {
		return type;
	}
	
	public boolean hasID() {
		if (id == null || id.length() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean isIdenticalTo(Part part) {
		return hasID() && part.hasID() && id.equals(part.getID());
	}
	
	public enum PartType {
    	PROMOTER ("promoter"),
    	RBS ("ribosome_entry_site"),
    	CDS ("CDS"),
    	TERMINATOR ("terminator");
    	
    	private final String value;
    	
    	PartType(String value) {
    		this.value = value;
    	}
    	
    	public String getValue() {
    		return value;
    	}
    }
	
}
