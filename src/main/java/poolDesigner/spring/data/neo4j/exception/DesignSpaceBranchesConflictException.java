package poolDesigner.spring.data.neo4j.exception;

import java.util.Set;

public class DesignSpaceBranchesConflictException extends RuntimeException {

	private static final long serialVersionUID = 2536217673758883842L;
	
	
	Set<String> spaceIDs;
	
	Set<String> branchIDs;
	
	public DesignSpaceBranchesConflictException(Set<String> spaceIDs, Set<String> branchIDs) {
		this.spaceIDs = spaceIDs;
		
		this.branchIDs = branchIDs;
	}
	
	public String getMessage() {
		String message = "Design spaces ";
	
		for (String spaceID : spaceIDs) {
			message = message + spaceID + ", ";
		}
		
		message = message + " have branches with conflicting IDs:  ";
		
		for (String branchID : branchIDs) {
			message = message + branchID + ", ";
		}
		
		message = message.substring(0, message.length() - 2);
		
		return message;
	}

}
