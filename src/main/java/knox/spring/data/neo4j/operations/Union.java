package knox.spring.data.neo4j.operations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Node.NodeType;

public class Union {
	
	private NodeSpace unionSpace;
	
	public Union(NodeSpace unionSpace) {
		this.unionSpace = unionSpace;
	}
	
	public Union(List<NodeSpace> spaces) {
		unionSpace = new NodeSpace(0);
		
		for (NodeSpace space : spaces) {
    		unionSpace.union(space);
    	}
	}
	
	public void apply() {
		Set<Node> startNodes = unionSpace.getStartNodes();
		
		Set<Node> primaryStartNodes = new HashSet<Node>();
		
		primaryStartNodes.add(unionSpace.createStartNode());
		
		unionSpace.concatenateNodes(primaryStartNodes, startNodes);
	}
	
	public NodeSpace getUnionSpace() {
		return unionSpace;
	}
	
}
