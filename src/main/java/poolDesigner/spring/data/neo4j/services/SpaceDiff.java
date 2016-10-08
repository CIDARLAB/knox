package poolDesigner.spring.data.neo4j.services;

import java.util.Set;

import poolDesigner.spring.data.neo4j.domain.Edge;
import poolDesigner.spring.data.neo4j.domain.Node;

public class SpaceDiff {
    Set<Edge> diffEdges;
    
    Set<Node> diffNodes;

    public SpaceDiff(Set<Edge> diffEdges, Set<Node> diffNodes) {
    	this.diffEdges = diffEdges;
    	
    	this.diffNodes = diffNodes;
    }
    
    public Set<Edge> getEdges() {
    	return diffEdges;
    }
    
    public Set<Node> getNodes() {
    	return diffNodes;
    }
}