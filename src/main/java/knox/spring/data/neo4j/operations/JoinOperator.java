package knox.spring.data.neo4j.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.NodeSpace;

public class JoinOperator {
	
	public static void apply(List<NodeSpace> inputSpaces, NodeSpace outputSpace) {
		Concatenation concat = new Concatenation();

		Set<Edge> blankEdges = new HashSet<Edge>();

		for (NodeSpace inputSpace : inputSpaces) {
			blankEdges.addAll(concat.apply(inputSpace));
		}

		concat.getSpace().deleteBlankEdges(blankEdges);

		outputSpace.shallowCopyNodeSpace(concat.getSpace());
	}
}
