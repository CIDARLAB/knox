package knox.spring.data.neo4j.exception;

public class SBOLException extends RuntimeException{
    private static final long serialVersionUID = 462532991641214656L;

    private String message;

    public SBOLException(String message) {
        this.message = message;
    }
}
