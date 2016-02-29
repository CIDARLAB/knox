package knox.spring.data.neo4j.exception;

public class DesignSpaceBranchesConflictException extends RuntimeException {

	private static final long serialVersionUID = 2536217673758883842L;
	
	String spaceID1;
	String spaceID2;
	
	public DesignSpaceBranchesConflictException(String spaceID1, String spaceID2) {
		this.spaceID1 = spaceID1;
		this.spaceID2 = spaceID2;
	}
	
	public String getMessage() {
		return "Design space " + spaceID1 + " and design space " + spaceID2 
				+ " have branches with conflicting IDs.";
	}

}
