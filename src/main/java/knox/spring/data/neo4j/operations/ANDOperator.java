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
		
		//System.out.println("AND Operator:");
		//System.out.println("tolerance: " + String.valueOf(tolerance));
		Product product = new Product(inputSpaces.get(0));
				
		for (int i = 1; i < inputSpaces.size(); i++) {
			if (isComplete) {
				product.applyTensor(inputSpaces.get(i), tolerance, 0, 2, roles, irrelevantParts);
			} else {
				product.applyTensor(inputSpaces.get(i), tolerance, 0, 0, roles, irrelevantParts);
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
