package knox.spring.data.neo4j.eugene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Edge.Orientation;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class Forward {
	
	NodeSpace space;
	
	private static final Logger LOG = LoggerFactory.getLogger(Forward.class);
	
	Rule rule;
	
	public Forward(NodeSpace space, Rule rule) {
		this.space = space;
		
		this.rule = rule;
	}
	
	public void apply() {
		if (rule.isForward()) {
			if (rule.getNumOperands() == 1) {
				apply(rule.getOperands().get(0), Orientation.REVERSE_COMPLEMENT.getValue());
			} else {
				apply(Orientation.REVERSE_COMPLEMENT.getValue());
			}
		} else if (rule.isReverse()) {
			if (rule.getNumOperands() == 1) {
				apply(rule.getOperands().get(0), Orientation.INLINE.getValue());
			} else {
				apply(Orientation.INLINE.getValue());
			}
		}
	}
	
	private void apply(String orientation) {
		if (space.hasNodes()) {
			for (Node node : space.getNodes()) {
				node.deleteEdgesWithOrientation(orientation);
			}
			
			space.deleteUnacceptableNodes();
		}
		
		
	}
	
	private void apply(String objectID, String orientation) {
		if (space.hasNodes()) {
			for (Node node : space.getNodes()) {
				node.deleteComponentID(objectID, orientation);
			}
			
			space.deleteUnacceptableNodes();
		}
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
