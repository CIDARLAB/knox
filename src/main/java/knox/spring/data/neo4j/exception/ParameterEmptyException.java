package knox.spring.data.neo4j.exception;

public class ParameterEmptyException extends RuntimeException {

	private static final long serialVersionUID = 462532991641214656L;
	String parameterID;
	
	public ParameterEmptyException(String parameterID) {
		this.parameterID = parameterID;
	}
	
	public String getMessage() {
		return "Parameter " + parameterID + " is empty.";
	}
	
}
