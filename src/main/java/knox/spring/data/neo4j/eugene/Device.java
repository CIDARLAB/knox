package knox.spring.data.neo4j.eugene;

import java.util.List;
import java.util.Set;

public class Device {
	
	private String id;
	
	private List<Part> parts;
	
	private Set<Rule> rules;
	
	public Device() {
		
	}
	
	public Device(String id, List<Part> parts) {
		this.id = id;
		
		this.parts = parts;
	}
	
	public Device(String id, List<Part> parts, Set<Rule> rules) {
		this.id = id;
		
		this.parts = parts;
		
		this.rules = rules;
	}
	
	public String getID() {
		return id;
	}
	
	public Part getLastPart() {
		if (hasParts()) {
			return parts.get(getNumParts() - 1);
		} else {
			return null;
		}
	}
	
	public int getNumParts() {
		return parts.size();
	}
	
	public Part getPart(int index) {
		if (hasParts() && index >= 0 && index < parts.size()) {
			return parts.get(index);
		} else {
			return null;
		}
	}
	
	public List<Part> getParts() {
		return parts;
	}
	
	public Set<Rule> getRules() {
		return rules;
	}

	public boolean hasParts() {
		return parts != null && parts.size() > 0;
	}
}
