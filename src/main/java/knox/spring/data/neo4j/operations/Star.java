package knox.spring.data.neo4j.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Star {
	
	private NodeSpace starSpace;
	
	public Star(List<NodeSpace> spaces) {
		Concatenation concat = new Concatenation();
		
		Set<Edge> blankEdges = new HashSet<Edge>();
		
		for (NodeSpace space : spaces) {
			blankEdges.addAll(concat.apply(space));
		}
		
		concat.getSpace().deleteBlankEdges(blankEdges);
		
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
