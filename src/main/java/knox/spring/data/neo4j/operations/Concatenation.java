package knox.spring.data.neo4j.operations;

import java.util.Set;

import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.Node.NodeType;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Concatenation {

	private NodeSpace concatenationSpace;
	
	public Concatenation() {
		concatenationSpace = new NodeSpace(0);
	}
	
	public void connect(NodeSpace space) {
		if (!concatenationSpace.hasNodes()) {
			concatenationSpace.union(space);
		} else {
			Set<Node> acceptNodes = concatenationSpace.getAcceptNodes();
			
			Set<Node> startNodes = concatenationSpace.getStartNodes();
			
			concatenationSpace.union(space);
			
			for (Node startNode : concatenationSpace.getStartNodes()) {
				if (!startNodes.contains(startNode)) {
					for (Node acceptNode : acceptNodes) {
						acceptNode.createEdge(startNode);
						
						acceptNode.deleteNodeType(NodeType.ACCEPT.getValue());
					}
					
					startNode.deleteNodeType(NodeType.START.getValue());
				}
			}
		}
	}
	
	public NodeSpace getConcatenationSpace() {
		return concatenationSpace;
	}
	
}
