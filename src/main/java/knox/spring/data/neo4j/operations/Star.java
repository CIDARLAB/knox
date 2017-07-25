package knox.spring.data.neo4j.operations;

import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Star {
private NodeSpace starSpace;
	
	public Star(List<NodeSpace> spaces) {
		this.starSpace = new NodeSpace();
		
		Concatenation concat = new Concatenation();
		
		for (NodeSpace space : spaces) {
			concat.connect(space);
		}
		
		starSpace.shallowCopyNodeSpace(concat.getConcatenationSpace());
	}
	
	public void connect(boolean isOptional) {
		Set<Node> startNodes = starSpace.getStartNodes();
		
		Set<Node> acceptNodes = starSpace.getAcceptNodes();
		
		for (Node acceptNode : acceptNodes) {
			for (Node startNode : startNodes) {
				acceptNode.createEdge(startNode);
			}
		}
		
		if (isOptional) {
			Node primaryStartNode = starSpace.createStartNode();
			
			Node primaryAcceptNode = starSpace.createAcceptNode();
			
			primaryStartNode.createEdge(primaryAcceptNode);
			
			for (Node startNode : startNodes) {
				primaryStartNode.createEdge(startNode);
				
				startNode.clearNodeType();
			}
			
			for (Node acceptNode : acceptNodes) {
				acceptNode.createEdge(primaryAcceptNode);
				
				acceptNode.clearNodeType();
			}
		}
	}
	
	public NodeSpace getStarSpace() {
		return starSpace;
	}
}
