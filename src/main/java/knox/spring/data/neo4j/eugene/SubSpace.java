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
	
	private HashMap<PartType, List<Integer>> partTypeToNodeIndices;
	
	public Set<Edge> outgoingEdges = new HashSet<Edge>();
	
	private SubSpace(DesignSpace space, List<Node> nodes, HashMap<PartType, List<Integer>> partTypeToNodeIndices) {
		this.space = space;
		
		this.nodes = nodes;
		
		this.partTypeToNodeIndices = partTypeToNodeIndices;
	}
	
	public SubSpace(DesignSpace space, Device device, HashMap<PartType, Set<Part>> partsLibrary) {
		this.space = space;
		
		this.nodes = new ArrayList<Node>(device.getNumParts()); 
		
		this.partTypeToNodeIndices = new HashMap<PartType, List<Integer>>();
		
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
	}
	
	public void addParts(Set<Part> parts) {
		for (Part part : parts) {
			addPart(part);
		}
	}
	
	public void addPart(Part part) {
		if (partTypeToNodeIndices.containsKey(part.getType())) {
			for (Integer nodeIndex : partTypeToNodeIndices.get(part.getType())) {
				for (Edge edge : nodes.get(nodeIndex).getEdges()) {
					if (!outgoingEdges.contains(edge)) {
						edge.addComponent(part.getID(), part.getType().getValue());
					}
				}
			}
		}
	}
	
	public boolean beginsWithPartType(Part part) {
		return hasPartType(part, 1);
	}
	
	public void connectToSubSpace(SubSpace nextSubSpace, Part part, int connectionIndex) {
		List<Integer> connectionIndices = new ArrayList<Integer>(1);
		
		connectionIndices.set(0, new Integer(connectionIndex));
		
		connectToSubSpace(nextSubSpace, part, connectionIndices);
	}
	
	public void connectToSubSpace(SubSpace nextSubSpace, Part part) {
		connectToSubSpace(nextSubSpace, part, partTypeToNodeIndices.get(part.getType()));
	}
	
	private void connectToSubSpace(SubSpace nextSubSpace, Part part, List<Integer> nodeIndices) {
		int shift = nextSubSpace.getNumNodes() - nodes.size() + 1;
		
		for (int i = 0; i < nodeIndices.size(); i++) {
			int shiftedIndex = nodeIndices.get(i).intValue() + shift;
			
			if (shiftedIndex < nextSubSpace.getNumNodes()) {
				Node node = nodes.get(nodeIndices.get(i).intValue());

				Node nextNode = nextSubSpace.getNode(shiftedIndex);
				
				ArrayList<String> compIDs = new ArrayList<String>(1);
				
				compIDs.add(part.getID());
				
				ArrayList<String> compRoles = new ArrayList<String>(1);
				
				compRoles.add(part.getType().getValue());
				
				if (!node.hasEdge(nextNode)) {
					outgoingEdges.add(node.createEdge(nextNode, compIDs, compRoles));
				}
			}
		}
	}
	
	private void connectNodes(int index1, int index2, Part part, 
			HashMap<PartType, Set<Part>> partsLibrary) {
		ArrayList<String> compIDs;

		if (part.hasID()) {
			compIDs = new ArrayList<String>(1);

			compIDs.add(part.getID());
		} else {
			compIDs = new ArrayList<String>(partsLibrary.size());

			for (Part p : partsLibrary.get(part.getType())) {
				compIDs.add(p.getID());
			}
		}

		ArrayList<String> compRoles = new ArrayList<String>(1);

		compRoles.add(part.getType().getValue());
		
		nodes.get(index1).createEdge(nodes.get(index2), compIDs, compRoles);

		if (!partTypeToNodeIndices.containsKey(part.getType())) {
			partTypeToNodeIndices.put(part.getType(), new LinkedList<Integer>());
		}
		
		partTypeToNodeIndices.get(part.getType()).add(new Integer(index1));
	}
	
	public SubSpace copy() {
		return copyFromIndex(0);
    }
	
	public SubSpace copyFromPart(Part part) {
		List<Integer> nodeIndices = partTypeToNodeIndices.get(part.getType());
		
		if (nodeIndices.size() > 0) {
			return copyFromIndex(nodeIndices.get(0) + 1);
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
		
		HashMap<PartType, List<Integer>> partTypeToNodeIndicesCopy = new HashMap<PartType, List<Integer>>();
		
		for (PartType type : partTypeToNodeIndices.keySet()) {
			List<Integer> nodeIndicesCopy = new LinkedList<Integer>();
			
			for (Integer nodeIndex : partTypeToNodeIndices.get(type)) {
				if (nodeIndex.intValue() >= copyIndex) {
					nodeIndicesCopy.add(new Integer(nodeIndex.intValue() - copyIndex));
				}
			}
			
			partTypeToNodeIndicesCopy.put(type, nodeIndicesCopy);
		}
		
		return new SubSpace(space, nodeCopies, partTypeToNodeIndicesCopy);
    }
	
	public void deleteParts(Set<Part> parts) {
		for (Part part : parts) {
			deletePart(part);
		}
	}
	
	public void deletePart(Part part) {
		if (partTypeToNodeIndices.containsKey(part.getType())) {
			for (Integer nodeIndex : partTypeToNodeIndices.get(part.getType())) {
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
	
	public List<Integer> getNodeIndices(PartType type) {
		return partTypeToNodeIndices.get(type);
	}
	
	public List<Node> getNodes() {
		return nodes;
	}
	
	public int getNumNodes() {
		return nodes.size();
	}
	
	public HashMap<PartType, List<Integer>> getPartTypeToNodeIndices() {
		return partTypeToNodeIndices;
	}
	
	public boolean hasPartType(Part part) {
		return hasPartType(part, nodes.size());
	}
	
	private boolean hasPartType(Part part, int bound) {
		for (int i = 0; i < bound; i++) {
			if (nodes.get(i).hasEdges()) {
				for (Edge edge : nodes.get(i).getEdges()) {
					if (!outgoingEdges.contains(edge)
							&& edge.hasComponentRole(part.getType().getValue())) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
}
