package knox.spring.data.neo4j.operations;

import java.util.List;

import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Union {
	private NodeSpace unionSpace;
	
	public Union(NodeSpace unionSpace) {
		this.unionSpace = unionSpace;
	}
	
	public Union(List<NodeSpace> spaces) {
		unionSpace = new NodeSpace();
		
		for (NodeSpace space : spaces) {
    		unionSpace.unionNodes(space);
    	}
	}
	
	public void connect(boolean isClosed) {
		Node primaryStartNode = unionSpace.createStartNode();
    	
    	for (Node startNode : unionSpace.getStartNodes()) {
    		if (!primaryStartNode.isIdenticalTo(startNode)) {
    			primaryStartNode.createEdge(startNode);
    			
    			startNode.clearNodeTypes();
    		}
    	}
    	
    	if (isClosed) {
    		Node primaryAcceptNode = unionSpace.createAcceptNode();
    		
    		for (Node acceptNode : unionSpace.getAcceptNodes()) {
    			if (!primaryAcceptNode.isIdenticalTo(acceptNode)) {
    				acceptNode.createEdge(primaryAcceptNode);
    				
    				acceptNode.clearNodeTypes();
    			}
    		}
    	}
	}
	
	public NodeSpace getUnionSpace() {
		return unionSpace;
	}
}
