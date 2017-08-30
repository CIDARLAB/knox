package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.operations.Union;

public class Equals {
	
	NodeSpace space;
	
	private static final Logger LOG = LoggerFactory.getLogger(Equals.class);
	
	Rule rule;
	
	public Equals(NodeSpace space, Rule rule) {
		this.space = space;
		
		this.rule = rule;
	}
	
	public void apply() {
		if (rule.isStartsWith()) {
			apply(0, rule.getOperands().get(0));
		} else if (rule.isEquals()) {
			String subjectIndex = rule.getOperands().get(0);
			
			if (Rule.isIndex(rule.getOperands().get(1))) {
				String objectIndex = rule.getOperands().get(1);
				
				apply(Integer.parseInt(subjectIndex.substring(1, subjectIndex.length() - 1)),
						Integer.parseInt(objectIndex.substring(1, objectIndex.length() - 1)));
			} else {
				apply(Integer.parseInt(subjectIndex.substring(1, subjectIndex.length() - 1)),
						rule.getOperands().get(1));
			}
		}
	}
	
	private void copyPath(List<Edge> path, String objectID, HashMap<String, Node> idToNodeCopy) {
		space.unionEdges(path, idToNodeCopy);
		
		Node pathTail = path.get(path.size() - 1).getTail(); 
		
		ArrayList<String> compIDs = new ArrayList<String>();
		
		compIDs.add(objectID);
		
		pathTail.getEdges().iterator().next().setComponentIDs(compIDs);
		
		Node pathHead = path.get(path.size() - 1).getHead();
		
		if (pathHead.hasEdges()) {
			for (Edge edge : pathHead.getEdges()) {
				idToNodeCopy.get(pathHead.getNodeID()).copyEdge(edge);
			}
		}
	}
	
	private void apply(int subjectIndex, String objectID) {
		HashMap<String, Node> idToNodeCopy = new HashMap<String, Node>();
		
		LOG.info("subjectIndex+1= {}", subjectIndex + 1);
		
		Set<Node> originalStartNodes = space.getStartNodes();
		
		for (Node startNode : originalStartNodes) {
			List<Edge> path = new LinkedList<Edge>();
			
			Stack<List<Edge>> pathStack = new Stack<List<Edge>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				pathStack.push(path);
			}
			
			Stack<Edge> edgeStack = new Stack<Edge>();
			
			if (startNode.hasEdges()) {
				for (Edge edge : startNode.getEdges()) {
					edgeStack.push(edge);
				}
			}

			while (!edgeStack.isEmpty()) {
				Edge edge = edgeStack.pop();
				
				path = new LinkedList<Edge>(path);
				
				path.add(edge);
				
				
				
				if (edge.hasComponentID(objectID) && path.size() == subjectIndex + 1) {
					LOG.info("copy {}", "path");
					
					copyPath(path, objectID, idToNodeCopy);
				}

				if (edge.getHead().hasEdges() && path.size() < subjectIndex + 1) {
					for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
						pathStack.push(path);
					}

					for (Edge headEdge : edge.getHead().getEdges()) {
						edgeStack.push(headEdge);
					}
				} else if (!pathStack.isEmpty()) {
					path = pathStack.pop();
					
					LOG.info("backtrack on {}", edge.getComponentIDs().toString() + " forks "
							+ pathStack.size());
				}
				
				LOG.info("add {}", edge.getComponentIDs().toString() + " length " + path.size()
						+ " forks " + pathStack.size());
			}
		}
		
		for (Node originalStartNode : originalStartNodes) {
			originalStartNode.deleteStartNodeType();
		}
		
		LOG.info("size {}", space.getNumNodes());
		
		Union union = new Union(space);

		union.connect(false);
		
		LOG.info("size {}", space.getNumNodes());
		
		space.minimize();
		
		space.deleteUnreachableNodes();
	}
	
	private void apply(int subjectIndex, int objectIndex) {
		
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
