package knox.spring.data.neo4j.services;

import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.Commit;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Snapshot;
import knox.spring.data.neo4j.domain.Node.NodeType;
import knox.spring.data.neo4j.eugene.EugeneConverter;
import knox.spring.data.neo4j.eugene.Device;
import knox.spring.data.neo4j.eugene.Part;
import knox.spring.data.neo4j.eugene.Rule;
import knox.spring.data.neo4j.exception.DesignSpaceBranchesConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceConflictException;
import knox.spring.data.neo4j.exception.DesignSpaceNotFoundException;
import knox.spring.data.neo4j.exception.NodeNotFoundException;
import knox.spring.data.neo4j.exception.ParameterEmptyException;
import knox.spring.data.neo4j.repositories.CommitRepository;
import knox.spring.data.neo4j.repositories.DesignSpaceRepository;
import knox.spring.data.neo4j.repositories.EdgeRepository;
import knox.spring.data.neo4j.repositories.NodeRepository;
import knox.spring.data.neo4j.repositories.SnapshotRepository;
import knox.spring.data.neo4j.sample.DesignSampler;

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
//import org.springframework.transaction.annotation.Transactional;


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

@Service
//@Transactional
public class DesignSpaceService {

    @Autowired CommitRepository commitRepository;
    @Autowired DesignSpaceRepository designSpaceRepository;
    @Autowired EdgeRepository edgeRepository;
    @Autowired NodeRepository nodeRepository;
    @Autowired SnapshotRepository snapshotRepository;
    
    public static final String RESERVED_ID = "knox";
    
//    public void importEugene(List<InputStream> inputEugeneStreams) {
    public void importEugene() {
//    	for (InputStream inputEugeneStream : inputEugeneStreams) {
//    		try {
//    			
//    		}
//    	}
    	
    	Set<Part> parts = new HashSet<Part>();
    	
    	Part pLac = new Part("pLac", Part.PartType.PROMOTER);
    	Part pTet = new Part("pTet", Part.PartType.PROMOTER);
    	Part pR = new Part("pR", Part.PartType.PROMOTER);
    	
    	parts.add(pLac);
    	parts.add(pTet);
    	parts.add(pR);
    	
//    	parts.add(new Part("p1", Part.PartType.PROMOTER));
//    	parts.add(new Part("p2", Part.PartType.PROMOTER));
//    	parts.add(new Part("p3", Part.PartType.PROMOTER));
    	
//    	parts.add(new Part("z1", Part.PartType.RIBOZYME));
    	
//    	parts.add(new Part("r1", Part.PartType.RBS));
//    	parts.add(new Part("r2", Part.PartType.RBS));
    	
//    	Part gene1 = new Part("gene1", Part.PartType.CDS);
//    	Part gene2 = new Part("gene2", Part.PartType.CDS);
//    	Part gene3 = new Part("gene3", Part.PartType.CDS);
//    	Part gene4 = new Part("gene4", Part.PartType.CDS);
//    	Part gene5 = new Part("gene5", Part.PartType.CDS);
//    	Part gene6 = new Part("gene6", Part.PartType.CDS);
//    	Part gene7 = new Part("gene7", Part.PartType.CDS);
//    	Part gene8 = new Part("gene8", Part.PartType.CDS);
//    	Part gene9 = new Part("gene9", Part.PartType.CDS);
//    	Part gene10 = new Part("gene10", Part.PartType.CDS);
//    	Part gene11 = new Part("gene11", Part.PartType.CDS);
    	
//    	parts.add(gene1);
//    	parts.add(gene2);
//    	parts.add(gene3);
//    	parts.add(gene4);
//    	parts.add(gene5);
//    	parts.add(gene6);
//    	parts.add(gene7);
//    	parts.add(gene8);
//    	parts.add(gene9);
//    	parts.add(gene10);
//    	parts.add(gene11);
    	
    	Part lacI = new Part("lacI", Part.PartType.CDS);
    	Part tetR = new Part("tetR", Part.PartType.CDS);
    	Part cI = new Part("cI", Part.PartType.CDS);
    	
    	parts.add(lacI);
    	parts.add(tetR);
    	parts.add(cI);
    	
    	Part doubleT = new Part("tDouble", Part.PartType.TERMINATOR);
    	
    	parts.add(doubleT);
    	parts.add(new Part("t1", Part.PartType.TERMINATOR));
    	parts.add(new Part("t2", Part.PartType.TERMINATOR));
    	
    	parts.add(new Part("s1", Part.PartType.SCAR));
    	
    	List<Part> architecture = new ArrayList<Part>(7);
    	architecture.add(new Part(Part.PartType.PROMOTER));
    	architecture.add(new Part(Part.PartType.CDS));
    	architecture.add(new Part(Part.PartType.TERMINATOR));
    	architecture.add(new Part(Part.PartType.SCAR));
    	architecture.add(new Part(Part.PartType.PROMOTER));
    	architecture.add(new Part(Part.PartType.CDS));
    	architecture.add(new Part(Part.PartType.TERMINATOR));
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(new Part(Part.PartType.CDS));
//    	architecture.add(new Part(Part.PartType.TERMINATOR));
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(new Part(Part.PartType.CDS));
//    	architecture.add(new Part(Part.PartType.TERMINATOR));
    	
//    	List<Part> architecture = new ArrayList<Part>(25);
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene1);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene2);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene3);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene4);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene5);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene6);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene7);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene8);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene9);
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene10);
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene11);
//    	architecture.add(new Part(Part.PartType.TERMINATOR));
    	
//    	List<Part> architecture = new ArrayList<Part>(25);
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RIBOZYME));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene1);
//    	architecture.add(new Part(Part.PartType.TERMINATOR));
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RIBOZYME));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene2);
//    	architecture.add(new Part(Part.PartType.TERMINATOR));
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RIBOZYME));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene3);
//    	architecture.add(new Part(Part.PartType.TERMINATOR));
//    	architecture.add(new Part(Part.PartType.PROMOTER));
//    	architecture.add(new Part(Part.PartType.RIBOZYME));
//    	architecture.add(new Part(Part.PartType.RBS));
//    	architecture.add(gene4);
//    	architecture.add(new Part(Part.PartType.TERMINATOR));
    	
    	Set<Rule> rules = new HashSet<Rule>();
//    	rules.add(new Rule(Rule.RuleType.BEFORE, tetR, pTet));
    	rules.add(new Rule(Rule.RuleType.NEXTTO, pLac, tetR));
    	rules.add(new Rule(Rule.RuleType.NEXTTO, tetR, doubleT));
//    	rules.add(new Rule(Rule.RuleType.BEFORE, lacI, pLac));
//    	rules.add(new Rule(Rule.RuleType.SOME_BEFORE, doubleT, pTet));
    	
    	Device device = new Device("toggleSwitch", architecture, rules);
    	
    	DesignSpace space = convertDeviceToDesignSpace(device, parts);
    	
//    	DesignSampler sampler = new DesignSampler(space);
//    	
//    	long total = 0;
    	
//    	Set<List<String>> samples = sampler.sample(3);
    	
//    	for (int i = 0; i < 50; i++) {
//    	long startTime = System.nanoTime();
//    	Set<List<String>> samples = sampler.enumerate();
//    	total += System.nanoTime() - startTime;
//    	}
//    	
//    	System.out.println(total/50.00);
    	
//    	for (List<String> sample : samples) {
//    		System.out.println("------------------");
//    		System.out.println(samples.iterator().next().toString());
//    		System.out.println();
//    	}
    	
