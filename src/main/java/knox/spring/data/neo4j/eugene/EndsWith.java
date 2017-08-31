package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

public class EndsWith {
	
	NodeSpace space;
	
	private static final Logger LOG = LoggerFactory.getLogger(EndsWith.class);
	
	Rule rule;
	
	public EndsWith(NodeSpace space, Rule rule) {
		this.space = space;
		
		this.rule = rule;
	}
	
	public void apply() {
		if (rule.isEndsWith()) {
			apply(rule.getOperands().get(0));
		}
	}
	
	private void apply(String objectID) {
		HashMap<String, Set<Edge>> idToIncomingEdges = space.mapNodeIDsToIncomingEdges();
		
		for (Node acceptNode : space.getAcceptNodes()) {
			
		}
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
