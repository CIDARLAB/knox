package knox.spring.data.neo4j.exception;

public class DesignSpaceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 462532991641214656L;
    String spaceID;

    public DesignSpaceNotFoundException(String spaceID) {
        this.spaceID = spaceID;
    }

    public String getMessage() {
        return "Design space " + spaceID + " not found.";
    }
}
