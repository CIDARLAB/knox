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
				
				startNode.deleteNodeType(NodeType.START.getValue());
			}
			
			for (Node acceptNode : acceptNodes) {
				acceptNode.createEdge(primaryAcceptNode);
				
				acceptNode.deleteNodeType(NodeType.ACCEPT.getValue());
			}
		}
	}
	
	public NodeSpace getStarSpace() {
		return starSpace;
	}
	
}
