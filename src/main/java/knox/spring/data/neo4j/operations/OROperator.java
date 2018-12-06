package knox.spring.data.neo4j.operations;

import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.NodeSpace;

public class OROperator {
	
	public static void apply(List<NodeSpace> inputSpaces, NodeSpace outputSpace) {
		Union union = new Union(inputSpaces);
		
		Set<Edge> blankEdges = union.apply();
		
		union.getSpace().deleteBlankEdges(blankEdges);
		
		outputSpace.shallowCopyNodeSpace(union.getSpace());
	}
}
