package knox.spring.data.neo4j.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.NodeSpace;

public class ANDOperator {
	
	private static final Logger LOG = LoggerFactory.getLogger(NodeSpace.class);
	
	public static void apply(List<NodeSpace> inputSpaces, NodeSpace outputSpace, int tolerance, boolean isComplete, Set<String> roles, ArrayList<String> irrelevantParts) {
		apply(inputSpaces, outputSpace, tolerance, 0, isComplete, roles, irrelevantParts);
	}
	
	public static void apply(List<NodeSpace> inputSpaces, NodeSpace outputSpace, int tolerance, int weightTolerance, boolean isComplete, Set<String> roles, ArrayList<String> irrelevantParts) {
		
		//System.out.println("AND Operator:");
		//System.out.println("tolerance: " + String.valueOf(tolerance));

		// Sort input spaces by number of edges (ascending)
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < inputSpaces.size(); i++) {
			indices.add(i);
		}
		indices.sort((a, b) -> {
			int edgesA = inputSpaces.get(a).getEdges().size();
			int edgesB = inputSpaces.get(b).getEdges().size();
			return Integer.compare(edgesA, edgesB);
		});

		Product product = new Product(inputSpaces.get(indices.get(0)));
				
		for (int i = 1; i < inputSpaces.size(); i++) {
			if (isComplete) {
				product.applyTensor(inputSpaces.get(indices.get(i)), tolerance, weightTolerance, 2, roles, irrelevantParts);
			} else {
				product.applyTensor(inputSpaces.get(indices.get(i)), tolerance, weightTolerance, 0, roles, irrelevantParts);
			}

			// Early exit if product is already empty
			if (!product.getSpace().hasNodes()) {
				outputSpace.shallowCopyNodeSpace(new NodeSpace(new ArrayList<String>(), new ArrayList<String>()));
				return;
			}
			
			product.getSpace().deleteBlankEdges(product.getSpace().getBlankEdges());
		}
		
		if (product.getSpace().hasNodes()) {
			Union union = new Union(product.getSpace());

			Set<Edge> blankEdges = union.apply();

			union.getSpace().deleteBlankEdges(blankEdges);

			outputSpace.shallowCopyNodeSpace(union.getSpace());
		} else {
			outputSpace.shallowCopyNodeSpace(new NodeSpace(new ArrayList<String>(), new ArrayList<String>()));
		}
	}
}