//    	System.out.println(samples.size());
    	
    	saveDesignSpace(space);
    }
    
    public DesignSpace convertDeviceToDesignSpace(Device device, Set<Part> parts) {
    	EugeneConverter converter = new EugeneConverter(parts);
    	
    	return converter.convertDevice(device);
    }
    
    public void importCSV(List<InputStream> inputCSVStreams, String outputSpacePrefix, 
    		boolean isMerge) {
    	List<BufferedReader> designReaders = new LinkedList<BufferedReader>();
    	
    	List<BufferedReader> compReaders = new LinkedList<BufferedReader>();
    	
    	for (InputStream inputCSVStream : inputCSVStreams) {
    		try {
    			String csvLine;
    			
    			BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputCSVStream));
    			
    			if ((csvLine = csvReader.readLine()) != null) {
    				ArrayList<String> csvArray = csvToArrayList(csvLine);
    				
    				if (csvArray.size() > 0) {
    					if (csvArray.get(0).equals("id") && csvArray.get(1).equals("role")
    							&& csvArray.get(2).equals("sequence")) {
    						compReaders.add(csvReader);
    					} else if (csvArray.get(0).equals("design")) {
    						designReaders.add(csvReader);
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
    	
    	List<NodeSpace> csvSpaces = new LinkedList<NodeSpace>();
    	
    	for (BufferedReader designReader : designReaders) {
    		try {
    			csvSpaces.addAll(processCSVDesigns(designReader, outputSpacePrefix, compIDToRole));
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
    	
    	if (isMerge && csvSpaces.size() > 0) {
    		DesignSpace csvSpace = (DesignSpace) csvSpaces.get(0);
    		
    		csvSpace.setSpaceID(outputSpacePrefix);
    		
    		mergeNodeSpaces(csvSpaces, false, false, 0, 1);
    		
//    		csvSpace.createHeadBranch(csvSpace.getSpaceID());
    		
    		System.out.println("creating");
    		
    		System.out.println("size " + csvSpaces.get(0).getSize());
    		
//        	DesignSampler sampler = new DesignSampler(csvSpace);
//    		
//        	Set<List<String>> samples = sampler.enumerate();
//        	
//        	int maxLength = 0;
////        	
////        	int numCycles = 0;
//        	
//        	for (List<String> sample : samples) {
//        		if (sample.size() > maxLength) {
//        			maxLength = sample.size();
//        		}
////        		
////        		for (int i = 0; i < sample.size(); i++) {
////        			if (sample.subList(i + 1, sample.size()).contains(sample.get(i))) {
////        				numCycles++;
////        			}
////        		}
//        	}
//        	
//        	System.out.println("max length " + maxLength);
        	
//        	HashMap<String, Integer> p5ToEdgeCount = new HashMap<String, Integer>();
//        	
//        	HashMap<String, Integer> p3ToEdgeCount = new HashMap<String, Integer>();
//        	
//        	for (Node node : csvSpace.getNodes()) {
//        		if (node.hasEdges()) {
//        			for (Edge edge : node.getEdges()) {
//        				if (edge.getHead().hasEdges() && edge.getHead().getNumEdges() > 1) {
//        					for (String compID : edge.getComponentIDs()) {
//        						p5ToEdgeCount.put(compID, edge.getHead().getNumEdges());
//        					}
//        				}
//        				
//        				if (edge.getComponentIDs().size() > 1) {
//        					for (Edge headEdge : edge.getHead().getEdges()) {
//        						for (String compID : headEdge.getComponentIDs()) {
//        							p3ToEdgeCount.put(compID, edge.getComponentIDs().size());
//        						}
//        					}
//        				}
//        			}
//        		}
//        	}
//        	
//        	System.out.println("5p");
//        	
//        	String[][] p5Counts= new String[p5ToEdgeCount.size()][2];
//        	
//        	int i = 0;
//        	
//        	for (String pos : p5ToEdgeCount.keySet()) {
//        		p5Counts[i][0] = pos;
//        		
//        		p5Counts[i][1] = p5ToEdgeCount.get(pos).toString();
//        		
//        		i++;
//        	}
//        	
//        	for (int x = 0; x < p5Counts.length; x++) {
//        		String line = p5Counts[x][0];
//        		
//        		for (int y = 1; y < p5Counts[x].length; y++) {
//        			line = line + "," + p5Counts[x][y];
//        		}
//        		
//        		System.out.println(line);
//        	}
//        	
//        	System.out.println("3p");
//        	
//        	String[][] p3Counts= new String[p3ToEdgeCount.size()][2];
//        	
//        	i = 0;
//        	
//        	for (String pos : p3ToEdgeCount.keySet()) {
//        		p3Counts[i][0] = pos;
//        		
//        		p3Counts[i][1] = p3ToEdgeCount.get(pos).toString();
//        		
//        		i++;
//        	}
//        	
//        	for (int x = 0; x < p3Counts.length; x++) {
//        		String line = p3Counts[x][0];
//        		
//        		for (int y = 1; y < p3Counts[x].length; y++) {
//        			line = line + "," + p3Counts[x][y];
//        		}
//        		
//        		System.out.println(line);
//        	}
    		
    		saveDesignSpace(csvSpace);
    		
//    		commitToHeadBranch(csvSpace.getSpaceID());
    	} else {
    		for (int i = 0; i < csvSpaces.size(); i++) {
    			DesignSpace csvSpace = (DesignSpace) csvSpaces.get(i);
    			
    			csvSpace.createHeadBranch(csvSpace.getSpaceID());
        		
        		saveDesignSpace(csvSpace);
        		
        		commitToHeadBranch(csvSpace.getSpaceID());
    		}
    	}
    }
    
    public List<DesignSpace> processCSVDesigns(BufferedReader csvReader, String outputSpacePrefix, 
    		HashMap<String, String> compIDToRole) throws IOException {
    	List<DesignSpace> csvSpaces = new LinkedList<DesignSpace>();
    	
    	String csvLine;
    	
		int j = -1;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);

			if (csvArray.size() > 0 && csvArray.get(0).length() > 0) {
				j++;

				DesignSpace outputSpace = new DesignSpace(outputSpacePrefix + j);
				
				System.out.println("loading " + outputSpacePrefix + j);

				Node outputStart = outputSpace.createStartNode();

				Node outputPredecessor = outputStart;

				for (int i = 0; i < csvArray.size(); i++) {
					if (csvArray.get(i).length() > 0) {
						ArrayList<String> compIDs = new ArrayList<String>(1);

						compIDs.add(csvArray.get(i));

						ArrayList<String> compRoles = new ArrayList<String>(1);

						if (compIDToRole.containsKey(csvArray.get(i))) {
							compRoles.add(compIDToRole.get(csvArray.get(i)));
						} else {
							compRoles.add(Part.PartType.REGION.getValue());
						}
						

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

				csvSpaces.add(outputSpace);
			}
		}
		
		return csvSpaces;
    }
    
    
    
    public HashMap<String, String> processCSVComponents(BufferedReader csvReader) throws IOException {
    	HashMap<String, String> compIDToRole = new HashMap<String, String>();
    	
    	String csvLine;
		
		while ((csvLine = csvReader.readLine()) != null) {
			List<String> csvArray = csvToArrayList(csvLine);
			
			if (csvArray.size() >= 3) {
				System.out.println("loading " + csvArray.get(0));
				
				compIDToRole.put(csvArray.get(0), csvArray.get(1));
				
				compIDToRole.put("r" + csvArray.get(0), csvArray.get(1));
			}
		}
		
		return compIDToRole;
    }
    
    public void importSBOL(List<InputStream> inputSBOLStreams, String outputSpaceID, String authority) 
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

    				outputSpace.createHeadBranch(compDef.getIdentity().toString());

    				saveDesignSpace(outputSpace);
    				
    				commitToBranch(outputSpace.getSpaceID(), outputSpace.getHeadBranch().getBranchID());
    			}
    		}

    		for (ComponentDefinition compDef : compDefs) {
    			if (compDef.getComponents().size() > 0) {
    				compositeDefIDs.add(compDef.getIdentity().toString());
    			}
    		}
    	}
    
    	orDesignSpaces(compositeDefIDs, outputSpaceID);
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
			SequenceOntology so = new SequenceOntology();
			
			for (URI soIdentifier : soIdentifiers) {
				if (soIdentifier.equals(SequenceOntology.PROMOTER)) {
		    		roleNames.add("promoter");
		    	} else if (soIdentifier.equals(so.getURIbyId("SO:0000374"))) {
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
    	deleteAllNodes(targetSpaceID);
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
    
    public void resetBranch(DesignSpace targetSpace, Branch targetBranch, List<String> commitPath) {
    	if (targetBranch != null && targetBranch.hasCommits()) {
    		Commit targetCommit = targetBranch.getLatestCommit();

    		int i = 0;

    		if (targetCommit.getCommitID().equals(commitPath.get(i))) {
    			while (targetCommit != null && i + 1 < commitPath.size()) {
    				targetCommit = targetCommit.findPredecessor(commitPath.get(i + 1));
    				i++;
    			}
    			
    			if (targetCommit != null) {
    				targetBranch.setLatestCommit(targetCommit);
    				
        			Set<Commit> diffCommits = targetBranch.retainCommits(targetCommit.getHistory());
        			
        			Set<Commit> deletedCommits = new HashSet<Commit>();
        			
        			for (Commit diffCommit : diffCommits) {
        				if (!targetSpace.containsCommit(diffCommit)) {
        					deletedCommits.add(diffCommit);
        				}
        			}
        			
        			if (deletedCommits.size() > 0) {
        				deleteCommits(deletedCommits);
        			}
        		}
    		}
    		
    		saveDesignSpace(targetSpace);
    	}
    }
    
    public void resetBranch(String targetSpaceID, String targetBranchID, List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	Branch targetBranch = targetSpace.findBranch(targetBranchID);
    	
    	resetBranch(targetSpace, targetBranch, commitPath);
    }
    
    public void resetHeadBranch(String targetSpaceID, List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	resetBranch(targetSpace, targetSpace.getHeadBranch(), commitPath);
    }
    
    public void revertBranch(DesignSpace targetSpace, Branch targetBranch, List<String> commitPath) {
    	if (targetBranch != null && targetBranch.hasCommits()) {
    		Commit targetCommit = targetBranch.getLatestCommit();

    		int i = 0;

    		while (targetCommit != null && i + 1 < commitPath.size()) {
    			targetCommit = targetCommit.findPredecessor(commitPath.get(i + 1));
    		
    			i++;
    		}

    		if (targetCommit != null) {
    			Commit commitCopy = targetBranch.copyCommit(targetCommit);
    			
    			commitCopy.addPredecessor(targetBranch.getLatestCommit());
    			
    			targetBranch.setLatestCommit(commitCopy);
    		}
    		
    		saveDesignSpace(targetSpace);
    	}
    }
    
    public void revertBranch(String targetSpaceID, String targetBranchID, List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	Branch targetBranch = targetSpace.findBranch(targetBranchID);
    	
    	revertBranch(targetSpace, targetBranch, commitPath);
    }
    
    public void revertHeadBranch(String targetSpaceID, List<String> commitPath) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	revertBranch(targetSpace, targetSpace.getHeadBranch(), commitPath);
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
    
    public void joinBranches(String targetSpaceID, List<String> inputBranchIDs) {
    	joinBranches(targetSpaceID, inputBranchIDs, RESERVED_ID);
    }
    
    public void joinBranches(String targetSpaceID, List<String> inputBranchIDs, String outputBranchID) {
    	Set<String> prunedBranchIDs = new HashSet<String>(inputBranchIDs);
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		indexVersionMerger(targetSpaceID, inputBranchID);
    	}
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		mergeBranch(targetSpaceID, inputBranchID, outputBranchID);
    	}
    	
    	createCommit(targetSpaceID, outputBranchID);
    	
    	Set<String> visitedBranchIDs = new HashSet<String>();
    	
    	int outputIndex = inputBranchIDs.indexOf(outputBranchID);
    	
    	for (int i = outputIndex + 1; i < inputBranchIDs.size(); i++) {
    		String inputBranchID = inputBranchIDs.get(i);
    		
    		if (visitedBranchIDs.contains(inputBranchID) && i > 0) {
    			deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    		}
    		
    		Set<String> startNodeIDs1 = getStartNodeIDs(targetSpaceID, outputBranchID);

    		Set<String> acceptNodeIDs1 = getAcceptNodeIDs(targetSpaceID, outputBranchID);

    		unionSnapshot(targetSpaceID, inputBranchID, outputBranchID);

    		if (acceptNodeIDs1.size() > 0) {
    			Set<String> startNodeIDs2 = new HashSet<String>();
    			
    			for (String startNodeID : getStartNodeIDs(targetSpaceID, outputBranchID)) {
    				if (!startNodeIDs1.contains(startNodeID)) {
    					deleteNodeType(targetSpaceID, outputBranchID, startNodeID);
    					
    					startNodeIDs2.add(startNodeID);
    				}
    			}
    			
    			if (startNodeIDs2.size() > 0) {
					for (String acceptNodeID1 : acceptNodeIDs1) {
						deleteNodeType(targetSpaceID, outputBranchID, acceptNodeID1);
						
						for (String startNodeID2 : startNodeIDs2) {
							createEdge(targetSpaceID, outputBranchID, acceptNodeID1, startNodeID2);
						}
					}
    			}
    		}

    		visitedBranchIDs.add(inputBranchID);
    	}
    	
    	for (int i = 0; i < outputIndex; i++) {
    		String inputBranchID = inputBranchIDs.get(i);
    		
    		if (visitedBranchIDs.contains(inputBranchID)) {
    			deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    		}
    		
    		Set<String> startNodeIDs2 = getStartNodeIDs(targetSpaceID, outputBranchID);

    		Set<String> acceptNodeIDs2 = getAcceptNodeIDs(targetSpaceID, outputBranchID);

    		unionSnapshot(targetSpaceID, inputBranchID, outputBranchID);

    		if (startNodeIDs2.size() > 0) {
    			Set<String> acceptNodeIDs1 = new HashSet<String>();
    			
    			for (String acceptNodeID : getAcceptNodeIDs(targetSpaceID, outputBranchID)) {
    				if (!acceptNodeIDs2.contains(acceptNodeID)) {
    					deleteNodeType(targetSpaceID, outputBranchID, acceptNodeID);
    					
    					acceptNodeIDs1.add(acceptNodeID);
    				}
    			}
    			
    			if (acceptNodeIDs1.size() > 0) {
        			for (String startNodeID2 : startNodeIDs2) {
        				deleteNodeType(targetSpaceID, outputBranchID, startNodeID2);

        				for (String acceptNodeID1 : acceptNodeIDs1) {
        					createEdge(targetSpaceID, outputBranchID, acceptNodeID1, startNodeID2);
        				}
        			}
    			}
    		}
    		
    		visitedBranchIDs.add(inputBranchID);
    	}
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		for (String inputBranchID : prunedBranchIDs) {
    			fastForwardBranch(targetSpaceID, inputBranchID, RESERVED_ID);
    		}
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
    public void mergeBranches(String targetSpaceID, List<String> inputBranchIDs, 
    		int tolerance, int strength, boolean isIntersection, boolean isConservative) {
    	mergeBranches(targetSpaceID, inputBranchIDs, RESERVED_ID, tolerance, strength, 
    			isIntersection, isConservative);
    }
    
    public void mergeBranches(String targetSpaceID, List<String> inputBranchIDs, String outputBranchID, 
    		int tolerance, int strength, boolean isIntersection, boolean isConservative) {
    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 5);
    	
    	List<String> prunedBranchIDs = new LinkedList<String>(inputBranchIDs);
    	
    	for (String inputBranchID : inputBranchIDs) {
    		if (!prunedBranchIDs.contains(inputBranchID)) {
    			prunedBranchIDs.add(inputBranchID);
    		}
    	}
    	
    	List<Commit> inputCommits = new LinkedList<Commit>();
    	
    	List<NodeSpace> inputSnapshots = new LinkedList<NodeSpace>();
    	
    	int maxIDIndex = 0;
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		indexVersionMerger(targetSpace.getSpaceID(), inputBranchID);
    		
    		Branch inputBranch = targetSpace.findBranch(inputBranchID);
    		
    		inputCommits.add(inputBranch.getLatestCommit());
    		
    		inputSnapshots.add(inputBranch.getLatestCommit().getSnapshot());
    		
    		if (inputBranch.getIdIndex() > maxIDIndex) {
    			maxIDIndex = inputBranch.getIdIndex();
    		}
    	}
    	
    	Branch outputBranch = targetSpace.createBranch(outputBranchID, maxIDIndex);
    	
    	Commit outputCommit = outputBranch.createCommit();
    	
    	outputBranch.setLatestCommit(outputCommit);
    	
    	outputCommit.createSnapshot();
    	
    	for (Commit inputCommit : inputCommits) {
    		outputBranch.addCommit(inputCommit);
    		
    		outputCommit.addPredecessor(inputCommit);
    	}
   
    	mergeNodeSpaces(inputSnapshots, outputCommit.getSnapshot(), tolerance, strength, 
    			isIntersection, isConservative);

    	saveDesignSpace(targetSpace);
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		for (String inputBranchID : prunedBranchIDs) {
    			fastForwardBranch(targetSpace.getSpaceID(), inputBranchID, RESERVED_ID);
    		}
        	deleteBranch(targetSpace.getSpaceID(), RESERVED_ID);
    	}
    }
    
    public void orBranches(String targetSpaceID, List<String> inputBranchIDs) {
    	orBranches(targetSpaceID, inputBranchIDs, RESERVED_ID);
    }
    
    public void orBranches(String targetSpaceID, List<String> inputBranchIDs, String outputBranchID) {
    	Set<String> prunedBranchIDs = new HashSet<String>(inputBranchIDs);
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		indexVersionMerger(targetSpaceID, inputBranchID);
    	}
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		mergeBranch(targetSpaceID, inputBranchID, outputBranchID);
    	}
    	
    	createCommit(targetSpaceID, outputBranchID);
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		unionSnapshot(targetSpaceID, inputBranchID, outputBranchID);
    	}
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	Set<Node> startNodes = getStartNodes(targetSpaceID, outputBranchID);
    	
    	if (startNodes.size() > 0) {
    		Node startNodePrime = createTypedNode(targetSpaceID, outputBranchID, Node.NodeType.START.getValue());

    		for (Node startNode : startNodes) {
    			deleteNodeType(targetSpaceID, outputBranchID, startNode.getNodeID());
    			createEdge(targetSpaceID, outputBranchID, startNodePrime.getNodeID(), startNode.getNodeID());
    		}
    	}
    	
    	Set<Node> acceptNodes = getAcceptNodes(targetSpaceID, outputBranchID);
    	
    	if (acceptNodes.size() > 0) {
    		Node acceptNodePrime = createTypedNode(targetSpaceID, outputBranchID, Node.NodeType.ACCEPT.getValue());

    		for (Node acceptNode : acceptNodes) {
    			deleteNodeType(targetSpaceID, outputBranchID, acceptNode.getNodeID());
    			createEdge(targetSpaceID, outputBranchID, acceptNode.getNodeID(), acceptNodePrime.getNodeID());
    		}
    	}
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		for (String inputBranchID : prunedBranchIDs) {
    			fastForwardBranch(targetSpaceID, inputBranchID, RESERVED_ID);
    		}
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
    }
    
