package knox.spring.data.neo4j.operations;

import java.util.HashSet;
import java.util.Set;

import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Concatenation {

	private NodeSpace concatenationSpace;
	
	public Concatenation() {
		concatenationSpace = new NodeSpace(0);
	}
	
	public void apply(NodeSpace space) {
		if (!concatenationSpace.hasNodes()) {
			concatenationSpace.union(space);
		} else {
			Set<Node> acceptNodes = concatenationSpace.getAcceptNodes();
			
			Set<Node> originalStartNodes = concatenationSpace.getStartNodes();
			
			concatenationSpace.union(space);
			
			Set<Node> startNodes = new HashSet<Node>();
			
			for (Node startNode : concatenationSpace.getStartNodes()) {
				if (!originalStartNodes.contains(startNode)) {
					startNodes.add(startNode);
				}
			}
			
			concatenationSpace.concatenateNodes(acceptNodes, startNodes);
		}
	}
	
	public NodeSpace getConcatenationSpace() {
		return concatenationSpace;
	}
	
}
