package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.Commit;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Snapshot;
import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.NodeNotFoundException;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;
import knox.spring.data.neo4j.repositories.NodeRepository;

import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceOntology;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Location;
import org.sbolstandard.core2.Range;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SequenceAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


@Service
@Transactional
public class DesignSpaceService {

    @Autowired DesignSpaceRepository designSpaceRepository;
    @Autowired NodeRepository nodeRepository;
    
    public static final String RESERVED_ID = "knox";
    
    public void mergeSBOL(List<InputStream> inputSBOLStreams, String outputSpaceID, String authority) 
    		throws SBOLValidationException, IOException, SBOLConversionException {
    	
    	Set<ComponentDefinition> compositeDefs = new HashSet<ComponentDefinition>();
    	
    	for (InputStream inputSBOLStream : inputSBOLStreams) {
    		
    		if (authority != null) {
    			SBOLReader.setURIPrefix(authority);
    		}
    		
    		System.out.println("reading");
    		SBOLDocument sbolDoc = SBOLReader.read(inputSBOLStream);
    		System.out.println("fetching");

    		Set<ComponentDefinition> compDefs = getDNAComponentDefinitions(sbolDoc);

    		for (ComponentDefinition compDef : compDefs) {
    			if (!hasDesignSpace(compDef.getIdentity().toString())) {
    				System.out.println(compDef.getIdentity().toString());
    				DesignSpace outputSpace = new DesignSpace(compDef.getIdentity().toString());

    				List<ComponentDefinition> leafDefs = flattenComponentDefinition(compDef);

    				Node currentNode = outputSpace.createNode("start");

    				for (int i = 0; i < leafDefs.size(); i++) {
    					ArrayList<String> compIDs = new ArrayList<String>();
    					compIDs.add(leafDefs.get(i).getIdentity().toString());

    					ArrayList<String> compRoles = new ArrayList<String>(
    							convertSOIdentifiersToNames(leafDefs.get(i).getRoles()));

    					Node nextNode;
    					if (i < leafDefs.size() - 1) {
    						nextNode = outputSpace.createNode();
    					} else {
    						nextNode = outputSpace.createNode("accept");
    					}

    					currentNode.createEdge(nextNode, compIDs, compRoles);

    					currentNode = nextNode;
    				}

    				outputSpace.createHeadBranch(compDef.getIdentity().toString());

    				designSpaceRepository.save(outputSpace);
    				
    				commitToBranch(outputSpace.getSpaceID(), outputSpace.getHeadBranch().getBranchID());
    			}
    		}

    		for (ComponentDefinition compDef : compDefs) {
    			if (compDef.getComponents().size() > 0) {
    				compositeDefs.add(compDef);
    			}
    		}
    	}
    	
    	Iterator<ComponentDefinition> defIterator = compositeDefs.iterator();
    	
    	if (compositeDefs.size() > 1) {
    		String defID1 = defIterator.next().getIdentity().toString();
    		String defID2 = defIterator.next().getIdentity().toString();
    		System.out.println("merging " + defID1 + " " + defID2);
    		mergeDesignSpaces(defID1, defID2, 
    				outputSpaceID, false, false);
    	}
    	
    	while (defIterator.hasNext()) {
    		String defID = defIterator.next().getIdentity().toString();
    		System.out.println("merging " + defID);
    		mergeDesignSpaces(outputSpaceID, defID, outputSpaceID, false, false);
    	}
    }
    
    private List<ComponentDefinition> flattenComponentDefinition(ComponentDefinition rootDef) {
    	List<ComponentDefinition> leafDefs = new ArrayList<ComponentDefinition>();
    	
    	Stack<ComponentDefinition> defStack = new Stack<ComponentDefinition>();
    	defStack.push(rootDef);
    	
    	while (defStack.size() > 0) {
    		ComponentDefinition compDef = defStack.pop();
   
    		Set<SequenceAnnotation> seqAnnos = compDef.getSequenceAnnotations();
    		
    		if (seqAnnos.size() == 0) {
    			leafDefs.add(compDef);
    		} else {
    			List<Component> sortedSubComps = new ArrayList<Component>();
    			HashMap<String, Integer> compIDToStart = new HashMap<String, Integer>();

    			for (SequenceAnnotation seqAnno : seqAnnos) {
    				Component subComp = seqAnno.getComponent();
    				compIDToStart.put(subComp.getIdentity().toString(), 
    						getStartOfSequenceAnnotation(seqAnno));

    				int i = 0;
    				while (i < sortedSubComps.size() 
    						&& compIDToStart.get(subComp.getIdentity().toString()) 
    						< compIDToStart.get(sortedSubComps.get(i).getIdentity().toString())) {
    					i++;
    				}

    				sortedSubComps.add(i, subComp);
    			}

    			for (Component subComp : sortedSubComps) {
    				defStack.push(subComp.getDefinition());
    			}
    		}
    	}
    	return leafDefs;
    }
    
    private Set<ComponentDefinition> getDNAComponentDefinitions(SBOLDocument sbolDoc) {
    	Set<ComponentDefinition> dnaCompDefs = new HashSet<ComponentDefinition>();
    	
    	for (ComponentDefinition compDef : sbolDoc.getComponentDefinitions()) {
			if (isDNAComponentDefinition(compDef)) {
				dnaCompDefs.add(compDef);
			}
		}
    	return dnaCompDefs;
    }
    