//    public void resetHeadBranch(String targetSpaceID, String targetCommitID) {
//    	validateDesignSpaceOperator(targetSpaceID);
//    	designSpaceRepository.resetHeadBranch(targetSpaceID, targetCommitID);
//    }
   
    public void deleteDesignSpace(String targetSpaceID) {
    	validateDesignSpaceOperator(targetSpaceID);
    	
    	designSpaceRepository.deleteDesignSpace(targetSpaceID);
    }
    
    public void createDesignSpace(String outputSpaceID) {
    	validateGenerativeDesignSpaceOperator(outputSpaceID);
    	
    	designSpaceRepository.createDesignSpace(outputSpaceID);
    }
    
    public void createDesignSpace(String outputSpaceID, List<String> compIDs, List<String> compRoles) {
    	validateGenerativeDesignSpaceOperator(outputSpaceID);
    	
    	designSpaceRepository.createDesignSpace(outputSpaceID, new ArrayList<String>(compIDs), new ArrayList<String>(compRoles));
    }
    
    public Map<String, Object> d3GraphDesignSpace(String targetSpaceID) {
        return mapDesignSpaceToD3Format(designSpaceRepository.mapDesignSpace(targetSpaceID));
    }
    
    public void insertDesignSpace(String inputSpaceID1, String inputSpaceID2, String targetNodeID, String outputSpaceID)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException, NodeNotFoundException {
    	validateCombinationalDesignSpaceOperator(inputSpaceID1, inputSpaceID2, outputSpaceID);
    	validateNodeOperator(inputSpaceID1, targetNodeID);
    	
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
    
    public void joinDesignSpaces(List<String> inputSpaceIDs) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	joinDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0));
    }
    
    public void joinDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	Set<String> visitedSpaceIDs = new HashSet<String>();
    	
    	if (inputSpaceIDs.contains(outputSpaceID)) {
    		visitedSpaceIDs.add(outputSpaceID);
    	}
    	
    	int outputIndex = inputSpaceIDs.indexOf(outputSpaceID);
    	
    	for (int i = outputIndex + 1; i < inputSpaceIDs.size(); i++) {
    		String inputSpaceID = inputSpaceIDs.get(i);
    		
    		if (visitedSpaceIDs.contains(inputSpaceID) && i > 0) {
    			deleteNodeCopyIndices(outputSpaceID);
    		}
    		
    		Set<String> startNodeIDs1 = getStartNodeIDs(outputSpaceID);

    		Set<String> acceptNodeIDs1 = getAcceptNodeIDs(outputSpaceID);

    		unionDesignSpace(inputSpaceID, outputSpaceID);

    		if (acceptNodeIDs1.size() > 0) {
    			Set<String> startNodeIDs2 = new HashSet<String>();
    			
    			for (String startNodeID : getStartNodeIDs(outputSpaceID)) {
    				if (!startNodeIDs1.contains(startNodeID)) {
    					deleteNodeType(outputSpaceID, startNodeID);
    					
    					startNodeIDs2.add(startNodeID);
    				}
    			}
    			
    			if (startNodeIDs2.size() > 0) {
					for (String acceptNodeID1 : acceptNodeIDs1) {
						deleteNodeType(outputSpaceID, acceptNodeID1);
						
						for (String startNodeID2 : startNodeIDs2) {
							createEdge(outputSpaceID, acceptNodeID1, startNodeID2);
						}
					}
    			}
    		}

    		visitedSpaceIDs.add(inputSpaceID);
    	}
    	
    	for (int i = 0; i < outputIndex; i++) {
    		String inputSpaceID = inputSpaceIDs.get(i);
    		
    		if (visitedSpaceIDs.contains(inputSpaceID)) {
    			deleteNodeCopyIndices(outputSpaceID);
    		}
    		
    		Set<String> startNodeIDs2 = getStartNodeIDs(outputSpaceID);

    		Set<String> acceptNodeIDs2 = getAcceptNodeIDs(outputSpaceID);

    		unionDesignSpace(inputSpaceID, outputSpaceID);

    		if (startNodeIDs2.size() > 0) {
    			Set<String> acceptNodeIDs1 = new HashSet<String>();
    			
    			for (String acceptNodeID : getAcceptNodeIDs(outputSpaceID)) {
    				if (!acceptNodeIDs2.contains(acceptNodeID)) {
    					deleteNodeType(outputSpaceID, acceptNodeID);
    					
    					acceptNodeIDs1.add(acceptNodeID);
    				}
    			}
    			
    			if (acceptNodeIDs1.size() > 0) {
        			for (String startNodeID2 : startNodeIDs2) {
        				deleteNodeType(outputSpaceID, startNodeID2);

        				for (String acceptNodeID1 : acceptNodeIDs1) {
        					createEdge(outputSpaceID, acceptNodeID1, startNodeID2);
        				}
        			}
    			}
    		}
    		
    		visitedSpaceIDs.add(inputSpaceID);
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);
    	
    	visitedSpaceIDs.clear();
    	
    	List<String> headBranchIDs = new LinkedList<String>();

    	for (String inputSpaceID : inputSpaceIDs) {
    		String headBranchID = getHeadBranchID(inputSpaceID);
    		
    		if (!visitedSpaceIDs.contains(inputSpaceID)) {
    			
    			if (!inputSpaceID.equals(outputSpaceID)) {
    				mergeVersionHistory(inputSpaceID, outputSpaceID);
    			}
        		
        		indexVersionMerger(outputSpaceID, headBranchID);
        		
        		visitedSpaceIDs.add(inputSpaceID);
    		}
    		
    		headBranchIDs.add(headBranchID);
    	}

    	joinBranches(outputSpaceID, headBranchIDs);
    	
    	if (!inputSpaceIDs.contains(outputSpaceID)) {
    		selectHeadBranch(outputSpaceID, headBranchIDs.get(0));
    	}
    }
    
