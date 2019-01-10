package knox.spring.data.neo4j.operations;

import java.util.List;

import knox.spring.data.neo4j.domain.NodeSpace;

public class RepeatOperator {
	
	public static void apply(List<NodeSpace> inputSpaces, NodeSpace outputSpace, boolean isOptional) {
		Star star = new Star(inputSpaces);

		star.apply(isOptional);
		
		star.getSpace().removeBlankCycles();

		outputSpace.shallowCopyNodeSpace(star.getSpace());
	}
}
