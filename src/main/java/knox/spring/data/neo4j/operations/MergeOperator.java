package knox.spring.data.neo4j.operations;

import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.NodeSpace;

public class MergeOperator {
	
	public static void apply(List<NodeSpace> inputSpaces, NodeSpace outputSpace, 
			int tolerance, Set<String> roles) {
		Product product = new Product(inputSpaces.get(0));

		for (int i = 1; i < inputSpaces.size(); i++) {
			List<Set<Edge>> blankEdges = product.applyModifiedStrong(inputSpaces.get(i), tolerance, roles);

			for (int j = 0; j < blankEdges.size(); j++) {
				product.getSpace().deleteBlankEdges(blankEdges.get(j));
			}	
		}

		Union union = new Union(product.getSpace());

		Set<Edge> blankEdges = union.apply();

		union.getSpace().deleteBlankEdges(blankEdges);
		
		union.getSpace().removeBlankCycles();

		outputSpace.shallowCopyNodeSpace(union.getSpace());

		outputSpace.weightBlankEdges();

		outputSpace.deleteUnacceptableNodes();
	}
}
