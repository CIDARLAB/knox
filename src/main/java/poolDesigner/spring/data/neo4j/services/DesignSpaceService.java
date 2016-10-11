package poolDesigner.spring.data.neo4j.services;

import poolDesigner.spring.data.neo4j.domain.DesignSpace;
import poolDesigner.spring.data.neo4j.domain.Edge;
import poolDesigner.spring.data.neo4j.domain.Node;
import poolDesigner.spring.data.neo4j.domain.Node.NodeType;
import poolDesigner.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import poolDesigner.spring.data.neo4j.exception.DesignSpaceConflictException;
import poolDesigner.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import poolDesigner.spring.data.neo4j.exception.ParameterEmptyException;
import poolDesigner.spring.data.neo4j.repositories.DesignSpaceRepository;
import poolDesigner.spring.data.neo4j.repositories.EdgeRepository;
import poolDesigner.spring.data.neo4j.repositories.NodeRepository;

import org.sbolstandard.core2.Cut;
import org.sbolstandard.core2.OrientationType;
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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class DesignSpaceService {

    @Autowired DesignSpaceRepository designSpaceRepository;
    @Autowired EdgeRepository edgeRepository;
    @Autowired NodeRepository nodeRepository;
    
    private Pattern poolPattern = Pattern.compile("\\[(?:\\w|\\s)+(?:,(?:\\w|\\s)+)*\\]");
	
    private Pattern subPoolPattern = Pattern.compile("(?:\\w|\\s)+");
    
    public static final String RESERVED_ID = "poolDesigner";
    
    public static final String REVERSE_PREFIX = "r__";
    
    public List<String> designPools(List<String> poolSpecs) throws DesignSpaceNotFoundException {
    	List<String> pools = new ArrayList<String>(poolSpecs.size());
    	
    	for (String poolSpec : poolSpecs) {
    		DesignSpace specSpace = convertPoolToDesignSpace(poolSpec, RESERVED_ID);
    		
    		saveDesignSpace(specSpace);
    		
    		List<String> constructIDs = new LinkedList<String>(getDesignSpaceIDsBySize(specSpace.getNumNodes()));
    		
    		Set<String> matchIDs = matchDesignSpace(specSpace.getSpaceID(), constructIDs, RESERVED_ID);
    		
    		deleteDesignSpace(RESERVED_ID);
    		
    		List<String> completeMatchIDs = new LinkedList<String>();
    		
    		for (String matchID : matchIDs) {
    			if (hasNodes(matchID)) {
    				if (hasReverseComponents(matchID)) {
    					reverseDesignSpace(matchID);
    				}
    				
    				completeMatchIDs.add(matchID);
    			} else {
    				deleteDesignSpace(matchID);
    			}
    		}
    		
    		mergeDesignSpaces(completeMatchIDs, RESERVED_ID, false, false, 2, 0);
    		
    		for (String matchSpaceID : completeMatchIDs) {
    			deleteDesignSpace(matchSpaceID);
    		}
    		
    		DesignSpace poolSpace = loadDesignSpace(RESERVED_ID, 2);
    		
    		pools.add(convertDesignSpaceToPool(poolSpace));
    		
    		deleteDesignSpace(RESERVED_ID);
    	}
    	
    	return pools;
    }
    
    public void deleteAll() {
    	designSpaceRepository.deleteAll();
    }
    
    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return mapDesignSpaceToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public void importSBOL(Set<SBOLDocument> sbolDocs) {
    	Set<String> spaceIDs = getDesignSpaceIDs();
    	
    	for (SBOLDocument sbolDoc : sbolDocs) {
    		int i = 1;
    		
        	for (ComponentDefinition compDef : getDNAComponentDefinitions(sbolDoc)) {
        		String compID = compDef.getIdentity().toString();
        		
        		if (compDef.getComponents().size() > 0) {;
        			if (!spaceIDs.contains(compID)) {
        				convertComponentDefinitionToDesignSpace(compDef);
        				
        				spaceIDs.add(compID);
        			}
        		} else {
        			ArrayList<String> compRoles = convertSOIdentifiersToNames(compDef.getRoles());
        			
        			if (compDef.getDisplayId() != null) {
            			createPartSpace(compDef.getDisplayId(), compID, compRoles, spaceIDs);
            			
            			spaceIDs.add(compDef.getDisplayId());
            		}

            		if (compDef.getName() != null) {
            			createPartSpace(compDef.getName(), compID, compRoles, spaceIDs);
            			
            			spaceIDs.add(compDef.getName());
            		}
            		
            		for (String compRole : compRoles) {
            			createPartSpace(compRole, compID, compRoles, spaceIDs);
            			
            			spaceIDs.add(compRole);
            		}
        		}
        		
        		System.out.println(i);
				
				i++;
        	}
    	}
    }
    
    private void deleteDesignSpace(String targetSpaceID) {
    	designSpaceRepository.deleteDesignSpace(targetSpaceID);
    }
    
    private void reverseDesignSpace(String inputSpaceID) {
    	DesignSpace inputSpace = loadDesignSpace(inputSpaceID, 2);
    	
    	if (inputSpace.hasNodes()) {
    		for (Node node : inputSpace.getNodes()) {
    			if (node.isAcceptNode()) {
    				node.setNodeType(NodeType.START.getValue());
    			} else if (node.isStartNode()) {
    				node.setNodeType(NodeType.ACCEPT.getValue());
    			}
    			
    			if (node.hasEdges()) {
    				for (Edge edge : node.getEdges()) {
    					Node temp = edge.getHead();
    					
    					edge.setHead(edge.getTail());
    					
    					edge.setTail(temp);
    					
    					for (String compID : edge.getComponentIDs()) {
    						if (compID.startsWith(REVERSE_PREFIX)) {
    							compID = compID.substring(REVERSE_PREFIX.length());
    						} else {
    							compID = REVERSE_PREFIX + compID;
    						}
    					}
    				}
    			}
    		}
    	}
    	
    	saveDesignSpace(inputSpace);
    }
    
    private String convertDesignSpaceToPool(DesignSpace space) {
    	String pool = "";
    	
    	Set<String> visitedNodeIDs = new HashSet<String>();
    	
    	Stack<Node> nodeStack = new Stack<Node>();
    	
    	Set<Node> nextNodes = new HashSet<Node>();
    	
    	nextNodes.addAll(space.getStartNodes());
    	
    	while (nextNodes.size() > 0) {
    		pool += "[";
    		
    		nodeStack.addAll(nextNodes);
    		
    		nextNodes.clear();
    		
    		while(!nodeStack.isEmpty()) {
    			Node node = nodeStack.pop();
    			
    			visitedNodeIDs.add(node.getNodeID());
    			
    			if (node.hasEdges()) {
    				for (Edge edge : node.getEdges()) {
    					if (!visitedNodeIDs.contains(edge.getHead().getNodeID())) {
    						nextNodes.add(edge.getHead());
    					}
    					
    					if (edge.hasComponentIDs()) {
    						for (String compID : edge.getComponentIDs()) {
    							pool = pool + compID + ",";
    						}
    					}
    				}
    			}
    		}
    		
    		pool = pool.substring(0, pool.length() - 1);
    		
    		pool += "],";
    	}
    	
    	pool = pool.substring(0, pool.length() - 1);
    	
    	return pool;
    }
    
    private DesignSpace convertPoolToDesignSpace(String poolSpec, String outputSpaceID)
    		throws DesignSpaceNotFoundException {
    	DesignSpace space = new DesignSpace(outputSpaceID);
    	
    	Node currentNode = space.createStartNode();
    	
    	Matcher subPoolMatcher = poolPattern.matcher(poolSpec);
		
		while (subPoolMatcher.find()) {
			Matcher partMatcher = subPoolPattern.matcher(subPoolMatcher.group(0));
			
			while (partMatcher.find()) {
				String partID = partMatcher.group(0);
				
				if (hasDesignSpace(partID)) {
					Node nextNode = space.createNode();

					currentNode.createEdge(nextNode, 
							new ArrayList<String>(getComponentIDs(partID)), 
							new ArrayList<String>(getComponentRoles(partID)));

					currentNode = nextNode;
				} else {
					throw new DesignSpaceNotFoundException(partID);
				}
			}
			
			currentNode.setNodeType(NodeType.ACCEPT.getValue());
		}
		
		return space;
    }
    
    private void convertComponentDefinitionToDesignSpace(ComponentDefinition compDef) {
		List<ComponentDefinition> leafDefs = new LinkedList<ComponentDefinition>();
		
		List<Boolean> areLeavesForward = new LinkedList<Boolean>();
		
		flattenRootComponentDefinition(compDef, leafDefs, areLeavesForward);
		
		ArrayList<ArrayList<String>> allCompIDs = new ArrayList<ArrayList<String>>();
		
		ArrayList<ArrayList<String>> allCompRoles = new ArrayList<ArrayList<String>>();

		for (int i = 0; i < leafDefs.size(); i++) {
			ArrayList<String> compIDs = new ArrayList<String>();
			
			if (areLeavesForward.get(i).booleanValue()) {
				compIDs.add(leafDefs.get(i).getIdentity().toString());
			} else {
				compIDs.add(REVERSE_PREFIX + leafDefs.get(i).getIdentity().toString());
			}

			ArrayList<String> compRoles = new ArrayList<String>(convertSOIdentifiersToNames(leafDefs.get(i).getRoles()));

			allCompIDs.add(compIDs);
			
			allCompRoles.add(compRoles);
		}
		
		createDesignSpace(compDef.getIdentity().toString(), allCompIDs, allCompRoles);
    }
    
    private void createPartSpace(String partID, String compID, ArrayList<String> compRoles,
    		Set<String> spaceIDs) {
    	if (spaceIDs.contains(partID)) {
			DesignSpace partSpace = loadDesignSpace(partID, 2);
			
			if (partSpace.hasNodes()) {
				for (Node startNode : partSpace.getStartNodes()) {
					if (startNode.hasEdges()) {
						for (Edge edge : startNode.getEdges()) {
							edge.addComponent(compID, compRoles);
						}
					}
				}
				
				saveDesignSpace(partSpace);
			}
		} else {
			ArrayList<String> compIDs = new ArrayList<String>();
			
			compIDs.add(compID);
			
			ArrayList<ArrayList<String>> allCompIDs = new ArrayList<ArrayList<String>>();
			
			allCompIDs.add(compIDs);
			
			ArrayList<ArrayList<String>> allCompRoles = new ArrayList<ArrayList<String>>();
			
			allCompRoles.add(compRoles);
			
			createDesignSpace(partID, allCompIDs, allCompRoles);
		}
    }
    
//    private void createComponentEdge(String targetSpaceID, ArrayList<String> compIDs, ArrayList<String> compRoles) {
//    	designSpaceRepository.createComponentEdge(targetSpaceID, compIDs, compRoles);
//    }
//    
//    private void createDesignSpace(String outputSpaceID) {
//    	designSpaceRepository.createDesignSpace(outputSpaceID);
//    }
    
    private void createDesignSpace(String outputSpaceID, ArrayList<ArrayList<String>> compIDs, ArrayList<ArrayList<String>> compRoles) {
    	designSpaceRepository.createDesignSpace(outputSpaceID, compIDs, compRoles);
    }
    
    private void flattenComponentDefinition(ComponentDefinition compDef, List<ComponentDefinition> leafDefs,
    		List<Boolean> areLeavesForward, boolean isForward) {
		Set<Component> subComps = compDef.getComponents();

		if (subComps.size() == 0) {
			leafDefs.add(compDef);
			
			areLeavesForward.add(new Boolean(isForward));
		} else {
			Set<SequenceAnnotation> seqAnnos = compDef.getSequenceAnnotations();
			
			HashMap<String, SequenceAnnotation> compIDToSeqAnno = new HashMap<String, SequenceAnnotation>();
			
			for (SequenceAnnotation seqAnno : seqAnnos) {
				if (seqAnno.getComponentURI() != null) {
					if (getStartOfSequenceAnnotation(seqAnno) > 0) {
						compIDToSeqAnno.put(seqAnno.getComponentURI().toString(), seqAnno);
					}
				}
			}
			
			List<Component> sortedSubComps = new ArrayList<Component>(subComps.size());
			
			for (Component subComp : subComps) {
				if (compIDToSeqAnno.containsKey(subComp.getIdentity().toString())) {
					SequenceAnnotation seqAnno = compIDToSeqAnno.get(subComp.getIdentity().toString());
					
					int i = 0;

					while (i < sortedSubComps.size() 
							&& getStartOfSequenceAnnotation(seqAnno)
									> getStartOfSequenceAnnotation(
											compIDToSeqAnno.get(sortedSubComps.get(i).getIdentity().toString()))) {
						i++;
					}

					sortedSubComps.add(i, subComp);
				}
			}
			
			for (Component subComp : sortedSubComps) {
				SequenceAnnotation seqAnno = compIDToSeqAnno.get(subComp.getIdentity().toString());
			
				if (isSequenceAnnotationForward(seqAnno)) {
					flattenComponentDefinition(subComp.getDefinition(), leafDefs, areLeavesForward, isForward);
				} else {
					flattenComponentDefinition(subComp.getDefinition(), leafDefs, areLeavesForward, !isForward);
				}
			}
		}
    }
    
    private void flattenRootComponentDefinition(ComponentDefinition rootDef, List<ComponentDefinition> leafDefs,
    		List<Boolean> areLeavesForward) {
    	flattenComponentDefinition(rootDef, leafDefs, areLeavesForward, true);
    }
    
    private Set<String> getComponentIDs(String targetSpaceID) {
    	return designSpaceRepository.getComponentIDs(targetSpaceID);
    }
    
    private Set<String> getComponentRoles(String targetSpaceID) {
    	return designSpaceRepository.getComponentRoles(targetSpaceID);
    }
    
    private Set<String> getDesignSpaceIDs() {
    	return designSpaceRepository.getDesignSpaceIDs();
    }
    
    private Set<String> getDesignSpaceIDsBySize(int size) {
    	return designSpaceRepository.getDesignSpaceIDsBySize(size);
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
    
    private ArrayList<String> convertSOIdentifiersToNames(Set<URI> soIdentifiers) {
    	ArrayList<String> roleNames= new ArrayList<String>();
		if (soIdentifiers.size() == 0) {
			roleNames.add("sequence_feature");
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
		    	} else if (soIdentifier.equals(SequenceOntology.type("SO:0001953"))) {
		    		roleNames.add("restriction_enzyme_assembly_scar");
		    	} else if (soIdentifier.equals(SequenceOntology.RESTRICTION_ENZYME_RECOGNITION_SITE)) {
		    		roleNames.add("restriction_enzyme_recognition_site");
		    	} else if (soIdentifier.equals(SequenceOntology.PRIMER_BINDING_SITE)) {
		    		roleNames.add("primer_binding_site");
		    	} else {
		    		roleNames.add("sequence_feature");
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
    		} else if (location instanceof Cut) {
    			Cut cut = (Cut) location;
    			
    			if (start < 0 || cut.getAt() < start) {
    				start = cut.getAt();
    			}
    		}
    	}
    	
    	return start;
    }
    
    private boolean isSequenceAnnotationForward(SequenceAnnotation seqAnno) {
    	for (Location location : seqAnno.getLocations()) {
    		if (location.getOrientation().equals(OrientationType.REVERSECOMPLEMENT)) {
    			return false;
    		}
    	}
    	
    	return true;
    }
    
    private Set<String> matchDesignSpace(String inputSpaceID1, List<String> inputSpaceIDs2, String outputSpacePrefix) {
    	Set<String> outputSpaceIDs = new HashSet<String>();
    	
    	for (int i = 0; i < inputSpaceIDs2.size(); i++) {
        	List<String> inputSpaceIDs = new ArrayList<String>(1);
        	
        	inputSpaceIDs.add(inputSpaceIDs2.get(i));
        	
        	String outputSpaceID = outputSpacePrefix + i;

        	unionDesignSpaces(inputSpaceIDs, outputSpaceID);

        	List<String> inputSpaceIDs1 = new ArrayList<String>(2);

        	inputSpaceIDs1.add(outputSpaceID);

        	inputSpaceIDs1.add(inputSpaceID1);

        	mergeDesignSpaces(inputSpaceIDs1, true, true, 1, 1);
        	
        	outputSpaceIDs.add(outputSpaceID);
        }
    	
    	return outputSpaceIDs;
    }
    
    private void mergeDesignSpaces(List<String> inputSpaceIDs, boolean isIntersection, boolean isCompleteMatch,
    		int strength, int degree) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	mergeDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), isIntersection, isCompleteMatch, strength, degree);
    }
    
    private void mergeDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, boolean isIntersection, 
    		boolean isCompleteMatch, int strength, int degree) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);

    	List<String> prunedSpaceIDs = new LinkedList<String>();
    	
    	for (String inputSpaceID : inputSpaceIDs) {
    		if (!prunedSpaceIDs.contains(inputSpaceID)) {
    			prunedSpaceIDs.add(inputSpaceID);
    		}
    	}
    	
    	List<DesignSpace> prunedSpaces = new LinkedList<DesignSpace>();
    	
    	DesignSpace outputSpace;
    	
    	if (prunedSpaceIDs.remove(outputSpaceID)) {
    		outputSpace = loadDesignSpace(outputSpaceID, 2);
    		
    		for (String inputSpaceID : prunedSpaceIDs) {
        		prunedSpaces.add(loadDesignSpace(inputSpaceID, 2));
        	}
    	} else {
    		int maxMergeIndex = 0;

    		for (String inputSpaceID : prunedSpaceIDs) {
    			DesignSpace inputSpace = loadDesignSpace(inputSpaceID, 2);
    			
    			prunedSpaces.add(inputSpace);
    		}

    		outputSpace = new DesignSpace(outputSpaceID, 0, maxMergeIndex);
    	}
    	
    	if (isIntersection) {
    		boolean isDiffDeleted = false;

    		for (DesignSpace inputSpace : prunedSpaces) {
    			List<Node> inputStarts = new LinkedList<Node>();
    			
    			List<Node> outputStarts = new LinkedList<Node>();
    			
    			if (degree >= 1) {
    				if (degree == 2) {
    					if (inputSpace.hasNodes()) {
    						inputStarts.addAll(inputSpace.getNodes());
    					}
    				} else {
        				inputStarts.addAll(inputSpace.getStartNodes());
    				}
    			
    				if (outputSpace.hasNodes()) {
    					outputStarts.addAll(outputSpace.getNodes());
    				}
    			} else {
    				inputStarts.addAll(inputSpace.getStartNodes());
    				
    				outputStarts.addAll(outputSpace.getStartNodes());
    			}
    			
    			SpaceDiff diff = mergeNodeSpaces(inputStarts, outputStarts, inputSpace, outputSpace, 
    					isIntersection, isCompleteMatch, strength);
    			
    			if (!isDiffDeleted && inputSpaceIDs.contains(outputSpaceID)) {
    				deleteEdges(diff.getEdges());
    				
    				deleteNodes(diff.getNodes());
    				
    				isDiffDeleted = true;
    			}
    		}
    	} else {
    		for (DesignSpace inputSpace : prunedSpaces) {
    			List<Node> inputStarts = new LinkedList<Node>();
    			
    			List<Node> outputStarts = new LinkedList<Node>();
    			
    			if (degree >= 1) {
    				if (degree == 2) {
    					if (inputSpace.hasNodes()) {
    						inputStarts.addAll(inputSpace.getNodes());
    					}
    				} else {
        				inputStarts.addAll(inputSpace.getStartNodes());
    				}
    			
    				if (outputSpace.hasNodes()) {
    					outputStarts.addAll(outputSpace.getNodes());
    				}
    			} else {
    				inputStarts.addAll(inputSpace.getStartNodes());
    				
    				outputStarts.addAll(outputSpace.getStartNodes());
    			}
    			
    			mergeNodeSpaces(inputStarts, outputStarts, inputSpace, outputSpace, isIntersection, isCompleteMatch, strength);
    		}
    	}
  
    	saveDesignSpace(outputSpace);
    	
    	if (inputSpaceIDs.contains(outputSpaceID)) {
    		prunedSpaces.add(0, outputSpace);
    	}
    }
    
    private Node mergeNodes(Node inputNode, Node outputNode, DesignSpace outputSpace, 
    		Stack<Node> inputNodeStack, Stack<Node> outputNodeStack,
    		HashMap<String, Node> mergedIDToOutputNode, HashMap<String, Set<Node>> inputIDToOutputNodes) {
    	String mergerID = inputNode.getNodeID() + outputNode.getNodeID();
    	
		if (mergedIDToOutputNode.containsKey(mergerID)) {
			return mergedIDToOutputNode.get(mergerID);
		} else {
			if (mergedIDToOutputNode.values().contains(outputNode)) {
				outputNode = outputSpace.copyNodeWithEdges(outputNode);
			} 

			if (!inputIDToOutputNodes.containsKey(inputNode.getNodeID())) {
				inputIDToOutputNodes.put(inputNode.getNodeID(), new HashSet<Node>());
			}
			
			inputIDToOutputNodes.get(inputNode.getNodeID()).add(outputNode);

			mergedIDToOutputNode.put(mergerID, outputNode);
		
			inputNodeStack.push(inputNode);
			
			outputNodeStack.push(outputNode);
			
			return outputNode;
		}
    }
    
    private SpaceDiff mergeNodeSpaces(List<Node> inputStarts, List<Node> outputStarts, DesignSpace inputSpace, 
    		DesignSpace outputSpace, boolean isIntersection, boolean isCompleteMatch, int strength) {    	
    	HashMap<String, Set<Node>> inputIDToOutputNodes = new HashMap<String, Set<Node>>();
    	
    	HashMap<String, Node> mergedIDToOutputNode = new HashMap<String, Node>();
    	
    	Set<Edge> mergedEdges = new HashSet<Edge>();
    	
    	Set<Edge> duplicateEdges = new HashSet<Edge>();
    	
    	for (Node inputStart : inputStarts) {
    		Stack<Node> inputNodeStack = new Stack<Node>();
    		
			Set<Node> reachableInputs = new HashSet<Node>();
			
			if (isCompleteMatch && outputStarts.size() > 0) {
				inputNodeStack.push(inputStart);
				
				while (inputNodeStack.size() > 0) {
					Node inputNode = inputNodeStack.pop();

					reachableInputs.add(inputNode);

					if (inputNode.hasEdges()) {
						for (Edge inputEdge : inputNode.getEdges()) {
							if (!reachableInputs.contains(inputEdge.getHead())) {
								inputNodeStack.push(inputEdge.getHead());
							}
						}
					}
				}
			}
    		
    		for (Node outputStart : outputStarts) {
    	    	Stack<Node> outputNodeStack = new Stack<Node>();
    	    	
    			if (!isIntersection || isInputStartMatching(inputStart, outputStart, strength)) {
    				mergeNodes(inputStart, outputStart, outputSpace, inputNodeStack, outputNodeStack, 
    						mergedIDToOutputNode, inputIDToOutputNodes);
    			}
    			
    			Set<Edge> matchingEdges = new HashSet<Edge>();
    			
    			Set<Node> matchingInputs = new HashSet<Node>();

    	    	while (inputNodeStack.size() > 0 && outputNodeStack.size() > 0) {
    	    		Node inputNode = inputNodeStack.pop();
    	    		
    	    		matchingInputs.add(inputNode);
    	    		
    	    		Node outputNode = outputNodeStack.pop();
    	    		
    	    		if (inputNode.hasEdges() && outputNode.hasEdges()) {
    	    			for (Edge outputEdge : outputNode.getEdges()) {
    	    				Node outputSuccessor = outputEdge.getHead();
    	    				
    	    				for (Edge inputEdge : inputNode.getEdges()) {
    	    					if (inputEdge.isMatchingTo(outputEdge, strength)) {
    	    						Node inputSuccessor = inputEdge.getHead();
    	    						
    	    						outputSuccessor = mergeNodes(inputSuccessor, outputSuccessor, outputSpace, 
    	    								inputNodeStack, outputNodeStack, mergedIDToOutputNode, inputIDToOutputNodes);

    	    						if (outputSuccessor != outputEdge.getHead()) {
    	    							outputEdge = outputEdge.copy(outputNode, outputSuccessor);
    	    							duplicateEdges.add(outputEdge);
    	    						}
    	    							
    	    						if (isIntersection) {
    	    							outputEdge.intersectWithEdge(inputEdge);
    	    						} else {
    	    							outputEdge.unionWithEdge(inputEdge);
    	    						}
    	    						
    	    						matchingEdges.add(outputEdge);
    	    					}
    	    				}
    	    			}
    	    		} 
    	    	}
    	    	
    	    	if (isCompleteMatch) {
    	    		if (matchingInputs.equals(reachableInputs)) {
        	    		mergedEdges.addAll(matchingEdges);
        	    	}
    	    	} else {
    	    		mergedEdges.addAll(matchingEdges);
    	    	}
    		}
    	}
    	
    	for (Edge duplicateEdge : duplicateEdges) {
    		duplicateEdge.getTail().addEdge(duplicateEdge);
    	}
    	
    	for (Node typedInput : inputSpace.getTypedNodes()) {
    		if (inputIDToOutputNodes.containsKey(typedInput.getNodeID())) {
    			for (Node outputNode : inputIDToOutputNodes.get(typedInput.getNodeID())) {
    				outputNode.setNodeType(typedInput.getNodeType());
    			}
    		}
    	}
    	
    	if (inputStarts.size() > 0 && (!isIntersection || outputStarts.size() == 0)) {
    		HashMap<String, Node> inputIDToSurplusOutput = new HashMap<String, Node>();
    		
    		Set<String> visitedNodeIDs = new HashSet<String>();

    		Stack<Node> inputNodeStack = new Stack<Node>();
    		
    		for (Node inputStart : inputStarts) {
    			inputNodeStack.push(inputStart);
        		
        		if (!inputIDToOutputNodes.containsKey(inputStart.getNodeID())) {
    				inputIDToSurplusOutput.put(inputStart.getNodeID(), outputSpace.copyNode(inputStart));
    			}

        		while (inputNodeStack.size() > 0) {
        			Node inputNode = inputNodeStack.pop();
        			
        			visitedNodeIDs.add(inputNode.getNodeID());

        			if (inputNode.hasEdges()) {
        				Set<Node> outputNodes;
        				
        				if (inputIDToOutputNodes.containsKey(inputNode.getNodeID())) {
        					outputNodes = inputIDToOutputNodes.get(inputNode.getNodeID());
        				} else {
        					outputNodes = new HashSet<Node>();
        					
        					if (inputIDToSurplusOutput.containsKey(inputNode.getNodeID())) {
        						outputNodes.add(inputIDToSurplusOutput.get(inputNode.getNodeID()));
        					}
        				}
        				
        				for (Edge inputEdge : inputNode.getEdges()) {	
        					Node inputSuccessor = inputEdge.getHead();

        					if (!visitedNodeIDs.contains(inputSuccessor.getNodeID())) {
        						inputNodeStack.push(inputSuccessor);
        					}

        					Set<Node> outputSuccessors;

        					if (inputIDToOutputNodes.containsKey(inputSuccessor.getNodeID())) {
        						outputSuccessors = inputIDToOutputNodes.get(inputSuccessor.getNodeID());
        					} else {
        						outputSuccessors = new HashSet<Node>();
        
        						if (inputIDToSurplusOutput.containsKey(inputSuccessor.getNodeID())) {
        							outputSuccessors.add(inputIDToSurplusOutput.get(inputSuccessor.getNodeID()));
        						} else {
        							Node outputSuccessor = outputSpace.copyNode(inputSuccessor);
        							outputSuccessors.add(outputSuccessor);
        							inputIDToSurplusOutput.put(inputSuccessor.getNodeID(), outputSuccessor);
            					}
        					}
        				
        					if (!inputIDToOutputNodes.containsKey(inputNode.getNodeID()) 
        							|| !inputIDToOutputNodes.containsKey(inputSuccessor.getNodeID())) {
        						for (Node outputNode : outputNodes) {
        							for (Node outputSuccessor : outputSuccessors) { 
        								mergedEdges.add(outputNode.copyEdge(inputEdge, outputSuccessor));
        							}
        						}
        					}
        				}
        			}
        		}
    		}
    	}
    	
    	if (inputStarts.size() > 0 && outputStarts.size() > 0) {
    		Set<Node> mergedNodes = new HashSet<Node>();
        	
        	for (Edge mergedEdge : mergedEdges) {
        		mergedNodes.add(mergedEdge.getTail());
        		
        		mergedNodes.add(mergedEdge.getHead());
        	}
        	
        	Set<Edge> diffEdges;
        	
        	Set<Node> diffNodes;
        	
        	if (isIntersection) {
        		diffEdges = outputSpace.retainEdges(mergedEdges);
        		
        		diffNodes = outputSpace.retainNodes(mergedNodes);
        	} else {
        		diffEdges = outputSpace.getOtherEdges(mergedEdges);
        		
        		diffNodes = outputSpace.getOtherNodes(mergedNodes);
        	}
        	
        	return new SpaceDiff(diffEdges, diffNodes);
    	} else {
    		return new SpaceDiff(new HashSet<Edge>(), new HashSet<Node>());
    	}
    }
    
	private void deleteEdges(Set<Edge> deletedEdges) {
		edgeRepository.delete(deletedEdges);
	}
	
	private void deleteNodes(Set<Node> deletedNodes) {
		nodeRepository.delete(deletedNodes);
	}

	private void deleteNodeCopyIndices(String targetSpaceID) {
    	designSpaceRepository.deleteNodeCopyIndices(targetSpaceID);
    }

	private DesignSpace findDesignSpace(String targetSpaceID) {
    	return designSpaceRepository.findBySpaceID(targetSpaceID);
    }
	
	private DesignSpace loadDesignSpace(String targetSpaceID, int depth) {
		return designSpaceRepository.findOne(getGraphID(targetSpaceID), depth);
	}
	
	private Long getGraphID(String targetSpaceID) {
		Set<Integer> graphIDs = designSpaceRepository.getGraphID(targetSpaceID);
		if (graphIDs.size() > 0) {
			return new Long(graphIDs.iterator().next());
		} else {
			return null;
		}
	}
	
	private boolean hasDesignSpace(String targetSpaceID) {
		return findDesignSpace(targetSpaceID) != null;
	}
	
	private boolean hasNodes(String targetSpaceID) {
		return designSpaceRepository.getNodeIDs(targetSpaceID).size() > 0;
	}
	
	private boolean hasReverseComponents(String targetSpaceID) {
		Set<String> compIDs = designSpaceRepository.getComponentIDs(targetSpaceID);
		
		for (String compID : compIDs) {
			if (compID.startsWith(REVERSE_PREFIX)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isInputStartMatching(Node inputStart, Node outputStart, int strength) {
    	if (inputStart.hasEdges() && outputStart.hasEdges()) {
    		for (Edge outputEdge : outputStart.getEdges()) {
    			for (Edge inputEdge : inputStart.getEdges()) {
    				if (inputEdge.isMatchingTo(outputEdge, strength)) {
    					return true;
    				}
    			}
    		}
    	}

    	return false;
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
	
	private void saveDesignSpace(DesignSpace space) {
		designSpaceRepository.save(space);
	}
	
//	private void setNodeType(String targetSpaceID, String targetNodeID, String nodeType) {
//		designSpaceRepository.setNodeType(targetSpaceID, targetNodeID, nodeType);
//	}

	private void unionDesignSpace(String inputSpaceID, String outputSpaceID) {
        designSpaceRepository.unionDesignSpace(inputSpaceID, outputSpaceID);
    }
	
	private void unionDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID) {
		Set<String> prunedSpaceIDs = new HashSet<String>(inputSpaceIDs);
		
		prunedSpaceIDs.remove(outputSpaceID);
		
		for (String inputSpaceID : prunedSpaceIDs) {
			unionDesignSpace(inputSpaceID, outputSpaceID);
		}
		
		deleteNodeCopyIndices(outputSpaceID);
		
		if (inputSpaceIDs.contains(outputSpaceID)) {
    		prunedSpaceIDs.add(outputSpaceID);
    	}
    }
    
    private void validateListParameter(String parameterName, List<String> parameter)
    		throws ParameterEmptyException {
    	if (parameter.size() == 0) {
    		throw new ParameterEmptyException(parameterName);
    	}
    }
    
    private void validateCombinationalDesignSpaceOperator(List<String> inputSpaceIDs, String outputSpaceID)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	if (inputSpaceIDs.size() == 0) {
    		throw new ParameterEmptyException("inputSpaceIDs");
    	}
    	
    	if (outputSpaceID.length() == 0) {
    		throw new ParameterEmptyException("outputSpaceID");
    	}
    		
    	for (String inputSpaceID : inputSpaceIDs) {
    		if (!hasDesignSpace(inputSpaceID)) {
    			throw new DesignSpaceNotFoundException(inputSpaceID);
    		}
    	}

    	if (!inputSpaceIDs.contains(outputSpaceID) && hasDesignSpace(outputSpaceID)) {
    		throw new DesignSpaceConflictException(outputSpaceID);
    	}
    }
}
