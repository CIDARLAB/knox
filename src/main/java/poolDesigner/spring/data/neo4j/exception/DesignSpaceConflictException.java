package poolDesigner.spring.data.neo4j.exception;

public class DesignSpaceConflictException extends RuntimeException {
	
	private static final long serialVersionUID = 1792637265594949562L;
	
	String spaceID;
	
	public DesignSpaceConflictException(String spaceID) {
		this.spaceID = spaceID;
	}
	
	public String getMessage() {
		return "Design space with ID " + spaceID + " already exists.";
	}
}
