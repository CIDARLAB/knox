package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;

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
	
	private void copyPath(List<Edge> path, String objectID, int copyIndex, List<Node> nodeCopies) {
		if (nodeCopies.isEmpty()) {
			nodeCopies.add(space.copyNode(path.get(0).getTail()));
		}
		
		for (int i = copyIndex; i < path.size(); i ++) {
			Node headCopy = space.copyNode(path.get(i).getHead());
			
			if (i + 1 < nodeCopies.size()) {
				nodeCopies.remove(i + 1);
			}
			
			nodeCopies.add(i + 1, headCopy);
			
			if (i == path.size() - 1) {
				ArrayList<String> compIDs = new ArrayList<String>();
				
				compIDs.add(objectID);
				
				nodeCopies.get(i).copyEdge(path.get(i), headCopy, compIDs);
				
				if (path.get(i).getHead().hasEdges()) {
					for (Edge edge : path.get(i).getHead().getEdges()) {
						headCopy.copyEdge(edge);
					}
				}
			} else {
				nodeCopies.get(i).copyEdge(path.get(i), headCopy);
			}
		}
	}
	
	private void apply(int subjectIndex, String objectID) {
		Set<Node> originalStartNodes = space.getStartNodes();
		
		for (Node startNode : originalStartNodes) {
			List<Edge> path = new LinkedList<Edge>();
			
			Stack<List<Edge>> pathStack = new Stack<List<Edge>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				pathStack.push(path);
			}
			
			int copyIndex = 0;
			
			List<Node> nodeCopies = new LinkedList<Node>();
			
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
				
				if (edge.getHead().hasEdges() && path.size() < subjectIndex + 1) {
					for (int i = 0; i < edge.getHead().getNumEdges() - 1; i++) {
						pathStack.push(path);
					}

					for (Edge headEdge : edge.getHead().getEdges()) {
						edgeStack.push(headEdge);
					}
					
//					LOG.info("add {}", edge.getComponentIDs().toString() + " length " + path.size()
//							+ " forks " + pathStack.size());
				} else if (edge.hasComponentID(objectID) && path.size() == subjectIndex + 1) {
					copyPath(path, objectID, copyIndex, nodeCopies);

					if (!pathStack.isEmpty()) {
						path = pathStack.pop();

						copyIndex = path.size();
					} 
					
//					LOG.info("copy on {}", edge.getComponentIDs().toString() + " forks "
//							+ pathStack.size() + " copy " + copyIndex);
				} else if (!pathStack.isEmpty()) {
					path = pathStack.pop();

					if (path.size() < copyIndex) {
						copyIndex = path.size();
					}
					
//					LOG.info("backtrack on {}", edge.getComponentIDs().toString() + " forks "
//							+ pathStack.size() + " copy " + copyIndex);
				}
			}
		}
		
		for (Node originalStartNode : originalStartNodes) {
			originalStartNode.deleteStartNodeType();
		}
		
		space.deleteUnconnectedNodes();
	}
	
	private void apply(int subjectIndex, int objectIndex) {
		
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
