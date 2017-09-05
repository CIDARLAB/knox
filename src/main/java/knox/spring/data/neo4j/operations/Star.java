package knox.spring.data.neo4j.operations;

import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Node.NodeType;

public class Star {
	
	private NodeSpace starSpace;
	
	public Star(List<NodeSpace> spaces) {
		this.starSpace = new NodeSpace(0);
		
		Concatenation concat = new Concatenation();
		
		for (NodeSpace space : spaces) {
			concat.apply(space);
		}
		
		starSpace.shallowCopyNodeSpace(concat.getConcatenationSpace());
	}
	
	public void connect(boolean isOptional) {
		Set<Node> startNodes = starSpace.getStartNodes();
		
		Set<Node> acceptNodes = starSpace.getAcceptNodes();
		
		acceptNodes.removeAll(startNodes);
		
		for (Node acceptNode : acceptNodes) {
			acceptNode.unionEdges(startNodes);
		}
		
		if (isOptional) {
			for (Node startNode : startNodes) {
				startNode.addAcceptNodeType();
			}
		}
	}
	
	public NodeSpace getStarSpace() {
		return starSpace;
	}
	
}
