package knox.spring.data.neo4j.eugene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.eugene.Part.PartType;

public class SubSpace {
	
	private DesignSpace space;
	
	private List<Node> nodes;
	
	private HashMap<Part, List<Integer>> partToNodeIndices;
	
	private Set<Edge> outgoingEdges = new HashSet<Edge>();
	
	private int numRootNodes = -1;
	
	private SubSpace(DesignSpace space, List<Node> nodes, HashMap<Part, List<Integer>> partToNodeIndices,
			int numRootNodes) {
		this.space = space;
		
		this.nodes = nodes;
		
		this.partToNodeIndices = partToNodeIndices;
		
		this.numRootNodes = numRootNodes;
	}
	
	public SubSpace(DesignSpace space, Device device, HashMap<PartType, Set<Part>> partsLibrary) {
		this.space = space;
		
		this.nodes = new ArrayList<Node>(device.getNumParts()); 
		
		this.partToNodeIndices = new HashMap<Part, List<Integer>>();
		
		if (device.getNumParts() > 0) {
    		nodes.add(space.createStartNode());

    		for (int i = 1; i < device.getNumParts(); i++) {
    			nodes.add(space.createNode());

    			connectNodes(i - 1, i, device.getPart(i - 1), partsLibrary);
    		}
    		
    		nodes.add(space.createAcceptNode());
    		
    		connectNodes(device.getNumParts() - 1, device.getNumParts(), 
    				device.getLastPart(), partsLibrary);
    	}
		
		this.numRootNodes = nodes.size();
	}
	
	public void addParts(Set<Part> parts) {
		for (Part part : parts) {
			addPart(part);
		}
	}
	
	public void addPart(Part part) {
		if (partToNodeIndices.containsKey(part)) {
			for (Integer nodeIndex : partToNodeIndices.get(part)) {
				for (Edge edge : nodes.get(nodeIndex).getEdges()) {
					if (!outgoingEdges.contains(edge)) {
						edge.addComponent(part.getID(), part.getType().getValue());
					}
				}
			}
		}
	}
	
	public void connectByPart(SubSpace subSpace, Part part) {
		for (Integer fromIndex : getNodeIndices(part)) {
			connectToSubSpace(subSpace, part, fromIndex.intValue(), 
					fromIndex.intValue() - nodes.size() + subSpace.getNumNodes() + 1);
		}
	}
	
	public void connectByRuleset(SubSpace subSpace, Ruleset ruleset) {
		int fromIndex = ruleset.getIndex() - numRootNodes + nodes.size();
		
		int toIndex = ruleset.getIndex() - numRootNodes + subSpace.getNumNodes() + 1;

		connectToSubSpace(subSpace, ruleset.getImplicant(), fromIndex, toIndex);
	}
	
	public void connectToSubSpace(SubSpace subSpace, Part part, int fromIndex, int toIndex) {
		ArrayList<String> compIDs = new ArrayList<String>(1);

		compIDs.add(part.getID());

		ArrayList<String> compRoles = new ArrayList<String>(1);

		compRoles.add(part.getType().getValue());

		outgoingEdges.add(nodes.get(fromIndex).createEdge(subSpace.getNode(toIndex), compIDs, compRoles));

		System.out.println("connect " + part.getID() + " " + fromIndex + " of " + nodes.size()
				+ " to " + toIndex + " of " + subSpace.getNumNodes());
	}
	
	private void connectNodes(int fromIndex, int toIndex, Part part, 
			HashMap<PartType, Set<Part>> partsLibrary) {
		ArrayList<String> compIDs;

		if (part.hasID()) {
			compIDs = new ArrayList<String>(1);

			compIDs.add(part.getID());
			
			if (!partToNodeIndices.containsKey(part)) {
				partToNodeIndices.put(part, new LinkedList<Integer>());
			}
			
			partToNodeIndices.get(part).add(new Integer(fromIndex));
		} else {
			compIDs = new ArrayList<String>(partsLibrary.size());

			for (Part p : partsLibrary.get(part.getType())) {
				compIDs.add(p.getID());
				
				if (!partToNodeIndices.containsKey(p)) {
					partToNodeIndices.put(p, new LinkedList<Integer>());
				}
				
				partToNodeIndices.get(p).add(new Integer(fromIndex));
			}
		}

		ArrayList<String> compRoles = new ArrayList<String>(1);

		compRoles.add(part.getType().getValue());
		
		nodes.get(fromIndex).createEdge(nodes.get(toIndex), compIDs, compRoles);
	}
	
	public SubSpace copy() {
		return copyFromIndex(0);
    }
	
	public SubSpace copyFromPart(Part part) {
		List<Integer> nodeIndices = getNodeIndices(part);
		
		if (nodeIndices.size() > 0) {
			return copyFromIndex(nodeIndices.get(0).intValue() + 1);
		} else {
			return copy();
		}
	}
	
	public SubSpace copyByRuleset(Ruleset ruleset) {
		if (ruleset.hasIndex()) {
			return copyFromIndex(ruleset.getIndex() - numRootNodes + nodes.size() + 1);
		} else {
			return copy();
		}
	}
	
	public SubSpace copyFromIndex(int copyIndex) {
		List<Node> nodeCopies = new ArrayList<Node>(nodes.size() - copyIndex);
		
		for (int i = copyIndex; i < nodes.size(); i++) {
			nodeCopies.add(space.copyNode(nodes.get(i)));
		}
		
		for (int i = copyIndex; i < nodes.size() - 1; i++) {
			if (nodes.get(i).hasEdges()) {
				for (Edge edge : nodes.get(i).getEdges()) {
					if (!outgoingEdges.contains(edge)) {
						if (edge.isComponentEdge()) {
							nodeCopies.get(i - copyIndex).copyEdge(edge, nodeCopies.get(i + 1 - copyIndex));
						} else {
							nodeCopies.get(i - copyIndex).copyEdge(edge, nodeCopies.get(0));
						}
					}
				}
			}
		}
		
		HashMap<Part, List<Integer>> partToNodeIndicesCopy = new HashMap<Part, List<Integer>>();
		
		for (Part part : partToNodeIndices.keySet()) {
			List<Integer> nodeIndicesCopy = new LinkedList<Integer>();
			
			for (Integer nodeIndex : partToNodeIndices.get(part)) {
				if (nodeIndex.intValue() >= copyIndex) {
					nodeIndicesCopy.add(new Integer(nodeIndex.intValue() - copyIndex));
				}
			}
			
			partToNodeIndicesCopy.put(part, nodeIndicesCopy);
		}
		
		return new SubSpace(space, nodeCopies, partToNodeIndicesCopy, numRootNodes);
    }
	
	public void deleteParts(Set<Part> parts) {
		for (Part part : parts) {
			deletePart(part);
		}
	}
	
	public void deletePart(Part part) {
		if (partToNodeIndices.containsKey(part)) {
			for (Integer nodeIndex : partToNodeIndices.get(part)) {
				for (Edge edge : nodes.get(nodeIndex).getEdges()) {
					if (!outgoingEdges.contains(edge)) {
						edge.deleteComponent(part.getID());
					}
				}
			}
		}
	}
	
	public Node getNode(int nodeIndex) {
		return nodes.get(nodeIndex);
	}
	
	public List<Integer> getNodeIndices(Part part) {
		return partToNodeIndices.get(part);
	}
	
	public List<Node> getNodes() {
		return nodes;
	}
	
	public int getNumNodes() {
		return nodes.size();
	}
	
	public int getNumRootNodes() {
		return numRootNodes;
	}
}
