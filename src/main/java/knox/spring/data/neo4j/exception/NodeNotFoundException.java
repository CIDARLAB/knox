package knox.spring.data.neo4j.exception;

public class NodeNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -2722264417820334288L;

    String spaceID;
    String nodeID;

    public NodeNotFoundException(String spaceID, String nodeID) {
        this.spaceID = spaceID;
        this.nodeID = nodeID;
    }

    public String getMessage() {
        return "Node " + nodeID + " not found in design space " + spaceID + ".";
    }
}
