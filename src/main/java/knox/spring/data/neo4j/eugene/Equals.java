package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.HashSet;
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
		if (rule.isEndsWith()) {
			apply(rule.getOperands().get(0));
		} else if (rule.isStartsWith()) {
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
	
	private void apply(String objectID) {
		Set<Node> originalStartNodes = space.getStartNodes();
		
		for (Node startNode : originalStartNodes) {
			List<Edge> path = new LinkedList<Edge>();
			
			Set<Edge> backEdges = new HashSet<Edge>();
			
			Stack<List<Edge>> pathStack = new Stack<List<Edge>>();
			
			Stack<Set<Edge>> backStack = new Stack<Set<Edge>>();
			
			for (int i = 0; i < startNode.getNumEdges() - 1; i++) {
				pathStack.push(path);
				
				backStack.push(backEdges);
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
				
				backEdges = new HashSet<Edge>(backEdges);
				
				backEdges.add(edge);
				
				if (edge.hasComponentID(objectID) && edge.getHead().isAcceptNode()) {
					copyPath(path, objectID, copyIndex, nodeCopies);
				} 
				
				if (edge.getHead().hasEdges()) {
					int numEdges = 0;

					for (Edge headEdge : edge.getHead().getEdges()) {
						if (!backEdges.contains(headEdge)) {
							edgeStack.push(headEdge);
							
							numEdges++;
						}
					}
					
					for (int i = 0; i < numEdges - 1; i++) {
						pathStack.push(path);
						
						backStack.push(backEdges);
					}
				} else if (!pathStack.isEmpty()) {
					path = pathStack.pop();
					
					backEdges = backStack.pop();

					if (path.size() < copyIndex) {
						copyIndex = path.size();
					}
				}
			}
		}
		
		for (Node originalStartNode : originalStartNodes) {
			originalStartNode.deleteStartNodeType();
		}
		
		if (space.hasNodes()) {
			for (Node node : space.getNodes()) {
				if (node.hasEdges()) {
					for (Edge edge : node.getEdges()) {
						if (edge.getHead().isAcceptNode() && !edge.hasComponentID(objectID)) {
							edge.getHead().deleteAcceptNodeType();
						}
					}
				}
			}
		}
		
		space.deleteUnconnectedNodes();
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
				} else if (edge.hasComponentID(objectID) && path.size() == subjectIndex + 1) {
					copyPath(path, objectID, copyIndex, nodeCopies);

					if (!pathStack.isEmpty()) {
						path = pathStack.pop();

						copyIndex = path.size();
					} 
				} else if (!pathStack.isEmpty()) {
					path = pathStack.pop();

					if (path.size() < copyIndex) {
						copyIndex = path.size();
					}
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
	
	private void copyPath(List<Edge> path, String objectID, int copyIndex, List<Node> nodeCopies) {
		if (nodeCopies.isEmpty()) {
			nodeCopies.add(space.copyNode(path.get(0).getTail()));
		}
		
		for (int i = copyIndex; i < path.size(); i ++) {
			if (i == path.size() - 1) {
				ArrayList<String> compIDs = new ArrayList<String>();
				
				compIDs.add(objectID);
				
				Edge edgeCopy = nodeCopies.get(i).copyEdge(path.get(i));
				
				edgeCopy.setComponentIDs(compIDs);
			} else {
				Node headCopy = space.copyNode(path.get(i).getHead());
				
				if (i + 1 < nodeCopies.size()) {
					nodeCopies.remove(i + 1);
				}
				
				nodeCopies.add(i + 1, headCopy);
				
				nodeCopies.get(i).copyEdge(path.get(i), headCopy);
			}
		}
	}
	
	public NodeSpace getNodeSpace() {
		return space;
	}

}