//    public void matchDesignSpace(String inputSpaceID1, List<String> inputSpaceIDs2, String outputSpacePrefix) {
//    	for (int i = 0; i < inputSpaceIDs2.size(); i++) {
//        	List<String> inputSpaceIDs = new ArrayList<String>(1);
//        	
//        	inputSpaceIDs.add(inputSpaceIDs2.get(i));
//        	
//        	String outputSpaceID = outputSpacePrefix + i;
//
//        	unionDesignSpaces(inputSpaceIDs, outputSpaceID);
//
//        	List<String> inputSpaceIDs1 = new ArrayList<String>(2);
//
//        	inputSpaceIDs1.add(outputSpaceID);
//
//        	inputSpaceIDs1.add(inputSpaceID1);
//
//        	mergeDesignSpaces(inputSpaceIDs1, true, true, 1, 1);
//        }
//    }
    
    public Map<String, Object> matchDesignSpaces(List<String> querySpaceIDs, List<String> queriedSpaceIDs) {
    	List<DesignSpace> querySpaces = new ArrayList<DesignSpace>(querySpaceIDs.size());
    	
    	for (String querySpaceID : querySpaceIDs) {
    		querySpaces.add(loadDesignSpace(querySpaceID, 2));
    	}
    	
    	List<DesignSpace> queriedSpaces = new ArrayList<DesignSpace>(queriedSpaceIDs.size());
    	
    	for (String queriedSpaceID : queriedSpaceIDs) {
    		queriedSpaces.add(loadDesignSpace(queriedSpaceID, 2));
    	}
    	
    	List<List<DesignSpace>> allMatchSpaces = new ArrayList<List<DesignSpace>>(querySpaces.size());
    	
    	for (int i = 0; i < querySpaces.size(); i++) {
    		allMatchSpaces.add(new ArrayList<DesignSpace>(queriedSpaces.size()));
    		
    		for (int j = 0; j < queriedSpaces.size(); j++) {
    			List<NodeSpace> inputSpaces = new ArrayList<NodeSpace>(2);

    			inputSpaces.add(queriedSpaces.get(j));

    			inputSpaces.add(querySpaces.get(i));
    			
    			mergeNodeSpaces(inputSpaces, true, true, 1, 1);

    			allMatchSpaces.get(i).add(queriedSpaces.get(j));
    		}
    	}
    	
    	Map<String, Object> completeMatches = new HashMap<String, Object>();
		
		for (List<DesignSpace> matchSpaces : allMatchSpaces) {
			for (DesignSpace matchSpace : matchSpaces) {
				if (matchSpace.hasNodes()) {
					Set<String> matchNodeIDs = new HashSet<String>();
					
					for (Node startNode : matchSpace.getStartNodes()) {
						matchNodeIDs.add(startNode.getNodeID());
					}
					
					completeMatches.put(matchSpace.getSpaceID(), matchNodeIDs);
				}
			}
		}
    	
    	return completeMatches;
    }
    
    public void mergeDesignSpaces(List<String> inputSpaceIDs, int tolerance, int strength, 
    		boolean isIntersection, boolean isConservative) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	mergeDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0), tolerance, strength, isIntersection, isConservative);
    }
    
    public void mergeDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID, int tolerance, int strength,
    		boolean isIntersection, boolean isConservative) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);

    	List<String> prunedSpaceIDs = new LinkedList<String>();
    	
    	for (String inputSpaceID : inputSpaceIDs) {
    		if (!prunedSpaceIDs.contains(inputSpaceID)) {
    			prunedSpaceIDs.add(inputSpaceID);
    		}
    	}
    	
    	List<NodeSpace> prunedSpaces = new LinkedList<NodeSpace>();
    	
    	NodeSpace outputSpace;
    	
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
    			
    			if (inputSpace.getMergeIndex() > maxMergeIndex) {
    				maxMergeIndex = inputSpace.getMergeIndex();
    			}
    		}

    		outputSpace = new DesignSpace(outputSpaceID, 0, maxMergeIndex);
    	}
    	
    	mergeNodeSpaces(prunedSpaces, outputSpace, tolerance, strength,
    			isIntersection, isConservative);
  
    	saveDesignSpace((DesignSpace) outputSpace);
    	
    	if (inputSpaceIDs.contains(outputSpaceID)) {
    		prunedSpaces.add(0, outputSpace);
    	}
    	
    	List<String> headBranchIDs = new LinkedList<String>();

    	for (int i = 0; i < prunedSpaces.size(); i++) {
    		DesignSpace inputSpace = (DesignSpace) prunedSpaces.get(i);
    		if (!inputSpace.getSpaceID().equals(outputSpaceID)) {
    			mergeVersionHistory(inputSpace.getSpaceID(), outputSpaceID);
    		}
    		
    		String headBranchID = inputSpace.getHeadBranch().getBranchID();

    		indexVersionMerger(outputSpaceID, headBranchID);

    		headBranchIDs.add(headBranchID);
    	}
    	
    	mergeBranches(outputSpaceID, headBranchIDs, tolerance, strength, isIntersection, isConservative);
    	
    	if (!inputSpaceIDs.contains(outputSpaceID)) {
    		selectHeadBranch(outputSpaceID, headBranchIDs.get(0));
    	}
    }
    
    private void mergeNodeSpaces(List<NodeSpace> inputSpaces, boolean isIntersection, boolean isCompleteMatch,
    		int strength, int degree) {
    	if (inputSpaces.size() > 1) {
    		mergeNodeSpaces(inputSpaces.subList(1, inputSpaces.size()), inputSpaces.get(0), 
    				 strength, degree, isIntersection, isCompleteMatch);
    	}
    }
    
    private int locateNodeFrom(String nodeID, int k, List<Node> nodes) {
    	for (int i = k; i < nodes.size(); i++) {
    		if (nodes.get(i).getNodeID().equals(nodeID)) {
    			return i;
    		}
    	}
    	
    	for (int i = 0; i < k; i++) {
    		if (nodes.get(i).getNodeID().equals(nodeID)) {
    			return i;
    		}
    	}
    	
    	return -1;
    }
    
    private void pruneUnmergedEdges(NodeSpace space, Set<Edge> nsEdges, Set<Edge> ewEdges) {
    	Stack<Node> nodeStack = new Stack<Node>();
    	
    	Stack<Integer> directionStack = new Stack<Integer>();
    	
    	for (Node startNode : space.getStartNodes()) {
    		nodeStack.push(startNode);
    		
    		directionStack.push(new Integer(0));
    	}
    	
//    	System.out.println("start " + space.getStartNodes().size());
//    	
//    	System.out.println("ns " + nsEdges.size());
//    	
//    	System.out.println("ew " + ewEdges.size());
    	
    	Set<String> visitedIDs = new HashSet<String>();
    	
    	while (!nodeStack.isEmpty()) {
    		Node node = nodeStack.pop();
    		
    		Integer direction = directionStack.pop();
    		
    		visitedIDs.add(node.getNodeID());
    		
    		if (node.hasEdges()) {
    			Set<Edge> prunedEdges = new HashSet<Edge>();
    			
    			for (Edge edge : node.getEdges()) {
    				if (nsEdges.contains(edge)) {
    					if (direction.intValue() == 0 || direction.intValue() == 1) {
    						if (!visitedIDs.contains(edge.getHead().getNodeID())) {
    							nodeStack.push(edge.getHead());

    							directionStack.push(new Integer(1));
    						}
    					} else {
    						prunedEdges.add(edge);
    					}
    				} else if (ewEdges.contains(edge)) {
    					if (direction.intValue() == 0 || direction.intValue() == 2) {
    						if (!visitedIDs.contains(edge.getHead().getNodeID())) {
    							nodeStack.push(edge.getHead());

    							directionStack.push(new Integer(2));
    						}
    					} else {
    						prunedEdges.add(edge);
    					}
    				} else {
    					nodeStack.push(edge.getHead());

    					directionStack.push(new Integer(0));
    				}
    			}
    			
    			node.deleteEdges(prunedEdges);
    		}
    	}
    }
    
    private void mergeNodeSpaces(List<NodeSpace> inputSpaces, NodeSpace outputSpace,
    		int tolerance, int strength, boolean isIntersection, boolean isConservative) {
    	int x = 0;

    	for (NodeSpace inputSpace : inputSpaces) {
    		if (!outputSpace.hasNodes()) {
    			outputSpace.union(inputSpace);
    		} else {
    			System.out.println("merge " + x++);

    			List<Node> inputNodes = inputSpace.orderNodes();

    			List<Node> priorOutputNodes = outputSpace.orderNodes();
    			
    			outputSpace.clearNodes();

    			List<List<Node>> productNodes = new ArrayList<List<Node>>(inputNodes.size());
    			
    			// Cartesian product of nodes
    			for (int i = 0; i < inputNodes.size(); i++) {
    				productNodes.add(new ArrayList<Node>(priorOutputNodes.size()));

    				for (int j = 0; j < priorOutputNodes.size(); j++) {
    					if (inputNodes.get(i).isAcceptNode() || priorOutputNodes.get(j).isAcceptNode()) {
    						productNodes.get(i).add(outputSpace.createAcceptNode());
    					} else {
    						productNodes.get(i).add(outputSpace.createNode());
    					}
    				}
    			}

    			// Tensor product of graphs
    			if (strength == 0 || strength == 2) {
    				for (int i = 0; i < inputNodes.size(); i++) {
    					for (int j = 0; j < priorOutputNodes.size(); j++) {
    						if (inputNodes.get(i).hasEdges() 
    								&& priorOutputNodes.get(j).hasEdges()) {
    							for (Edge inputEdge : inputNodes.get(i).getEdges()) {
    								for (Edge priorOutputEdge : priorOutputNodes.get(j).getEdges()) {
    									if (inputEdge.isMatchingTo(priorOutputEdge, tolerance)) {
    										Node outputNode = productNodes.get(i).get(j);

    										int r = locateNodeFrom(inputEdge.getHead().getNodeID(), i, 
    												inputNodes);

    										int c = locateNodeFrom(priorOutputEdge.getHead().getNodeID(), j,
    												priorOutputNodes);

    										Edge outputEdge = outputNode.copyEdge(priorOutputEdge, 
    												productNodes.get(r).get(c));

    										if (isIntersection) {
    											outputEdge.intersectWithEdge(inputEdge);
    										} else if (tolerance > 0) {
    											outputEdge.unionWithEdge(inputEdge);
    										}
    									}
    								}
    							}
    						}
    					}
    				}
    				
    				if (strength == 0) {
        				outputSpace.labelStartNodes();
        				
        				outputSpace.mergeStartNodes();
        			}
    			}
    			
    			// Strong product of graphs
    			if (strength == 1 || strength == 2) {
    				Set<Edge> nsEdges = new HashSet<Edge>();

    				for (int i = 0; i < inputNodes.size(); i++) {
    					if (inputNodes.get(i).hasEdges()) {
    						for (Edge inputEdge : inputNodes.get(i).getEdges()) {
    							int r = locateNodeFrom(inputEdge.getHead().getNodeID(), i, inputNodes);

    							for (int j = 0; j < priorOutputNodes.size(); j++) {
    								nsEdges.add(productNodes.get(i).get(j).copyEdge(inputEdge, 
    										productNodes.get(r).get(j)));
    							}
    						}
    					}
    				}

    				Set<Edge> ewEdges = new HashSet<Edge>();

    				for (int j = 0; j < priorOutputNodes.size(); j++) {
    					if (priorOutputNodes.get(j).hasEdges()) {
    						for (Edge priorOutputEdge : priorOutputNodes.get(j).getEdges()) {
    							int c = locateNodeFrom(priorOutputEdge.getHead().getNodeID(), j, priorOutputNodes);

    							for (int i = 0; i < inputNodes.size(); i++) {
    								ewEdges.add(productNodes.get(i).get(j).copyEdge(priorOutputEdge, 
    										productNodes.get(i).get(c)));
    							}
    						}
    					}
    				}
    				
    				if (!productNodes.isEmpty() && !productNodes.get(0).isEmpty()) {
    					productNodes.get(0).get(0).setNodeType(Node.NodeType.START.getValue());
    				}
 
    				if (isConservative) {
    					pruneUnmergedEdges(outputSpace, nsEdges, ewEdges);
    				}
    			}
    			
    			outputSpace.deleteUnreachableNodes();
    			
    			outputSpace.labelAcceptNodes();
    		}
    	}
    }
    