    private boolean isDNAComponentDefinition(ComponentDefinition compDef) {
    	Set<URI> compTypes = compDef.getTypes();
    	if (compTypes.size() == 0) {
    		return false;
    	} else if (compTypes.contains(ComponentDefinition.DNA)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private Set<String> convertSOIdentifiersToNames(Set<URI> soIdentifiers) {
    	Set<String> roleNames= new HashSet<String>();
		if (soIdentifiers.size() == 0) {
			roleNames.add("engineered_region");
		} else {
			for (URI soIdentifier : soIdentifiers) {
				if (soIdentifier.equals(SequenceOntology.PROMOTER)) {
		    		roleNames.add("promoter");
		    	} else if (soIdentifier.equals(SequenceOntology.type("SO:0000374"))) {
		    		roleNames.add("ribozyme");
		    	} else if (soIdentifier.equals(SequenceOntology.INSULATOR)) {
		    		roleNames.add("insulator");
		    	} else if (soIdentifier.equals(SequenceOntology.RIBOSOME_ENTRY_SITE)) {
		    		roleNames.add("ribosome_entry_site");
		    	} else if (soIdentifier.equals(SequenceOntology.CDS)) {
		    		roleNames.add("CDS");
		    	} else if (soIdentifier.equals(SequenceOntology.TERMINATOR)) {
		    		roleNames.add("terminator");
		    	} else {
		    		roleNames.add("engineered_region");
		    	}
			}
		}
    	return roleNames;
    }
    
    private int getStartOfSequenceAnnotation(SequenceAnnotation seqAnno) {
    	int start = -1;
    	for (Location location : seqAnno.getLocations()) {
    		if (location instanceof Range) {
    			Range range = (Range) location;
    			if (start < 0 || range.getStart() < start) {
    				start = range.getStart();
    			}
    		}
    	}
    	return start;
    }
    
    public void deleteBranch(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteBranch(targetSpaceID, targetBranchID);
	}
    
    public void copyHeadBranch(String targetSpaceID, String outputBranchID) {
    	designSpaceRepository.copyHeadBranch(targetSpaceID, outputBranchID);
    }
    
    public void checkoutBranch(String targetSpaceID, String targetBranchID) {
    	deleteNodes(targetSpaceID);
    	designSpaceRepository.checkoutBranch(targetSpaceID, targetBranchID);
    	deleteNodeCopyIndices(targetSpaceID);
    }
    
    public void commitToBranch(String targetSpaceID, String targetBranchID) {
    	createCommit(targetSpaceID, targetBranchID);
    	copyDesignSpaceToSnapshot(targetSpaceID, targetBranchID);
    }
    
    public void commitToHeadBranch(String targetSpaceID) {
    	String headBranchID = getHeadBranchID(targetSpaceID);
    	createCommit(targetSpaceID, headBranchID);
    	copyDesignSpaceToSnapshot(targetSpaceID, headBranchID);
    }
    
    public Map<String, Object> d3GraphBranches(String targetSpaceID) {
    	return mapBranchesToD3Format(designSpaceRepository.mapBranches(targetSpaceID));
    }
    
    public void insertBranch(String targetSpaceID, String inputBranchID1, String inputBranchID2, String targetNodeID, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);

    	Node startNode1 = getStartNode(targetSpaceID, outputBranchID);
    	Set<Node> acceptNodes1 = getAcceptNodes(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	Node nodeCopy = findNodeCopy(targetSpaceID, inputBranchID2, targetNodeID, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	if (nodeCopy != null) {
    		Set<Edge> removedEdges = removeOutgoingEdges(targetSpaceID, outputBranchID, nodeCopy.getNodeID());

    		if (removedEdges.size() > 0) {
    			for (Node acceptNode1 : acceptNodes1) {
    				deleteNodeType(targetSpaceID, outputBranchID, acceptNode1.getNodeID());
    			}
    		} else {
    			Set<Node> acceptNodes = getAcceptNodes(targetSpaceID, outputBranchID);
    			for (Node acceptNode : acceptNodes) {
    				if (nodeCopy.getNodeID().equals(acceptNode.getNodeID())) {
    					deleteNodeType(targetSpaceID, outputBranchID, nodeCopy.getNodeID());
    				}
    			}
    		}
    		if (startNode1 != null) {
    			deleteNodeType(targetSpaceID, outputBranchID, startNode1.getNodeID());
    			createEdge(targetSpaceID, outputBranchID, nodeCopy.getNodeID(), startNode1.getNodeID());
    		}
    		for (Node acceptNode1 : acceptNodes1) {
    			for (Edge removedEdge : removedEdges) {
    				if (removedEdge.hasComponentIDs() && removedEdge.hasComponentRoles()) {
    					createComponentEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    				} else {
    					createEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), removedEdge.getHead().getNodeID());
    				}
    			}
    		}
    	}
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void joinBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);
    	
    	Node startNode1 = getStartNode(targetSpaceID, outputBranchID);
    	Set<Node> acceptNodes1 = getAcceptNodes(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	if (startNode1 != null) {
    		for (Node startNode : getStartNodes(targetSpaceID, outputBranchID)) {
    			if (!startNode.getNodeID().equals(startNode1.getNodeID())) {
    				deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
    				for (Node acceptNode1 : acceptNodes1) {
    					deleteNodeType(targetSpaceID, outputBranchID, acceptNode1.getNodeID());
    					createEdge(targetSpaceID, outputBranchID, acceptNode1.getNodeID(), startNode.getNodeID());
    				}
    			}
    		}
    	}
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void mergeBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, 
    		String outputBranchID, boolean isIntersection, boolean isStrong) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	Branch inputBranch1 = targetSpace.findBranch(inputBranchID1);
    	Branch inputBranch2 = targetSpace.findBranch(inputBranchID2);
    	
    	Branch outputBranch = targetSpace.createBranch(outputBranchID, 
				Math.max(inputBranch1.getIdIndex(), inputBranch2.getIdIndex()));
    	
    	Commit inputCommit1 = inputBranch1.getLatestCommit();
    	Commit inputCommit2 = inputBranch2.getLatestCommit();
    	
    	Commit outputCommit = outputBranch.createCommit();
    	
    	Node startNode1 = null;
    	Node startNode2 = null;
    	
    	if (inputCommit1 != null) {
    		outputBranch.addCommit(inputCommit1);
    		outputCommit.addPredecessor(inputCommit1);
    		startNode1 = inputCommit1.getSnapshot().getStartNode();
    	}
    	if (inputCommit2 != null) {
    		outputBranch.addCommit(inputCommit2);
    		outputCommit.addPredecessor(inputCommit2);
    		startNode2 = inputCommit2.getSnapshot().getStartNode();
    	}
    	
    	Snapshot outputSnapshot = outputCommit.createSnapshot();
    	
    	HashMap<String, Set<Node>> inputToOutputNodes1 = new HashMap<String, Set<Node>>();
		HashMap<String, Set<Node>> inputToOutputNodes2 = new HashMap<String, Set<Node>>();
    	if (startNode1 != null && startNode2 != null) {
    		mergeNodeSpaces(startNode1, startNode2, outputSnapshot, inputToOutputNodes1, inputToOutputNodes2, 
    				isIntersection, isStrong);
    	}
   
    	if (startNode1 != null) {
    		mergeNodeSpace(startNode1, outputSnapshot, inputToOutputNodes1);
    	}

    	if (startNode2 != null) {
    		mergeNodeSpace(startNode2, outputSnapshot, inputToOutputNodes2);
    	}
    	
    	designSpaceRepository.save(targetSpace);
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void orBranches(String targetSpaceID, String inputBranchID1, String inputBranchID2, String outputBranchID) {
    	if (outputBranchID == null) {
    		outputBranchID = RESERVED_ID;
    	}
    	
    	indexVersionMerger(targetSpaceID, inputBranchID1);
    	indexVersionMerger(targetSpaceID, inputBranchID2);
    	
    	mergeBranch(targetSpaceID, inputBranchID1, outputBranchID);
    	mergeBranch(targetSpaceID, inputBranchID2, outputBranchID);
    	createCommit(targetSpaceID, outputBranchID);
    	
    	unionSnapshot(targetSpaceID, inputBranchID1, outputBranchID);
    	unionSnapshot(targetSpaceID, inputBranchID2, outputBranchID);
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    
    	Node startNodePrime = createTypedNode(targetSpaceID, outputBranchID, Node.NodeType.START.getValue());
    	
    	for (Node startNode : getStartNodes(targetSpaceID, outputBranchID)) {
    		if (!startNode.getNodeID().equals(startNodePrime.getNodeID())) {
    			deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
        		createEdge(targetSpaceID, outputBranchID, startNodePrime.getNodeID(), startNode.getNodeID());
    		}
    	}
        
    	Node acceptNodePrime = createTypedNode(targetSpaceID, outputBranchID, Node.NodeType.ACCEPT.getValue());
    	
    	for (Node acceptNode : getAcceptNodes(targetSpaceID, outputBranchID)) {
    		if (!acceptNode.getNodeID().equals(acceptNodePrime.getNodeID())) {
    			deleteNodeType(targetSpaceID, outputBranchID, acceptNode.getNodeID());
    			createEdge(targetSpaceID, outputBranchID, acceptNode.getNodeID(), acceptNodePrime.getNodeID());
    		}
    	}
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		fastForwardBranch(targetSpaceID, inputBranchID1, RESERVED_ID);
        	fastForwardBranch(targetSpaceID, inputBranchID2, RESERVED_ID);
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void resetHeadBranch(String targetSpaceID, String targetCommitID) {
    	validateDesignSpaceOperator(targetSpaceID);
    	designSpaceRepository.resetHeadBranch(targetSpaceID, targetCommitID);
    }
   
    public void deleteDesignSpace(String targetSpaceID) {
    	validateDesignSpaceOperator(targetSpaceID);
    	
    	designSpaceRepository.deleteDesignSpace(targetSpaceID);
    }
    
    public void createDesignSpace(String outputSpaceID) {
    	validateGenerativeDesignSpaceOperator(outputSpaceID);
    	
    	designSpaceRepository.createDesignSpace(outputSpaceID);
    }
    
    public void createDesignSpace(String outputSpaceID, String compID, Set<String> compRoles) {
    	validateGenerativeDesignSpaceOperator(outputSpaceID);
    	
    	designSpaceRepository.createDesignSpace(outputSpaceID, compID, new ArrayList<String>(compRoles));
    }
    
    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return mapDesignSpaceToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public void insertDesignSpace(String inputSpaceID1, String inputSpaceID2, String targetNodeID, String outputSpaceID)
    		throws NodeNotFoundException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateNodeOperator(inputSpaceID1, targetNodeID);
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	Node startNode2 = null;
    	Set<Node> acceptNodes2 = new HashSet<Node>();

    	Node targetNode;

    	if (outputSpaceID.equals(inputSpaceID1)) {
    		Node startNode1 = getStartNode(outputSpaceID);
    		Set<Node> acceptNodes1 = getAcceptNodes(outputSpaceID);

    		unionDesignSpace(inputSpaceID2, outputSpaceID);

    		for (Node startNode : getStartNodes(outputSpaceID)) {
    			if (!startNode1.equals(startNode)) {
    				startNode2 = startNode;
    			}
    		}
    		for (Node acceptNode : getAcceptNodes(outputSpaceID)) {
    			if (!acceptNodes1.contains(acceptNode)) {
    				acceptNodes2.add(acceptNode);
    			}
    		}

    		targetNode = findNode(outputSpaceID, targetNodeID);
    	} else {
    		if (!outputSpaceID.equals(inputSpaceID2)) {
    			unionDesignSpace(inputSpaceID2, outputSpaceID);
    		}

    		startNode2 = getStartNode(outputSpaceID);
    		acceptNodes2.addAll(getAcceptNodes(outputSpaceID));

    		unionDesignSpace(inputSpaceID1, outputSpaceID);

    		targetNode = findNodeCopy(inputSpaceID1, targetNodeID, outputSpaceID);
    	}

    	deleteNodeCopyIndices(outputSpaceID);

    	Set<Edge> removedEdges = removeOutgoingEdges(outputSpaceID, targetNode.getNodeID());

    	if (removedEdges.size() > 0) {
    		for (Node acceptNode2 : acceptNodes2) {
    			deleteNodeType(outputSpaceID, acceptNode2.getNodeID());
    		}
    	} 

    	if (startNode2 != null) {
    		deleteNodeType(outputSpaceID, startNode2.getNodeID());
    		createEdge(outputSpaceID, targetNode.getNodeID(), startNode2.getNodeID());
    	}
    	for (Node acceptNode2 : acceptNodes2) {
    		for (Edge removedEdge : removedEdges) {
    			if (removedEdge.hasComponentIDs() && removedEdge.hasComponentRoles()) {
    				createComponentEdge(outputSpaceID, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID(), removedEdge.getComponentIDs(), removedEdge.getComponentRoles());
    			} else {
    				createEdge(outputSpaceID, acceptNode2.getNodeID(), removedEdge.getHead().getNodeID());
    			}
    		}
    	}

    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}

    	insertBranch(outputSpaceID, headBranchID1, headBranchID2, targetNodeID, null);

    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    public void joinDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) 
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	Set<Node> acceptNodes1 = new HashSet<Node>();
    	Node startNode2 = null;
    	
