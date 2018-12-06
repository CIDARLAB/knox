package knox.spring.data.neo4j.operations;

import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.NodeSpace;

public class ANDOperator {
	
	public static void apply(List<NodeSpace> inputSpaces, NodeSpace outputSpace, 
			int tolerance, boolean isComplete, Set<String> roles) {
		Product product = new Product(inputSpaces.get(0));

		for (int i = 1; i < inputSpaces.size(); i++) {
			List<Set<Edge>> blankEdges;

			if (isComplete) {
				blankEdges = product.applyTensor(inputSpaces.get(i), tolerance, 2, roles);
			} else {
				blankEdges = product.applyTensor(inputSpaces.get(i), tolerance, 0, roles);
			}

			for (int j = 0; j < blankEdges.size(); j++) {
				product.getSpace().deleteBlankEdges(blankEdges.get(j));
			}
		}

		Union union = new Union(product.getSpace());

		Set<Edge> blankEdges = union.apply();

		union.getSpace().deleteBlankEdges(blankEdges);

		outputSpace.shallowCopyNodeSpace(union.getSpace());
	}
}