//    private void mergeNodeSpaces(List<NodeSpace> inputSpaces, NodeSpace outputSpace,
//    		boolean isIntersection, boolean isCompleteMatch, int strength, int degree) {
//    	int x = 0;
//    	for (NodeSpace inputSpace : inputSpaces) {
//    		System.out.println("merge " + x++);
//    		if (outputSpace.hasNodes()) {
//    			Set<Edge> mergedEdges = new HashSet<Edge>();
//    			
//    			HashMap<String, Node> mergedIDToOutputNode = new HashMap<String, Node>();
//    			
//    			HashMap<String, Set<Node>> inputIDToOutputNodes = new HashMap<String, Set<Node>>();
//    			
//    			HashMap<String, Set<Node>> priorIDToOutputNodes = new HashMap<String, Set<Node>>();
//        		
//        		HashMap<String, Integer> idToInputDepth = calculateNodeDepths(inputSpace);
//        		
//        		HashMap<String, Integer> idToOutputDepth = calculateNodeDepths(outputSpace);
//        		
//        		Set<Node> priorOutputNodes = new HashSet<Node>(outputSpace.getNodes());
//        		
//        		for (Node inputNode : inputSpace.getNodes()) {
//        			for (Node priorOutputNode : priorOutputNodes) {
//        				Integer inputDepth = idToInputDepth.get(inputNode.getNodeID());
//        				
//        				Integer outputDepth = idToOutputDepth.get(priorOutputNode.getNodeID());
//        				
//        				if (inputNode.hasEdges() 
//        						&& priorOutputNode.hasEdges()
//        						&& (degree == 2 
//        								|| (degree == 1 && inputDepth.intValue() <= outputDepth.intValue()) 
//        								|| (degree == 0 && inputDepth.intValue() == outputDepth.intValue()))) {
//        					for (Edge inputEdge : inputNode.getEdges()) {
//        						for (Edge priorOutputEdge : priorOutputNode.getEdges()) {
//        							if (inputEdge.isMatchingTo(priorOutputEdge, strength)) {
//        								String mergedID = inputNode.getNodeID() + priorOutputNode.getNodeID();
//        								
//        								Node outputNode;
//        								
//        								if (mergedIDToOutputNode.containsKey(mergedID)) {
//        									outputNode = mergedIDToOutputNode.get(mergedID);
//        								} else {
//        									if (inputNode.isStartNode() || priorOutputNode.isStartNode()) {
//        										outputNode = outputSpace.createStartNode();
//        									} else if (inputNode.isAcceptNode() || priorOutputNode.isAcceptNode()) {
//        										outputNode = outputSpace.createAcceptNode();
//        									} else {
//        										outputNode = outputSpace.createNode();
//        									}
//        									
//        									mergedIDToOutputNode.put(mergedID, outputNode);
//        									
//        									if (!inputIDToOutputNodes.containsKey(inputNode.getNodeID())) {
//        										inputIDToOutputNodes.put(inputNode.getNodeID(), new HashSet<Node>());
//        									} 
//        									
//        									inputIDToOutputNodes.get(inputNode.getNodeID()).add(outputNode);
//        									
//        									if (!priorIDToOutputNodes.containsKey(priorOutputNode.getNodeID())) {
//        										priorIDToOutputNodes.put(priorOutputNode.getNodeID(), new HashSet<Node>());
//        									}
//        									
//        									priorIDToOutputNodes.get(priorOutputNode.getNodeID()).add(outputNode);
//        								}
//        								
//        								mergedID = inputEdge.getHead().getNodeID() + priorOutputEdge.getHead().getNodeID();
//        								
//        								Node outputSuccessor;
//        								
//        								if (mergedIDToOutputNode.containsKey(mergedID)) {
//        									outputSuccessor = mergedIDToOutputNode.get(mergedID);
//        									
//        									if (outputSuccessor.isStartNode()) {
//        										if (inputEdge.getHead().isAcceptNode() || priorOutputEdge.getHead().isAcceptNode()) {
//        											outputSuccessor.setNodeType(NodeType.ACCEPT.getValue());
//        										} else {
//        											outputSuccessor.clearNodeType();
//        										}
//        									}
//        								} else {
//        									if (inputEdge.getHead().isAcceptNode() || priorOutputEdge.getHead().isAcceptNode()) {
//        										outputSuccessor = outputSpace.createAcceptNode();
//        									} else {
//        										outputSuccessor = outputSpace.createNode();
//        									}
//        									
//        									mergedIDToOutputNode.put(mergedID, outputSuccessor);
//        									
//        									if (!inputIDToOutputNodes.containsKey(inputEdge.getHead().getNodeID())) {
//        										inputIDToOutputNodes.put(inputEdge.getHead().getNodeID(), new HashSet<Node>());
//        									} 
//        									
//        									inputIDToOutputNodes.get(inputEdge.getHead().getNodeID()).add(outputSuccessor);
//        									
//        									if (!priorIDToOutputNodes.containsKey(priorOutputEdge.getHead().getNodeID())) {
//        										priorIDToOutputNodes.put(priorOutputEdge.getHead().getNodeID(), new HashSet<Node>());
//        									}
//        									
//        									priorIDToOutputNodes.get(priorOutputEdge.getHead().getNodeID()).add(outputSuccessor);
//        								}
//        								
//        								Edge outputEdge = outputNode.copyEdge(priorOutputEdge, outputSuccessor);
//        								
//        								if (isIntersection) {
//        									outputEdge.intersectWithEdge(inputEdge);
//        								} else if (strength > 0) {
//        									outputEdge.unionWithEdge(inputEdge);
//        								}
//        								
//        								mergedEdges.add(inputEdge);
//            							
//            							mergedEdges.add(priorOutputEdge);
//        							}
//        						}
//        					}
//        				}
//        			}
//        		}
//        		
//        		if (!isIntersection) {
//        			mergeSurplusNodes(inputSpace.getNodes(), mergedEdges, inputIDToOutputNodes, outputSpace);
//        			
//        			mergeSurplusNodes(priorOutputNodes, mergedEdges, priorIDToOutputNodes, outputSpace);
//        		}
//        		
//        		outputSpace.removeNodesWithSameIDs(priorOutputNodes);
//    		} else {
//    			outputSpace.unionNodes(inputSpace);
//    		}
//    		
//    		Set<String> successorIDs = new HashSet<String>();
//        	
//        	for (Node outputNode : outputSpace.getNodes()) {
//        		if (outputNode.hasEdges()) {
//        			for (Edge outputEdge : outputNode.getEdges()) {
//        				successorIDs.add(outputEdge.getHead().getNodeID());
//        			}
//        		} else if (!outputNode.isAcceptNode()) {
//        			outputNode.setNodeType(Node.NodeType.ACCEPT.getValue());
//        		}
//        	}
//        	
//        	for (Node outputNode : outputSpace.getNodes()) {
//        		if (!successorIDs.contains(outputNode.getNodeID()) && !outputNode.isStartNode()) {
//        			outputNode.setNodeType(Node.NodeType.START.getValue());
//        		}
//        	}
//        	
//        	outputSpace.mergeStartNodes();
//    	}
//    }
    
