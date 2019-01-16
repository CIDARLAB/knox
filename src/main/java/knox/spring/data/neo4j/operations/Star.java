package knox.spring.data.neo4j.operations;

import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Star {
	
	private NodeSpace starSpace;
	
	public Star(List<NodeSpace> spaces) {
		Concatenation concat = new Concatenation();
		
		for (NodeSpace space : spaces) {
			concat.apply(space);
		}
		
		starSpace = concat.getSpace().shallowCopy();
	}
	
	public void apply(boolean isOptional) {
		if (starSpace.hasNodes()) {
			Node startNode = starSpace.getStartNode();

			Set<Node> acceptNodes = starSpace.getAcceptNodes();

			for (Node acceptNode : acceptNodes) {
				acceptNode.createEdge(startNode);
			}
			
			startNode.deleteStartNodeType();
			
			Node primaryStartNode = starSpace.createStartNode();
			
			primaryStartNode.createEdge(startNode);

			if (isOptional) {
				for (Node acceptNode : acceptNodes) {
					acceptNode.deleteAcceptNodeType();
				}

				Node primaryAcceptNode = starSpace.createAcceptNode();

				for (Node acceptNode : acceptNodes) {
					acceptNode.createEdge(primaryAcceptNode);
				}
				
				primaryStartNode.createEdge(primaryAcceptNode);
			}
		}
	}
	
	public NodeSpace getSpace() {
		return starSpace;
	}
	
}
