package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.Node.NodeType;
import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.ParameterEmptyException;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;
import knox.spring.data.neo4j.repositories.EdgeRepository;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private Pattern poolPattern = Pattern.compile("\\{(?:\\w|\\s)+(?:,(?:\\w|\\s)+)*\\}");
	private Pattern subPoolPattern = Pattern.compile("(?:\\w|\\s)+");
    
    public static final String RESERVED_ID = "knox";
    
    public void designPools(SBOLDocument partsLibrary, SBOLDocument constructLibrary, Set<String> poolSpecs, 
    		String outputSpaceID) {
    	Set<DesignSpace> constructSpaces = new HashSet<DesignSpace>();
    	
    	HashMap<String, Set<ComponentDefinition>> nameToCompDef = new HashMap<String, Set<ComponentDefinition>>();
    	
    	HashMap<String, Set<ComponentDefinition>> roleToCompDef = new HashMap<String, Set<ComponentDefinition>>();
    	
    	for (ComponentDefinition partDef : partsLibrary.getComponentDefinitions()) {
    		if (partDef.getDisplayId() != null) {
    			if (!nameToCompDef.containsKey(partDef.getDisplayId())) {
    				nameToCompDef.put(partDef.getDisplayId(), new HashSet<ComponentDefinition>());
    			}

    			nameToCompDef.get(partDef.getDisplayId()).add(partDef);
    		}

    		if (partDef.getName() != null) {
    			if (!nameToCompDef.containsKey(partDef.getName())) {
    				nameToCompDef.put(partDef.getName(), new HashSet<ComponentDefinition>());
    			}

    			nameToCompDef.get(partDef.getName()).add(partDef);
    		}
    		
    		for (String role : convertSOIdentifiersToNames(partDef.getRoles())) {
    			if (!roleToCompDef.containsKey(role)) {
    				roleToCompDef.put(role, new HashSet<ComponentDefinition>());
    			}
    			
    			roleToCompDef.get(role).add(partDef);
    		}
    	}
    	
    	for (ComponentDefinition constructDef : constructLibrary.getComponentDefinitions()) {
    		constructSpaces.add(convertComponentDefinitionToDesignSpace(constructDef));
    	}
    	
    	for (String poolSpec : poolSpecs) {
    		DesignSpace inputSpace = convertPoolSpecificationToDesignSpace(poolSpec, nameToCompDef, 
    				roleToCompDef);
    	}
    }
    
    private DesignSpace convertPoolSpecificationToDesignSpace(String poolSpec,
    		HashMap<String, Set<ComponentDefinition>> nameToCompDef,
    		HashMap<String, Set<ComponentDefinition>> roleToCompDef) {
    	DesignSpace space = new DesignSpace(RESERVED_ID);
    	
    	Node currentNode = space.createStartNode();
    	
    	Matcher subPoolMatcher = poolPattern.matcher(poolSpec);
		
		while (subPoolMatcher.find()) {
			Matcher partMatcher = subPoolPattern.matcher(subPoolMatcher.group(0));
			
			while (partMatcher.find()) {
				Set<String> compIDs = new HashSet<String>();

				Set<String> compRoles = new HashSet<String>();
				
				String part = partMatcher.group(0);
				
				Set<ComponentDefinition> compDefs;
				
				if (roleToCompDef.containsKey(part)) {
					compDefs = roleToCompDef.get(part);
				} else if (nameToCompDef.containsKey(part)) {
					compDefs = nameToCompDef.get(part);
				} else {
					compDefs = new HashSet<ComponentDefinition>();
				}
				
				for (ComponentDefinition compDef :compDefs) {
					compIDs.add(compDef.getIdentity().toString());
					
					compRoles.addAll(convertSOIdentifiersToNames(compDef.getRoles()));
				}
				
				Node nextNode = space.createNode();

				currentNode.createEdge(nextNode, new ArrayList<String>(compIDs), 
						new ArrayList<String>(compRoles));

				currentNode = nextNode;
			}
			
			currentNode.setNodeType(NodeType.ACCEPT.getValue());
		}
		
		return space;
    }
    
    private DesignSpace convertComponentDefinitionToDesignSpace(ComponentDefinition compDef) {
    	DesignSpace space = new DesignSpace(compDef.getIdentity().toString());
    	
		Node currentNode = space.createStartNode();

		for (ComponentDefinition leafDef : flattenComponentDefinition(compDef)) {
			ArrayList<String> compIDs = new ArrayList<String>();
			
			compIDs.add(leafDef.getIdentity().toString());

			ArrayList<String> compRoles = new ArrayList<String>(convertSOIdentifiersToNames(leafDef.getRoles()));

			Node nextNode = space.createNode();

			currentNode.createEdge(nextNode, compIDs, compRoles);

			currentNode = nextNode;
		}
		
		currentNode.setNodeType(NodeType.ACCEPT.getValue());
		
		return space;
    }
    
    public void importCSV(List<InputStream> inputCSVStreams, String outputSpacePrefix) {
    	List<BufferedReader> designReaders = new LinkedList<BufferedReader>();
    	
    	List<BufferedReader> compReaders = new LinkedList<BufferedReader>();
    	
    	for (InputStream inputCSVStream : inputCSVStreams) {
    		try {
    			String csvLine;
    			
    			BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputCSVStream));
    			
    			if ((csvLine = csvReader.readLine()) != null) {
    				ArrayList<String> csvArray = csvToArrayList(csvLine);
    				
    				if (csvArray.size() > 0) {
    					if (csvArray.get(0).equals("part1")) {
    						designReaders.add(csvReader);
    					} else if (csvArray.get(0).equals("part name")) {
    						compReaders.add(csvReader);
    					}
    				}
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	HashMap<String, String> compIDToRole = new HashMap<String, String>();
    	
    	for (BufferedReader compReader : compReaders) {
    		try {
    			compIDToRole.putAll(processCSVComponents(compReader));
    		} catch (IOException e) {
    			e.printStackTrace();
    		} finally {
    			try {
    				if (compReader != null) {
    					compReader.close();
    				}
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    	}
    	
    	for (BufferedReader designReader : designReaders) {
    		try {
    			processCSVDesigns(designReader, outputSpacePrefix, compIDToRole);
    		} catch (IOException e) {
    			e.printStackTrace();
    		} finally {
    			try {
    				if (designReader != null) {
    					designReader.close();
    				}
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    	}
    }
    
    public void processCSVDesigns(BufferedReader csvReader, String outputSpacePrefix, 
    		HashMap<String, String> compIDToRole) throws IOException { 
    	String csvLine;
    	
		int j = -1;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);

			if (csvArray.size() > 0 && csvArray.get(0).length() > 0) {
				j++;

				DesignSpace outputSpace = new DesignSpace(outputSpacePrefix + j);

				Node outputStart = outputSpace.createStartNode();

				Node outputPredecessor = outputStart;

				for (int i = 0; i < csvArray.size(); i++) {
					if (csvArray.get(i).length() > 0) {
						ArrayList<String> compIDs = new ArrayList<String>(1);

						compIDs.add(csvArray.get(i));

						ArrayList<String> compRoles = new ArrayList<String>(1);

						compRoles.add(compIDToRole.get(csvArray.get(i)));

						Node outputNode;

						if (i < csvArray.size() - 1) {
							outputNode = outputSpace.createNode();
						} else {
							outputNode = outputSpace.createAcceptNode();
						}

						outputPredecessor.createEdge(outputNode, compIDs, compRoles);

						outputPredecessor = outputNode;
					}
				}

				saveDesignSpace(outputSpace);
			}
		}
    }
    
    public HashMap<String, String> processCSVComponents(BufferedReader csvReader) throws IOException {
    	HashMap<String, String> compIDToRole = new HashMap<String, String>();
    	
    	String csvLine;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);
			
			if (csvArray.size() >= 3) {
				compIDToRole.put(csvArray.get(0), csvArray.get(1));
				compIDToRole.put("r" + csvArray.get(0), csvArray.get(1));
			}
		}
		
		return compIDToRole;
    }
    
    public void mergeSBOL(List<InputStream> inputSBOLStreams, String outputSpaceID, String authority) 
    		throws SBOLValidationException, IOException, SBOLConversionException {
    	
    	List<String> compositeDefIDs = new LinkedList<String>();
    	
    	for (InputStream inputSBOLStream : inputSBOLStreams) {
    		
    		if (authority != null) {
    			SBOLReader.setURIPrefix(authority);
    		}
    		
    		SBOLDocument sbolDoc = SBOLReader.read(inputSBOLStream);
    
    		Set<ComponentDefinition> compDefs = getDNAComponentDefinitions(sbolDoc);

    		for (ComponentDefinition compDef : compDefs) {
    			if (!hasDesignSpace(compDef.getIdentity().toString())) {
    				DesignSpace outputSpace = new DesignSpace(compDef.getIdentity().toString());

    				List<ComponentDefinition> leafDefs = flattenComponentDefinition(compDef);

    				Node currentNode = outputSpace.createStartNode();

    				for (int i = 0; i < leafDefs.size(); i++) {
    					ArrayList<String> compIDs = new ArrayList<String>();
    					compIDs.add(leafDefs.get(i).getIdentity().toString());

    					ArrayList<String> compRoles = new ArrayList<String>(
    							convertSOIdentifiersToNames(leafDefs.get(i).getRoles()));

    					Node nextNode;
    					if (i < leafDefs.size() - 1) {
    						nextNode = outputSpace.createNode();
    					} else {
    						nextNode = outputSpace.createAcceptNode();
    					}

    					currentNode.createEdge(nextNode, compIDs, compRoles);

    					currentNode = nextNode;
    				}

    				saveDesignSpace(outputSpace);
    			}
    		}

    		for (ComponentDefinition compDef : compDefs) {
    			if (compDef.getComponents().size() > 0) {
    				compositeDefIDs.add(compDef.getIdentity().toString());
    			}
    		}
    	}
    
    	mergeDesignSpaces(compositeDefIDs, outputSpaceID, false, false, 0, 0);
    }
    
    private List<ComponentDefinition> flattenComponentDefinition(ComponentDefinition rootDef) {
    	List<ComponentDefinition> leafDefs = new LinkedList<ComponentDefinition>();
    	
    	Stack<ComponentDefinition> defStack = new Stack<ComponentDefinition>();
    	defStack.push(rootDef);
    	
    	while (defStack.size() > 0) {
    		ComponentDefinition compDef = defStack.pop();
    		
    		Set<SequenceAnnotation> seqAnnos = compDef.getSequenceAnnotations();
    		
    		HashMap<String, Integer> compIDToStart = new HashMap<String, Integer>();
    		
    		for (SequenceAnnotation seqAnno : seqAnnos) {
    			if (seqAnno.getComponent() != null) {
    				int start = getStartOfSequenceAnnotation(seqAnno);
    				if (start >= 0) {
    					compIDToStart.put(seqAnno.getComponentURI().toString(), new Integer(start));
    				}
    			}
    		}
    		
    		Set<Component> subComps = compDef.getComponents();
   
    		if (subComps.size() == 0) {
    			leafDefs.add(compDef);
    		} else {
    			List<Component> sortedSubComps = new ArrayList<Component>(subComps.size());
    			
    			for (Component subComp : subComps) {
    				if (compIDToStart.containsKey(subComp.getIdentity().toString())) {
    					int i = 0;

    					while (i < sortedSubComps.size() 
    							&& compIDToStart.get(subComp.getIdentity().toString()) 
    								< compIDToStart.get(sortedSubComps.get(i).getIdentity().toString())) {
    						i++;
    					}

    					sortedSubComps.add(i, subComp);
    				}
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
    		}
    	}
    	return start;
    }
    
    public void deleteDesignSpace(String targetSpaceID) {
    	validateDesignSpaceOperator(targetSpaceID);
    	
    	designSpaceRepository.deleteDesignSpace(targetSpaceID);
    }
    
    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return mapDesignSpaceToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    private void matchDesignSpace(String inputSpaceID1, List<String> inputSpaceIDs2, String outputSpacePrefix) {
    	for (int i = 0; i < inputSpaceIDs2.size(); i++) {
        	List<String> inputSpaceIDs = new ArrayList<String>(1);
        	
        	inputSpaceIDs.add(inputSpaceIDs2.get(i));
        	
        	String outputSpaceID = outputSpacePrefix + i;

        	unionDesignSpaces(inputSpaceIDs, outputSpaceID);

        	List<String> inputSpaceIDs1 = new ArrayList<String>(2);

        	inputSpaceIDs1.add(outputSpaceID);

        	inputSpaceIDs1.add(inputSpaceID1);

        	mergeDesignSpaces(inputSpaceIDs1, true, true, 1, 1);
        }
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
    
    private void partitionDesignSpace(String inputSpaceID, String outputSpacePrefix) {
    	DesignSpace inputSpace = loadDesignSpace(inputSpaceID, 2);
    	
    	int i = 0;
    	
    	for (Node inputStart : inputSpace.getStartNodes()) {
    		DesignSpace outputSpace = new DesignSpace(outputSpacePrefix + i);
    		
    		HashMap<String, Node> inputIDToOutputNode = new HashMap<String, Node>();
    		
    		inputIDToOutputNode.put(inputStart.getNodeID(), outputSpace.copyNode(inputStart));
    		
    		Stack<Node> inputNodeStack = new Stack<Node>();
    		
    		inputNodeStack.push(inputStart);
    		
    		while (inputNodeStack.size() > 0) {
    			Node inputNode = inputNodeStack.pop();
    			
    			Node outputNode;
    			
    			if (inputIDToOutputNode.containsKey(inputNode.getNodeID())) {
    				outputNode = inputIDToOutputNode.get(inputNode.getNodeID());
    			} else {
    				outputNode = outputSpace.copyNode(inputNode);
    				
    				inputIDToOutputNode.put(inputNode.getNodeID(), outputNode);
    			}
    			
    			if (inputNode.hasEdges()) {
    				for (Edge inputEdge : inputNode.getEdges()) {
    					Node inputSuccessor = inputEdge.getHead();

    					Node outputSuccessor;

    					if (inputIDToOutputNode.containsKey(inputSuccessor.getNodeID())) {
    						outputSuccessor = inputIDToOutputNode.get(inputSuccessor.getNodeID());
    					} else {
    						outputSuccessor = outputSpace.copyNode(inputSuccessor);

    						inputIDToOutputNode.put(inputSuccessor.getNodeID(), outputSuccessor);
    					}

    					outputNode.copyEdge(inputEdge, outputSuccessor);

    					inputNodeStack.push(inputSuccessor);
    				}
    			}
    		}
    		
    		saveDesignSpace(outputSpace);
    		
    		i++;
    	}
    }
	
	// Utility which converts CSV to ArrayList using split operation
	private static ArrayList<String> csvToArrayList(String csvLine) {
		ArrayList<String> csvArray = new ArrayList<String>();

		if (csvLine != null) {
			String[] splitData = csvLine.split("\\s*,\\s*");
			for (int i = 0; i < splitData.length; i++) {
				if (!(splitData[i] == null) || !(splitData[i].length() == 0)) {
					csvArray.add(splitData[i].trim());
				}
			}
		}

		return csvArray;
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
