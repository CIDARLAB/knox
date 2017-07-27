package knox.spring.data.neo4j.operations;

import java.util.Set;

import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Concatenation {

	private NodeSpace concatenationSpace;
	
	public Concatenation() {
		concatenationSpace = new NodeSpace();
	}
	
	public void connect(NodeSpace space) {
		if (concatenationSpace.isEmpty()) {
			concatenationSpace.unionNodes(space);
		} else {
			Set<Node> acceptNodes = concatenationSpace.getAcceptNodes();
			
			Set<Node> startNodes = concatenationSpace.getStartNodes();
			
			concatenationSpace.unionNodes(space);
			
			for (Node startNode : concatenationSpace.getStartNodes()) {
				if (!startNodes.contains(startNode)) {
					for (Node acceptNode : acceptNodes) {
						acceptNode.createEdge(startNode);
						
						acceptNode.clearNodeTypes();
					}
					
					startNode.clearNodeTypes();
				}
			}
		}
	}
	
	public NodeSpace getConcatenationSpace() {
		return concatenationSpace;
	}
}