//    private HashMap<String, Integer> calculateNodeDepths(NodeSpace space) {
//    	int maxDepth = 0;
//    	
//    	Stack<Node> nodeStack = new Stack<Node>();
//    	
//    	Stack<Integer> depthStack = new Stack<Integer>();
//    	
//    	for (Node startNode : space.getStartNodes()) {
//    		nodeStack.push(startNode);
//    		
//    		depthStack.push(new Integer(0));
//    	}
//    	
//    	HashMap<String, Integer> nodeDepths = new HashMap<String, Integer>();
//    	
//    	Set<String> visitedIDs = new HashSet<String>();
//    	
//    	while (!nodeStack.isEmpty()) {
//    		Node node = nodeStack.pop();
//    		
//    		Integer depth = depthStack.pop();
//    		
//    		if (nodeDepths.containsKey(node.getNodeID())) {
//    			Integer minDepth = nodeDepths.get(node.getNodeID());
//    			
//    			if (depth.intValue() < minDepth.intValue()) {
//    				nodeDepths.put(node.getNodeID(), depth);
//    			}
//    		} else {
//    			nodeDepths.put(node.getNodeID(), depth);
//    			
//    			if (depth > maxDepth) {
//    				maxDepth = depth;
//    			}
//    		}
//    		
//    		visitedIDs.add(node.getNodeID());
//    		
//    		if (node.hasEdges()) {
//    			for (Edge edge : node.getEdges()) {
//    				if (!visitedIDs.contains(edge.getHead().getNodeID())) {
//    					nodeStack.push(edge.getHead());
//        				
//        				depthStack.push(new Integer(depth.intValue() + 1));
//    				}
//    			}
//    		}
//    	}
//    	
//    	return nodeDepths;
//    }
    
    private void mergeSurplusNodes(Set<Node> surplusNodes, Set<Edge> mergedEdges, 
    		HashMap<String, Set<Node>> idToOutputNodes, NodeSpace outputSpace) {
    	HashMap<String, Node> idToOutputNode = new HashMap<String, Node>();
    	
    	for (Node surplusNode : surplusNodes) {
			Set<Node> outputNodes = mergeSurplusNode(surplusNode, idToOutputNodes, idToOutputNode,
					outputSpace);
		
			if (surplusNode.hasEdges()) {
				for (Edge surplusEdge : surplusNode.getEdges()) { 
					if (!mergedEdges.contains(surplusEdge)) {
						Set<Node> outputSuccessors = mergeSurplusNode(surplusEdge.getHead(), idToOutputNodes,
								idToOutputNode, outputSpace);

						for (Node outputNode : outputNodes) {
							for (Node outputSuccessor : outputSuccessors) {
								outputNode.copyEdge(surplusEdge, outputSuccessor);

								if (surplusEdge.getHead().isAcceptNode()) {
									outputSuccessor.setNodeType(NodeType.ACCEPT.getValue());
								} else {
									outputSuccessor.clearNodeType();
								}
							}
						}
					}
				}
			}
		}
    }
    
    private Set<Node> mergeSurplusNode(Node surplusNode, HashMap<String, Set<Node>> idToOutputNodes, 
    		HashMap<String, Node> idToOutputNode, NodeSpace outputSpace) {
    	Set<Node> outputNodes;
		
		if (idToOutputNodes.containsKey(surplusNode.getNodeID())) {
			outputNodes = idToOutputNodes.get(surplusNode.getNodeID());
		} else {
			outputNodes = new HashSet<Node>();
			
			Node outputNode;
			
			if (idToOutputNode.containsKey(surplusNode.getNodeID())) {
				outputNode = idToOutputNode.get(surplusNode.getNodeID());
			} else {
				outputNode = outputSpace.copyNode(surplusNode);
				
				idToOutputNode.put(surplusNode.getNodeID(), outputNode);
			}
			
			outputNodes.add(outputNode);
		}
		
		return outputNodes;
    }
    
    public void minimizeDesignSpace(String targetSpaceID) 
    		throws DesignSpaceNotFoundException {
    	validateDesignSpaceOperator(targetSpaceID);

    	DesignSpace targetSpace = loadDesignSpace(targetSpaceID, 2);
    	
    	HashMap<String, Node> nodeIDToNode = targetSpace.mapNodeIDsToNodes();
    	HashMap<String, Set<Edge>> nodeIDsToIncomingEdges = targetSpace.mapNodeIDsToIncomingEdges();
    	
    	Stack<Node> nodeStack = new Stack<Node>();
    	for (Node node : targetSpace.getNodes()) {
    		nodeStack.push(node);
    	}
    	
    	Set<String> deletedNodeIDs = new HashSet<String>();
    	
    	while (nodeStack.size() > 0) {
    		Node node = nodeStack.peek();
    		
    		Set<Edge> minimizableEdges = targetSpace.getMinimizableEdges(node, nodeIDsToIncomingEdges);

    		if (minimizableEdges.size() == 0 || deletedNodeIDs.contains(node.getNodeID())) {
    			nodeStack.pop();
    		} else {
    			for (Edge minimizableEdge : minimizableEdges) {
    				Node predecessor = nodeIDToNode.get(minimizableEdge.getTail().getNodeID());

    				deletedNodeIDs.add(predecessor.getNodeID());

    				if (nodeIDsToIncomingEdges.containsKey(predecessor.getNodeID())) {
    					for (Edge incomingEdge : nodeIDsToIncomingEdges.get(predecessor.getNodeID())) {
    						incomingEdge.setHead(node);
    						nodeIDsToIncomingEdges.get(node.getNodeID()).add(incomingEdge);
    					}
    				}

    				if (minimizableEdges.size() == 1) {
    					for (Edge edge : predecessor.getEdges()) {
    						if (!edge.equals(minimizableEdge)) {
    							nodeIDsToIncomingEdges.get(edge.getHead().getNodeID()).add(node.copyEdge(edge));
    							nodeIDsToIncomingEdges.get(edge.getHead().getNodeID()).remove(edge);
    						}
    					}
    				}

    				if (predecessor.hasNodeType()) {
    					node.setNodeType(predecessor.getNodeType());
    				}
    			}
    			
    			nodeIDsToIncomingEdges.get(node.getNodeID()).removeAll(minimizableEdges);
    		}
    	}
    	
    	Set<Node> deletedNodes = targetSpace.removeNodesByID(deletedNodeIDs);
    	
    	saveDesignSpace(targetSpace);
    	
    	nodeRepository.delete(deletedNodes);
    }
    
    public void orDesignSpaces(List<String> inputSpaceIDs) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateListParameter("inputSpaceIDs", inputSpaceIDs);
    	
    	orDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0));
    }
    
    
    public void orDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID) 
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
    	
    	Set<String> prunedSpaceIDs = new HashSet<String>(inputSpaceIDs);
    	
    	prunedSpaceIDs.remove(outputSpaceID);
    	
    	for (String inputSpaceID : prunedSpaceIDs) {
    		unionDesignSpace(inputSpaceID, outputSpaceID);
    	}
    	
    	deleteNodeCopyIndices(outputSpaceID);

    	Set<String> startNodeIDs = getStartNodeIDs(outputSpaceID);
    
    	if (startNodeIDs.size() > 1) {
    		Node startNodePrime = createTypedNode(outputSpaceID, Node.NodeType.START.getValue());

    		for (String startNodeID : startNodeIDs) {
    			deleteNodeType(outputSpaceID, startNodeID);
    			createEdge(outputSpaceID, startNodePrime.getNodeID(), startNodeID);
    		}
    	}
    	
    	Set<String> acceptNodeIDs = getAcceptNodeIDs(outputSpaceID);
        
    	if (acceptNodeIDs.size() > 1) {
    		Node acceptNodePrime = createTypedNode(outputSpaceID, Node.NodeType.ACCEPT.getValue());

    		for (String acceptNodeID : acceptNodeIDs) {
    			deleteNodeType(outputSpaceID, acceptNodeID);
    			createEdge(outputSpaceID, acceptNodeID, acceptNodePrime.getNodeID());
    		}
    	}
    	
    	if (inputSpaceIDs.contains(outputSpaceID)) {
    		prunedSpaceIDs.add(outputSpaceID);
    	}
    	
    	List<String> headBranchIDs = new LinkedList<String>();
    	
    	for (String inputSpaceID : prunedSpaceIDs) {
    		if (!inputSpaceID.equals(outputSpaceID)) {
    			mergeVersionHistory(inputSpaceID, outputSpaceID);
    		}
    		
    		String headBranchID = getHeadBranchID(inputSpaceID);

    		indexVersionMerger(outputSpaceID, headBranchID);

    		headBranchIDs.add(headBranchID);
    	}
    	
    	orBranches(outputSpaceID, headBranchIDs);

    	if (!inputSpaceIDs.contains(outputSpaceID)) {
    		selectHeadBranch(outputSpaceID, headBranchIDs.get(0));
    	}
    }
    
    public void partitionDesignSpace(String inputSpaceID, String outputSpacePrefix) {
    	DesignSpace inputSpace = loadDesignSpace(inputSpaceID, 2);
    	
    	String headBranchID = getHeadBranchID(inputSpaceID);
    	
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
    		
    		mergeVersionHistory(inputSpaceID, outputSpace.getSpaceID());
    		
    		selectHeadBranch(outputSpace.getSpaceID(), headBranchID);
    		
    		commitToHeadBranch(outputSpace.getSpaceID());
    		
    		i++;
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
	
	private void createEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID) {
		designSpaceRepository.createEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID);
	}
	
	public String createNode(String targetSpaceID) {
    	return designSpaceRepository.createNode(targetSpaceID);
	}
    
	private void createCommit(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.createCommit(targetSpaceID, targetBranchID);
	}

	public void createComponentEdge(String targetSpaceID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		designSpaceRepository.createComponentEdge(targetSpaceID, targetTailID, targetHeadID, componentIDs, componentRoles);
	}

	private void createComponentEdge(String targetSpaceID, String targetBranchID, String targetTailID, String targetHeadID, ArrayList<String> componentIDs, ArrayList<String> componentRoles) {
		designSpaceRepository.createComponentEdge(targetSpaceID, targetBranchID, targetTailID, targetHeadID, componentIDs, componentRoles);
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
	
	// Utility which converts CSV to ArrayList using split operation
	public static ArrayList<String> csvToArrayList(String csvLine) {
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
	
	private void deleteCommits(Set<Commit> deletedCommits) {
		Set<Snapshot> deletedSnapshots = new HashSet<Snapshot>();
		
		for (Commit deletedCommit : deletedCommits) {
			deletedSnapshots.add(deletedCommit.getSnapshot());
		}
		
		deleteSnapshots(deletedSnapshots);
		
		commitRepository.delete(deletedCommits);
	}

	private void deleteCommitCopyIndices(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteCommitCopyIndices(targetSpaceID, targetBranchID);
	}
	
	public void deleteEdge(String targetSpaceID, String targetTailID, String targetHeadID) {
		designSpaceRepository.deleteEdge(targetSpaceID, targetTailID, targetHeadID);
	}
	
	private void deleteEdges(Set<Edge> deletedEdges) {
		edgeRepository.delete(deletedEdges);
	}
	
	public void deleteNode(String targetSpaceID, String targetNodeID) {
		designSpaceRepository.deleteNode(targetSpaceID, targetNodeID);
	}
	
	private void deleteNodes(Set<Node> deletedNodes) {
		nodeRepository.delete(deletedNodes);
	}

	private void deleteNodeCopyIndices(String targetSpaceID) {
    	designSpaceRepository.deleteNodeCopyIndices(targetSpaceID);
    }
    
    private void deleteNodeCopyIndices(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.deleteNodeCopyIndices(targetSpaceID, targetBranchID);
	}

	private void deleteAllNodes(String targetSpaceID) {
		designSpaceRepository.deleteAllNodes(targetSpaceID);
	}

	private void deleteNodeType(String targetSpaceID, String targetNodeID) {
		designSpaceRepository.deleteNodeType(targetSpaceID, targetNodeID);
	}

	private void deleteNodeType(String targetSpaceID, String targetBranchID, String targetNodeID) {
		designSpaceRepository.deleteNodeType(targetSpaceID, targetBranchID, targetNodeID);
	}
	
	private void deleteSnapshots(Set<Snapshot> deletedSnapshots) {
		Set<Node> deletedNodes = new HashSet<Node>();
		
		for (Snapshot deletedSnapshot : deletedSnapshots) {
			deletedNodes.addAll(deletedSnapshot.getNodes());
		}
		
		deleteNodes(deletedNodes);
		
		snapshotRepository.delete(deletedSnapshots);
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
//		return designSpaceRepository.findBySpaceID(targetSpaceID);
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
	
	private Set<String> getAcceptNodeIDs(String targetSpaceID) {
 		return getNodeIDsByType(targetSpaceID, Node.NodeType.ACCEPT.getValue());
	}
	
	private Set<String> getAcceptNodeIDs(String targetSpaceID, String targetBranchID) {
 		return getNodeIDsByType(targetSpaceID, targetBranchID, Node.NodeType.ACCEPT.getValue());
	}
	
	private Set<String> getNodeIDs(String targetSpaceID) {
		return designSpaceRepository.getNodeIDs(targetSpaceID);
	}

	private Set<Node> getNodesByType(String targetSpaceID, String nodeType) {
		return designSpaceRepository.getNodesByType(targetSpaceID, nodeType);
	}

	private Set<Node> getNodesByType(String targetSpaceID, String targetBranchID, String nodeType) {
		return designSpaceRepository.getNodesByType(targetSpaceID, targetBranchID, nodeType);
	}
	
	private Set<String> getNodeIDsByType(String targetSpaceID, String nodeType) {
		return designSpaceRepository.getNodeIDsByType(targetSpaceID, nodeType);
	}

	private Set<String> getNodeIDsByType(String targetSpaceID, String targetBranchID, String nodeType) {
		return designSpaceRepository.getNodeIDsByType(targetSpaceID, targetBranchID, nodeType);
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
	
	private String getStartNodeID(String targetSpaceID) {
		Set<String> startNodeIDs = getNodeIDsByType(targetSpaceID, Node.NodeType.START.getValue());
		if (startNodeIDs.size() > 0) {
			return startNodeIDs.iterator().next();
		} else {
			return null;
		}
	}
	
	private Set<String> getStartNodeIDs(String targetSpaceID) {
 		return getNodeIDsByType(targetSpaceID, Node.NodeType.START.getValue());
	}
	
	private Set<String> getStartNodeIDs(String targetSpaceID, String targetBranchID) {
 		return getNodeIDsByType(targetSpaceID, targetBranchID, Node.NodeType.START.getValue());
	}
	
	public boolean hasBranch(String targetSpaceID, String targetBranchID) {
		return findBranch(targetSpaceID, targetBranchID) != null;
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
	
	private Map<String, Object> mapDesignSpaceToD3Format(DesignSpace space) {
		Map<String, Object> d3Graph = new HashMap<String, Object>();
		
		Map<String, Integer> nodeIndices = new HashMap<String, Integer>();
		
	    List<Map<String,Object>> nodes = new ArrayList<Map<String,Object>>();
	    
	    List<Map<String,Object>> links = new ArrayList<Map<String,Object>>();
	    
	    d3Graph.put("spaceID", space.getSpaceID());
	    
	    for (Node node : space.getNodes()) {
	    	nodes.add(makeD3("nodeID", node.getNodeID(), "nodeType", node.getNodeType()));
	    	
	    	nodeIndices.put(node.getNodeID(), new Integer(nodes.size()));
	    }
	    
	    for (Node node : space.getNodes()) {
	    	if (node.hasEdges()) {
	    		for (Edge edge : node.getEdges()) {
	    			ArrayList<String> compRoles;
	    			
	    			if (edge.hasComponentRoles()) {
	    				compRoles = edge.getComponentRoles();
	    			} else {
	    				compRoles = new ArrayList<String>();
	    			}
	    			
	    			Map<String, Object> link = makeD3("source", nodeIndices.get(edge.getTail().getNodeID()), 
	    					"target", nodeIndices.get(edge.getHead().getNodeID()));
	    			
	    			link.put("componentRoles", compRoles.toArray(new String[compRoles.size()]));
	    			
	    			links.add(link);
	    		}
	    	}
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
	
	private void saveDesignSpace(DesignSpace space) {
//		designSpaceRepository.save(space, 1);
//		
//		System.out.println("nodes saved");
//		
//		designSpaceRepository.save(space, 2);
//		
//		designSpaceRepository.save(space, 3);
//		
//		System.out.println("edges saved");
		designSpaceRepository.save(space);
	}

	private void selectHeadBranch(String targetSpaceID, String targetBranchID) {
		designSpaceRepository.selectHeadBranch(targetSpaceID, targetBranchID);
	}
	
	public void unionBranches(String targetSpaceID, List<String> inputBranchIDs) {
    	unionBranches(targetSpaceID, inputBranchIDs, RESERVED_ID);
    }
	
	public void unionBranches(String targetSpaceID, List<String> inputBranchIDs, String outputBranchID) {
		Set<String> prunedBranchIDs = new HashSet<String>(inputBranchIDs);
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		indexVersionMerger(targetSpaceID, inputBranchID);
    	}
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		mergeBranch(targetSpaceID, inputBranchID, outputBranchID);
    	}
    	
    	createCommit(targetSpaceID, outputBranchID);
    	
    	for (String inputBranchID : prunedBranchIDs) {
    		unionSnapshot(targetSpaceID, inputBranchID, outputBranchID);
    	}
    	
    	deleteNodeCopyIndices(targetSpaceID, outputBranchID);
    	
    	if (outputBranchID.equals(RESERVED_ID)) {
    		for (String inputBranchID : prunedBranchIDs) {
    			fastForwardBranch(targetSpaceID, inputBranchID, RESERVED_ID);
    		}
        	deleteBranch(targetSpaceID, RESERVED_ID);
    	}
	}

	private void unionDesignSpace(String inputSpaceID, String outputSpaceID) {
        designSpaceRepository.unionDesignSpace(inputSpaceID, outputSpaceID);
    }
	
	public void unionDesignSpaces(List<String> inputSpaceIDs) {	
		unionDesignSpaces(inputSpaceIDs, inputSpaceIDs.get(0));
	}
	
	public void unionDesignSpaces(List<String> inputSpaceIDs, String outputSpaceID) {
		Set<String> prunedSpaceIDs = new HashSet<String>(inputSpaceIDs);
		
		prunedSpaceIDs.remove(outputSpaceID);
		
		for (String inputSpaceID : prunedSpaceIDs) {
			unionDesignSpace(inputSpaceID, outputSpaceID);
		}
		
		deleteNodeCopyIndices(outputSpaceID);
		
		if (inputSpaceIDs.contains(outputSpaceID)) {
    		prunedSpaceIDs.add(outputSpaceID);
    	}
    	
    	List<String> headBranchIDs = new LinkedList<String>();
    	
    	for (String inputSpaceID : prunedSpaceIDs) {
    		if (!inputSpaceID.equals(outputSpaceID)) {
    			mergeVersionHistory(inputSpaceID, outputSpaceID);
    		}
    		
    		String headBranchID = getHeadBranchID(inputSpaceID);

    		indexVersionMerger(outputSpaceID, headBranchID);

    		headBranchIDs.add(headBranchID);
    	}
    	
    	unionBranches(outputSpaceID, headBranchIDs);

    	if (!inputSpaceIDs.contains(outputSpaceID)) {
    		selectHeadBranch(outputSpaceID, headBranchIDs.get(0));
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
    
    private void validateCombinationalDesignSpaceOperator(String inputSpaceID1, String inputSpaceID2, String outputSpaceID)
    		throws ParameterEmptyException, DesignSpaceNotFoundException, DesignSpaceConflictException, 
    		DesignSpaceBranchesConflictException {
    	List<String> inputSpaceIDs = new ArrayList<String>(2);
    	inputSpaceIDs.add(inputSpaceID1);
    	inputSpaceIDs.add(inputSpaceID2);
    	validateCombinationalDesignSpaceOperator(inputSpaceIDs, outputSpaceID);
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
    	
    	Set<String> conflictingSpaceIDs = new HashSet<String>();
    	
    	Set<String> conflictingBranchIDs = new HashSet<String>();
    	
    	HashMap<String, String> branchIDToSpaceID = new HashMap<String, String>();
		
		for (String inputSpaceID : inputSpaceIDs) {
			for (String branchID : getBranchIDs(inputSpaceID)) {
				if (!branchIDToSpaceID.containsKey(branchID)) {
					branchIDToSpaceID.put(branchID, inputSpaceID);
				} else if (!branchIDToSpaceID.get(branchID).equals(inputSpaceID)) {
					conflictingSpaceIDs.add(branchIDToSpaceID.get(branchID));
					
					conflictingSpaceIDs.add(inputSpaceID);
					
					conflictingBranchIDs.add(branchID);
				}
			}
		}
    	
    	if (conflictingBranchIDs.size() > 0) {
    		throw new DesignSpaceBranchesConflictException(conflictingSpaceIDs, conflictingBranchIDs);
    	}
    }
    
    private void printSpace(DesignSpace d) {
    	System.out.println(d.getSpaceID());
    	for (Node n : d.getNodes()) {
    		System.out.println(n.getNodeID());
    		if (n.hasEdges()) {
    			for (Edge e : n.getEdges()) {
    				System.out.println(e.getTail().getNodeID() + "-" + e.getHead().getNodeID());
    			}
    		}
    	}
    }
    
}
