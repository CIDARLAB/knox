package knox.spring.data.neo4j.operations;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.NodeSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReverseOperator {
	public static void apply(NodeSpace inputSpace, NodeSpace outputSpace) {
		//copy input space to a new output space
		outputSpace.copyNodeSpace(inputSpace);

		Set<Edge> allEdges = outputSpace.getEdges();

		//traverse all edges of input space and flip the orientation attribute
		for(Edge edge: allEdges){
			edge.reverseOrientation();
		}
	}
}