    	if (outputSpaceID.equals(inputSpaceID2)) {
    		startNode2 = getStartNode(outputSpaceID);
    		Set<Node> acceptNodes2 = getAcceptNodes(outputSpaceID);
    		
    		unionDesignSpace(inputSpaceID1, outputSpaceID);
    		
    		for (Node acceptNode : getAcceptNodes(outputSpaceID)) {
    			if (!acceptNodes2.contains(acceptNode)) {
    				acceptNodes1.add(acceptNode);
    			}
    		}
    	} else {
    		if (!outputSpaceID.equals(inputSpaceID1)) {
        		unionDesignSpace(inputSpaceID1, outputSpaceID);
        	}
    		
    		Node startNode1 = getStartNode(outputSpaceID);
        	acceptNodes1.addAll(getAcceptNodes(outputSpaceID));
    		
    		unionDesignSpace(inputSpaceID2, outputSpaceID);

    		for (Node startNode : getStartNodes(outputSpaceID)) {
    			if (!startNode1.equals(startNode)) {
    				startNode2 = startNode;
    			}
    		}	
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	if (startNode2 != null) {
    		deleteNodeType(outputSpaceID, startNode2.getNodeID());
    		for (Node acceptNode1 : acceptNodes1) {
    			deleteNodeType(outputSpaceID, acceptNode1.getNodeID());
    			createEdge(outputSpaceID, acceptNode1.getNodeID(), startNode2.getNodeID());
    		}
    	}

    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}

    	joinBranches(outputSpaceID, headBranchID1, headBranchID2, null);
    	
    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    public void mergeDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID, 
    		boolean isIntersection, boolean isStrong) 
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	DesignSpace inputSpace1 = loadDesignSpace(inputSpaceID1, 2);
    	DesignSpace inputSpace2 = loadDesignSpace(inputSpaceID2, 2);
    	
    	DesignSpace outputSpace;
    	
    	if (outputSpaceID.equals(inputSpaceID1) || outputSpaceID.equals(inputSpaceID2)) {
    		outputSpace = new DesignSpace(RESERVED_ID, 0, 
    				Math.max(inputSpace1.getMergeIndex(), inputSpace2.getMergeIndex()));
    	} else {
    		outputSpace = new DesignSpace(outputSpaceID, 0, 
    				Math.max(inputSpace1.getMergeIndex(), inputSpace2.getMergeIndex()));
    	}
    	
    	Node startNode1 = inputSpace1.getStartNode();
    	Node startNode2 = inputSpace2.getStartNode();
    	
    	HashMap<String, Set<Node>> inputToOutputNodes1 = new HashMap<String, Set<Node>>();
		HashMap<String, Set<Node>> inputToOutputNodes2 = new HashMap<String, Set<Node>>();
		
    	if (startNode1 != null && startNode2 != null) {
    		mergeNodeSpaces(startNode1, startNode2, outputSpace, inputToOutputNodes1, inputToOutputNodes2, 
    				isIntersection, isStrong);
    	}
    	
    	if (!isIntersection && startNode1 != null) {
    		mergeNodeSpace(startNode1, outputSpace, inputToOutputNodes1);
    	}

    	if (!isIntersection && startNode2 != null) {
    		mergeNodeSpace(startNode2, outputSpace, inputToOutputNodes2);
    	}
    	
    	designSpaceRepository.save(outputSpace);
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	mergeVersionHistory(inputSpaceID1, outputSpace.getSpaceID());
    	indexVersionMerger(outputSpace.getSpaceID(), headBranchID1);
    	mergeVersionHistory(inputSpaceID2, outputSpace.getSpaceID());
    	indexVersionMerger(outputSpace.getSpaceID(), headBranchID2);

    	mergeBranches(outputSpace.getSpaceID(), headBranchID1, headBranchID2, null, isIntersection, isStrong);
    	
    	selectHeadBranch(outputSpace.getSpaceID(), headBranchID1);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		deleteDesignSpace(inputSpaceID1);
    		renameDesignSpace(RESERVED_ID, inputSpaceID1);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		deleteDesignSpace(inputSpaceID2);
    		renameDesignSpace(RESERVED_ID, inputSpaceID2);
    	}
    }
    
    private void mergeNodeSpace(Node inputNode, NodeSpace outputSpace, 
    		HashMap<String, Set<Node>> inputToOutputNodes) {
    	HashMap<String, Node> inputToSurplusOutputNodes = new HashMap<String, Node>();
    	Set<Node> visitedInputNodes = new HashSet<Node>();
    	
    	Stack<Node> nodeStack = new Stack<Node>();
    	nodeStack.push(inputNode);
    	
    	while (nodeStack.size() > 0) {
    		
    		inputNode = nodeStack.pop();
    		
    		visitedInputNodes.add(inputNode);
    		
    		if (inputNode.hasEdges()) {
    			for (Edge inputEdge : inputNode.getEdges()) {
    				if (inputEdge.getTail().equals(inputNode)) {
    					Node inputSuccessor = inputEdge.getHead();
    					
    					if (!visitedInputNodes.contains(inputSuccessor)) {
    						nodeStack.push(inputSuccessor);
    					}
    					
    					Node outputSuccessor = null;

    					if (inputToSurplusOutputNodes.containsKey(inputSuccessor.getNodeID())) {
    						outputSuccessor = inputToSurplusOutputNodes.get(inputSuccessor);
    					} else if (!inputToOutputNodes.containsKey(inputSuccessor.getNodeID())) {
    						outputSuccessor = outputSpace.copyNode(inputSuccessor);
    						inputToSurplusOutputNodes.put(inputSuccessor.getNodeID(), outputSuccessor);
    					}

    					if (outputSuccessor != null) {
    						if (inputToOutputNodes.containsKey(inputNode.getNodeID())) {
    							for (Node outputNode : inputToOutputNodes.get(inputNode.getNodeID())) {
    								outputNode.addEdge(new Edge(outputNode, outputSuccessor, 
    										inputEdge.getComponentIDs(), inputEdge.getComponentRoles()));
    							}
    						} else if (inputToSurplusOutputNodes.containsKey(inputNode.getNodeID())) {
    							Node outputNode = inputToSurplusOutputNodes.get(inputNode.getNodeID());
    							outputNode.addEdge(new Edge(outputNode, outputSuccessor, 
    									inputEdge.getComponentIDs(), inputEdge.getComponentRoles()));
    						}
    					}
    				}
    			}
    		}
    	}
    }
    
    private Node mergeNodes(Node inputNode1, Node inputNode2, NodeSpace outputSpace,
    		HashMap<String, Node> mergedInputsToOutputNodes, 
    		HashMap<String, Set<Node>> inputToOutputNodes1, HashMap<String, Set<Node>> inputToOutputNodes2) {
    	Node outputNode;
    	String mergedInputIDs = inputNode1.getNodeID() + inputNode2.getNodeID();
		if (mergedInputsToOutputNodes.containsKey(mergedInputIDs)) {
			outputNode = mergedInputsToOutputNodes.get(mergedInputIDs);
		} else {
			if (inputNode1.hasNodeType() || !inputNode2.hasNodeType()) {
				outputNode = outputSpace.copyNode(inputNode1);
			} else {
				outputNode = outputSpace.copyNode(inputNode2);
			}

			if (!inputToOutputNodes1.containsKey(inputNode1.getNodeID())) {
				inputToOutputNodes1.put(inputNode1.getNodeID(), new HashSet<Node>());
			}
			inputToOutputNodes1.get(inputNode1.getNodeID()).add(outputNode);
			if (!inputToOutputNodes2.containsKey(inputNode2.getNodeID())) {
				inputToOutputNodes2.put(inputNode2.getNodeID(), new HashSet<Node>());
			}
			inputToOutputNodes2.get(inputNode2.getNodeID()).add(outputNode);

			mergedInputsToOutputNodes.put(inputNode1.getNodeID() + inputNode2.getNodeID(), outputNode);
		}
    	return outputNode;
    }
    
    private void mergeNodeSpaces(Node inputNode1, Node inputNode2, NodeSpace outputSpace,
    		HashMap<String, Set<Node>> inputToOutputNodes1, HashMap<String, Set<Node>> inputToOutputNodes2, 
    		boolean isIntersection, boolean isStrong) {
    	HashMap<String, Node> mergedInputsToOutputNodes = new HashMap<String, Node>();
    	
    	Stack<Node> nodeStack1 = new Stack<Node>();
    	nodeStack1.push(inputNode1);
    	Stack<Node> nodeStack2 = new Stack<Node>();
    	nodeStack2.push(inputNode2);
    	
    	while (nodeStack1.size() > 0 && nodeStack2.size() > 0) {
    		inputNode1 = nodeStack1.pop();
    		inputNode2 = nodeStack2.pop();
    		
    		Node outputNode = mergeNodes(inputNode1, inputNode2, outputSpace, mergedInputsToOutputNodes, 
        			inputToOutputNodes1, inputToOutputNodes2);
    		
    		if (inputNode1.hasEdges() && inputNode2.hasEdges()) {
    			for (Edge edge1 : inputNode1.getEdges()) {
    				if (edge1.getTail().equals(inputNode1)) {
    					for (Edge edge2 : inputNode2.getEdges()) {
    						if (edge2.getTail().equals(inputNode2) && 
    								(isStrong && edge1.hasSameComponentRoles(edge2) 
    								|| !isStrong && edge1.hasSameComponents(edge2))) {
    							if (!mergedInputsToOutputNodes.containsKey(edge1.getHead().getNodeID() 
    									+ edge2.getHead().getNodeID())) {
    								nodeStack1.push(edge1.getHead());
    								nodeStack2.push(edge2.getHead());
    							}
    							
    							Node outputSuccessor = mergeNodes(edge1.getHead(), edge2.getHead(), 
    									outputSpace, mergedInputsToOutputNodes, 
    									inputToOutputNodes1, inputToOutputNodes2);

    							if (isIntersection) {
    								outputNode.addEdge(intersectEdges(edge1, edge2, outputNode, outputSuccessor));
    							} else {
    								outputNode.addEdge(unionEdges(edge1, edge2, outputNode, outputSuccessor));
    							}
    						}
    					}
    				}
    			}
    		}
    	}
    }
    
    public void minimizeDesignSpace(String targetSpaceID) 
    		throws DesignSpaceNotFoundException {
    	validateDesignSpaceOperator(targetSpaceID);

    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 2);
    	
    	HashMap<String, Set<Edge>> nodeIDToIncomingEdges = targetSpace.mapNodeIDsToIncomingEdges();
    	
    	HashMap<String, Node> nodeIDToNode = targetSpace.mapNodeIDsToNodes();
    	
    	Stack<Node> nodeStack = new Stack<Node>();
    	for (Node node : targetSpace.getNodes()) {
    		nodeStack.push(node);
    	}
    	
    	Set<String> deletedNodeIDs = new HashSet<String>();
    	
    	while (nodeStack.size() > 0) {
    		Node node = nodeStack.peek();
    		
    		Set<Edge> minimizableEdges = targetSpace.getMinimizableEdges(node, nodeIDToIncomingEdges);
    		
    		if (minimizableEdges.size() == 0 || deletedNodeIDs.contains(node.getNodeID())) {
    			nodeStack.pop();
    		} else {
    			for (Edge minimizableEdge : minimizableEdges) {
    				Node predecessor = nodeIDToNode.get(minimizableEdge.getTail().getNodeID());
    				
    				deletedNodeIDs.add(predecessor.getNodeID());
    				
    				if (nodeIDToIncomingEdges.containsKey(predecessor.getNodeID())) {
    					for (Edge incomingEdge : nodeIDToIncomingEdges.get(predecessor.getNodeID())) {
    						incomingEdge.setHead(node);
    						nodeIDToIncomingEdges.get(node.getNodeID()).add(incomingEdge);
    					}
    				}
    				
    				if (minimizableEdges.size() == 1) {
    					for (Edge edge : predecessor.getEdges()) {
    						if (!edge.equals(minimizableEdge)) {
    							node.copyEdge(edge);
    						}
    					}
    				}

    				if (predecessor.hasNodeType()) {
    					node.setNodeType(predecessor.getNodeType());
    				}
    			}
    			
    			nodeIDToIncomingEdges.get(node.getNodeID()).removeAll(minimizableEdges);
    		}
    	}
    	
    	Set<Node> deletedNodes = targetSpace.removeNodesByID(deletedNodeIDs);
    	
    	designSpaceRepository.save(targetSpace);
    	
    	nodeRepository.delete(deletedNodes);
    }
    
    public void orDesignSpaces(String inputSpaceID1, String inputSpaceID2, String outputSpaceID) 
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	
    	if (!outputSpaceID.equals(inputSpaceID1)) {
    		unionDesignSpace(inputSpaceID1, outputSpaceID);
    	}
    	if (!outputSpaceID.equals(inputSpaceID2)) {
    		unionDesignSpace(inputSpaceID2, outputSpaceID);
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);

    	Set<Node> startNodes = getStartNodes(outputSpaceID);
    
    	Node startNodePrime = createTypedNode(outputSpaceID, Node.NodeType.START.getValue());
    	
    	for (Node startNode : startNodes) {
    		deleteNodeType(outputSpaceID, startNode.getNodeID());
    		createEdge(outputSpaceID, startNodePrime.getNodeID(), startNode.getNodeID());
    	}
    	
    	Set<Node> acceptNodes = getAcceptNodes(outputSpaceID);
        
    	Node acceptNodePrime = createTypedNode(outputSpaceID, Node.NodeType.ACCEPT.getValue());
    	
    	for (Node acceptNode : acceptNodes) {
    		deleteNodeType(outputSpaceID, acceptNode.getNodeID());
    		createEdge(outputSpaceID, acceptNode.getNodeID(), acceptNodePrime.getNodeID());
    	}
    	
    	String headBranchID1 = getHeadBranchID(inputSpaceID1);
    	String headBranchID2 = getHeadBranchID(inputSpaceID2);
    	
    	if (outputSpaceID.equals(inputSpaceID1)) {
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	} else if (outputSpaceID.equals(inputSpaceID2)) {
    		indexVersionMerger(outputSpaceID, headBranchID2);
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    	} else {
    		mergeVersionHistory(inputSpaceID1, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID1);
    		mergeVersionHistory(inputSpaceID2, outputSpaceID);
    		indexVersionMerger(outputSpaceID, headBranchID2);
    	}
    	
    	orBranches(outputSpaceID, headBranchID1, headBranchID2, null);

    	if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2)) {
    		selectHeadBranch(outputSpaceID, headBranchID1);
    	}
    }
    
    private void copyDesignSpaceToSnapshot(String inputSpaceID, String outputBranchID) {
		designSpaceRepository.copyDesignSpaceToSnapshot(inputSpaceID, outputBranchID);
	}

	private void copySnapshots(String inputSpaceID, String inputBranchID, String outputSpaceID, String outputBranchID) {
	    designSpaceRepository.copySnapshots(inputSpaceID, inputBranchID, outputSpaceID, outputBranchID);
	}

	public void createEdge(String targetSpaceID, String targetTailID, String targetHeadID) {
    	designSpaceRepository.createEdge(targetSpaceID, targetTailID, targetHeadID);
    }
	
	public void createNode(String targetSpaceID) {
    	designSpaceRepository.createNode(targetSpaceID);
	}
    
	private void createCommit(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.createCommit(targetSpaceID, targetBranchID);
	}

	private void createComponentEdge(String targetSpaceID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		designSpaceRepository.createComponentEdge(targetSpaceID, targetTailID, targetHeadID, componentIDs, componentRoles);
	}

	private void createComponentEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		designSpaceRepository.createComponentEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID, componentIDs, componentRoles);
	}

	private void createEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID) {
		designSpaceRepository.createEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID);
	}

	private Node createTypedNode(String targetSpaceID, String nodeType) {
		Set<Node> typedNodes = designSpaceRepository.createTypedNode(targetSpaceID, nodeType);
		if (typedNodes.size() > 0) {
			return typedNodes.iterator().next();
		} else {
			return null;
		}
	}

	private Node createTypedNode(String targetSpaceID, String targetBranchID, String nodeType) {
		Set<Node> typedNodes = designSpaceRepository.createTypedNode(targetSpaceID, targetBranchID, nodeType);
		if (typedNodes.size() > 0) {
			return typedNodes.iterator().next();
		} else {
			return null;
		}
	}

	private void deleteCommitCopyIndices(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteCommitCopyIndices(targetSpaceID, targetBranchID);
	}

	private void deleteNodeCopyIndices(String targetSpaceID) {
    	designSpaceRepository.deleteNodeCopyIndices(targetSpaceID);
    }
    
    private void deleteNodeCopyIndices(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteNodeCopyIndices(targetSpaceID, targetBranchID);
	}

	private void deleteNodes(String targetSpaceID) {
		designSpaceRepository.deleteNodes(targetSpaceID);
	}

	private void deleteNodeType(String targetSpaceID, String targetNodeID) {
		designSpaceRepository.deleteNodeType(targetSpaceID, targetNodeID);
	}

	private void deleteNodeType(String targetSpaceID, String targetBranchID, String targetNodeID) {
		designSpaceRepository.deleteNodeType(targetSpaceID, targetBranchID, targetNodeID);
	}

	private void fastForwardBranch(String targetSpaceID, String targetBranchID1, String targetBranchID2) {
		designSpaceRepository.fastForwardBranch(targetSpaceID, targetBranchID1, targetBranchID2);
	}
	
	private Branch findBranch(String targetSpaceID, String targetBranchID) {
		Set<Branch> targetBranches = designSpaceRepository.findBranch(targetSpaceID, targetBranchID);
		if (targetBranches.size() > 0) {
			return targetBranches.iterator().next();
		} else {
			return null;
		}
	}

	private DesignSpace findDesignSpace(String targetSpaceID) {
    	return designSpaceRepository.findBySpaceID(targetSpaceID);
    }
	
	private DesignSpace loadDesignSpace(String targetSpaceID, int depth) {
		return designSpaceRepository.findOne(getGraphID(targetSpaceID), depth);
	}
	
	private Node findNode(String targetSpaceID, String targetNodeID) {
		Set<Node> targetNodes = designSpaceRepository.findNode(targetSpaceID, targetNodeID);
		if (targetNodes.size() > 0) {
			return targetNodes.iterator().next();
		} else {
			return null;
		}
	}
    
    private Node findNodeCopy(String targetSpaceID1, String targetNodeID, String targetSpaceID2) {
		Set<Node> nodeCopies = designSpaceRepository.findNodeCopy(targetSpaceID1, targetNodeID, targetSpaceID2);
		if (nodeCopies.size() > 0) {
			return nodeCopies.iterator().next();
		} else {
			return null;
		}
	}

	private Node findNodeCopy(String targetSpaceID, String targetBranchID1, String targetNodeID, String targetBranchID2) {
		Set<Node> nodeCopies = designSpaceRepository.findNodeCopy(targetSpaceID, targetBranchID1, targetNodeID, targetBranchID2);
		if (nodeCopies.size() > 0) {
			return nodeCopies.iterator().next();
		} else {
			return null;
		}
	}

	private Set<String> getBranchIDs(String targetSpaceID) {
		return designSpaceRepository.getBranchIDs(targetSpaceID);
	}
	
	private Set<String> getCommonBranchIDs(String targetSpaceID1, String targetSpaceID2) {
		return designSpaceRepository.getCommonBranchIDs(targetSpaceID1, targetSpaceID2);
	}

	private String getHeadBranchID(String targetSpaceID) {
		Set<String> headBranchIDs = designSpaceRepository.getHeadBranchID(targetSpaceID);
		if (headBranchIDs.size() > 0) {
			return headBranchIDs.iterator().next();
		} else {
			return null;
		}
	}
	
	private Long getGraphID(String targetSpaceID) {
		Set<Integer> graphIDs = designSpaceRepository.getGraphID(targetSpaceID);
		if (graphIDs.size() > 0) {
			return new Long(graphIDs.iterator().next());
		} else {
			return null;
		}
	}
	
	private Set<Node> getAcceptNodes(String targetSpaceID) {
		return getNodesByType(targetSpaceID, Node.NodeType.ACCEPT.getValue());
	}
	
	private Set<Node> getAcceptNodes(String targetSpaceID, String targetBranchID) {
		return getNodesByType(targetSpaceID, targetBranchID, Node.NodeType.ACCEPT.getValue());
	}

	private Set<Node> getNodesByType(String targetSpaceID, String nodeType) {
		return designSpaceRepository.getNodesByType(targetSpaceID, nodeType);
	}

	private Set<Node> getNodesByType(String targetSpaceID, String targetBranchID, String nodeType) {
		return designSpaceRepository.getNodesByType(targetSpaceID, targetBranchID, nodeType);
	}
	
	private Set<Edge> getOutgoingEdges(String targetSpaceID, String targetNodeID) {
		return designSpaceRepository.getOutgoingEdges(targetSpaceID, targetNodeID);
	}

	private Set<Edge> getOutgoingEdges(String targetSpaceID, String targetBranchID, String targetNodeID) {
		return designSpaceRepository.getOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
	}
	
	private Node getStartNode(String targetSpaceID) {
		Set<Node> startNodes = getNodesByType(targetSpaceID, Node.NodeType.START.getValue());
		if (startNodes.size() > 0) {
			return startNodes.iterator().next();
		} else {
			return null;
		}
	}
	
	private Node getStartNode(String targetSpaceID, String targetBranchID) {
		Set<Node> startNodes = getNodesByType(targetSpaceID, targetBranchID, Node.NodeType.START.getValue());
		if (startNodes.size() > 0) {
			return startNodes.iterator().next();
		} else {
			return null;
		}
	}
	
	private Set<Node> getStartNodes(String targetSpaceID) {
		return getNodesByType(targetSpaceID, Node.NodeType.START.getValue());
	}
	
	private Set<Node> getStartNodes(String targetSpaceID, String targetBranchID) {
		return getNodesByType(targetSpaceID, targetBranchID, Node.NodeType.START.getValue());
	}
	
	public boolean hasBranch(String targetSpaceID, String targetBranchID) {
		return findBranch(targetSpaceID, targetBranchID) != null;
	}
	
	public boolean hasCommonBranches(String targetSpaceID1, String targetSpaceID2) {
		return getCommonBranchIDs(targetSpaceID1, targetSpaceID2).size() > 0;
	}
	
	public boolean hasDesignSpace(String targetSpaceID) {
		return findDesignSpace(targetSpaceID) != null;
	}
	
	public boolean hasNode(String targetSpaceID, String targetNodeID) {
		return findNode(targetSpaceID, targetNodeID) != null;
	}
	
	private void indexVersionMerger(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.indexVersionMerger(targetSpaceID, targetBranchID);
	}
	
	private Edge intersectEdges(Edge edge1, Edge edge2, Node tail, Node head) {
    	Set<String> intersectedCompIDs = new HashSet<String>();
    	Set<String> compIDs1 = new HashSet<String>();
    	
    	if (edge1.hasComponentIDs()) {
    		compIDs1.addAll(edge1.getComponentIDs());
    		
    		if (edge2.hasComponentIDs()) {
    			for (String compID : edge2.getComponentIDs()) {
    				if (compIDs1.contains(compID)) {
    					intersectedCompIDs.add(compID);
    				}
    			}
    		}
    	}
    	
    	Set<String> intersectedCompRoles = new HashSet<String>();
    	Set<String> compRoles1 = new HashSet<String>();
    	
    	if (edge1.hasComponentRoles()) {
    		compRoles1.addAll(edge1.getComponentRoles());
    		
    		if (edge2.hasComponentRoles()) {
    			for (String compRole : edge2.getComponentRoles()) {
    				if (compRoles1.contains(compRole)) {
    					intersectedCompRoles.add(compRole);
    				}
    			}
    		}
    	}
    	if (intersectedCompIDs.size() > 0) {
    		return new Edge(tail, head, new ArrayList<String>(intersectedCompIDs), 
        			new ArrayList<String>(intersectedCompRoles));
    	} else {
    		return new Edge(tail, head);
    	}
    	
    }
	
	private Map<String, Object> mapBranchesToD3Format(List<Map<String, Object>> branchMap) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
	
	    if (branchMap.size() > 0) {
	    	List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	        List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	        
	        Map<Map<String, Object>, Integer> nodeAddresses = new HashMap<Map<String, Object>, Integer>();
	        Set<String> branchIDs = new HashSet<String>();
	    	
	    	d3Graph.put("spaceID", branchMap.get(0).get("spaceID"));
	    	
	    	Map<String, Object> tail = makeD3("knoxID", "head", "knoxClass", "Head");
	    	Map<String, Object> head = makeD3("knoxID", branchMap.get(0).get("headBranchID"), "knoxClass", "Branch");
	    	
	    	links.add(makeLink(tail, head, nodes, nodeAddresses));
	    	
	    	for (Map<String, Object> row : branchMap) {
	    		String tailID = (String) row.get("tailID");
	    		String headID = (String) row.get("headID");
	    		if (tailID != null && headID != null) {
	    			tail = makeD3("knoxID", row.get("tailID"), "knoxClass", "Commit");
	    			tail.put("copyIndex", row.get("tailCopyIndex"));
	    			head = makeD3("knoxID", row.get("headID"), "knoxClass", "Commit");
	    			head.put("copyIndex", row.get("headCopyIndex"));
	    			links.add(makeLink(tail, head, nodes, nodeAddresses));
	    		}
	
	    		String branchID = (String) row.get("branchID");
	    		if (!branchIDs.contains(branchID) && row.get("latestCommitID") != null) {
	    			branchIDs.add(branchID);
	    			tail = makeD3("knoxID", branchID, "knoxClass", "Branch");
	    			head = makeD3("knoxID", row.get("latestCommitID"), "knoxClass", "Commit");
	    			head.put("copyIndex", row.get("latestCopyIndex"));
	    			links.add(makeLink(tail, head, nodes, nodeAddresses));
	    		}
	    	}
	    	for (Map<String, Object> node : nodes) {
	    		node.remove("copyIndex");
	    	}
	    	d3Graph.putAll(makeD3("nodes", nodes, "links", links));
	    }
	    return d3Graph;
	}

	private Map<String, Object> mapDesignSpaceToD3Format(List<Map<String, Object>> spaceMap) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
	    List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	    List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	    int i = 0;
	    for (Map<String, Object> row : spaceMap) {
	        if (d3Graph.isEmpty()) {
	        	d3Graph.put("spaceID", row.get("spaceID"));
	        }
	        Map<String, Object> tail = makeD3("nodeID", row.get("tailID"), "nodeType", row.get("tailType"));
	        int source = nodes.indexOf(tail);
	        if (source == -1) {
	        	nodes.add(tail);
	        	source = i++;
	        }
	        Map<String, Object> head = makeD3("nodeID", row.get("headID"), "nodeType", row.get("headType"));
	        int target = nodes.indexOf(head);
	        if (target == -1) {
	        	nodes.add(head);
	        	target = i++;
	        }
	        Map<String, Object> link = makeD3("source", source, "target", target);
	        if (row.containsKey("componentRoles") && row.get("componentRoles") != null) {
	        	link.put("componentRoles", row.get("componentRoles"));
	        }
	        links.add(link);
	    }
	    d3Graph.putAll(makeD3("nodes", nodes, "links", links));
	    return d3Graph;
	}

	private Map<String, Object> makeD3(String key1, Object value1, String key2, Object value2) {
	    Map<String, Object> result = new HashMap<String, Object>();
	    result.put(key1, value1);
	    result.put(key2, value2);
	    return result;
	}

	private Map<String, Object> makeLink(Map<String, Object> tail, Map<String, Object> head, List<Map<String,Object>> nodes, Map<Map<String, Object>, Integer> nodeAddresses) {
		 int source;
	     if (nodeAddresses.containsKey(tail)) {
	     	source = nodeAddresses.get(tail);
	     } else {
	     	source = nodes.size();
	     	nodes.add(tail);
	     	nodeAddresses.put(tail, source);
	     }
	     int target;
	     if (nodeAddresses.containsKey(head)) {
	     	target = nodeAddresses.get(head);
	     } else {
	     	target = nodes.size();
	     	nodes.add(head);
	     	nodeAddresses.put(head, target);
	     }
	     return makeD3("source", source, "target", target);
	}

	private void mergeBranch(String targetSpaceID, String inputBranchID, String outputBranchID) {
	    designSpaceRepository.mergeBranch(targetSpaceID, inputBranchID, outputBranchID);
	}

	private void mergeBranch(String inputSpaceID, String inputBranchID, String outputSpaceID, String outputBranchID) {
	    designSpaceRepository.mergeBranch(inputSpaceID, inputBranchID, outputSpaceID, outputBranchID);
	}
	
	private void mergeVersionHistory(String inputSpaceID, String outputSpaceID) {
    	Set<String> inputBranchIDs = getBranchIDs(inputSpaceID);
    	for (String inputBranchID : inputBranchIDs) {
    		mergeBranch(inputSpaceID, inputBranchID, outputSpaceID, inputBranchID);
    		copySnapshots(inputSpaceID, inputBranchID, outputSpaceID, inputBranchID);
    	}
    	for (String inputBranchID : inputBranchIDs) {
    		deleteCommitCopyIndices(outputSpaceID, inputBranchID);
    	}
    }
	
	private Set<Edge> removeOutgoingEdges(String targetSpaceID, String targetNodeID) {
		Set<Edge> removedEdges = getOutgoingEdges(targetSpaceID, targetNodeID);
		if (removedEdges.size() > 0) {
			designSpaceRepository.deleteOutgoingEdges(targetSpaceID, targetNodeID);
		}
		return removedEdges;
	}

	private Set<Edge> removeOutgoingEdges(String targetSpaceID, String targetBranchID, String targetNodeID) {
		Set<Edge> removedEdges = getOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
		if (removedEdges.size() > 0) {
			designSpaceRepository.deleteOutgoingEdges(targetSpaceID, targetBranchID, targetNodeID);
		}
		return removedEdges;
	}
	
	private void renameDesignSpace(String targetSpaceID, String outputSpaceID) {
    	designSpaceRepository.renameDesignSpace(targetSpaceID, outputSpaceID);
    }

	private void selectHeadBranch(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.selectHeadBranch(targetSpaceID, targetBranchID);
	}

	private void unionDesignSpace(String inputSpaceID, String outputSpaceID) {
        designSpaceRepository.unionDesignSpace(inputSpaceID, outputSpaceID);
    }
	
	private Edge unionEdges(Edge edge1, Edge edge2, Node tail, Node head) {
    	Set<String> mergedCompIDs = new HashSet<String>();
    	Set<String> mergedCompRoles = new HashSet<String>();
    	
    	if (edge1.hasComponentIDs()) {
    		mergedCompIDs.addAll(edge1.getComponentIDs());
    	}
    	
    	if (edge2.hasComponentIDs()) {
    		mergedCompIDs.addAll(edge2.getComponentIDs());
    	}
    	
    	if (edge1.hasComponentRoles()) {
    		mergedCompRoles.addAll(edge1.getComponentRoles());
    	}
    	
    	if (edge2.hasComponentRoles()) {
    		mergedCompRoles.addAll(edge2.getComponentRoles());
    	}
    	
    	if (mergedCompIDs.size() > 0) {
    		return new Edge(tail, head, new ArrayList<String>(mergedCompIDs), 
        			new ArrayList<String>(mergedCompRoles));
    	} else {
    		return new Edge(tail, head);
    	}
    }
    
    private void unionSnapshot(String targetSpaceID, String inputBranchID, String outputBranchID) {
        designSpaceRepository.unionSnapshot(targetSpaceID, inputBranchID, outputBranchID);
    }
    
    private void validateNodeOperator(String targetSpaceID, String targetNodeID) 
    		throws NodeNotFoundException {
    	if (!hasNode(targetSpaceID, targetNodeID)) {
    		throw new NodeNotFoundException(targetSpaceID, targetNodeID);
    	}
    }
    
    private void validateDesignSpaceOperator(String targetSpaceID) {
    	if (!hasDesignSpace(targetSpaceID)) {
    		throw new DesignSpaceNotFoundException(targetSpaceID);
    	}
    }
    
    private void validateGenerativeDesignSpaceOperator(String outputSpaceID) {
    	if (hasDesignSpace(outputSpaceID)) {
    		throw new DesignSpaceConflictException(outputSpaceID);
    	}
    }
    
    private void validateCombinationalDesignSpaceOperator(String inputSpaceID1, String inputSpaceID2, String outputSpaceID)
    		throws DesignSpaceNotFoundException, DesignSpaceConflictException, DesignSpaceBranchesConflictException {
    	if (!hasDesignSpace(inputSpaceID1)) {
    		throw new DesignSpaceNotFoundException(inputSpaceID1);
    	} else if (!hasDesignSpace(inputSpaceID2)){
    		throw new DesignSpaceNotFoundException(inputSpaceID2);
    	} else if (!outputSpaceID.equals(inputSpaceID1) && !outputSpaceID.equals(inputSpaceID2) && hasDesignSpace(outputSpaceID)) {
    		throw new DesignSpaceConflictException(outputSpaceID);
    	} else if (hasCommonBranches(inputSpaceID1, inputSpaceID2)) {
    		throw new DesignSpaceBranchesConflictException(inputSpaceID1, inputSpaceID2);
    	}
    }
    
    
    
}
