package knox.spring.data.neo4j.sbol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import knox.spring.data.neo4j.domain.Edge;
import org.sbolstandard.core2.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.operations.JoinOperator;
import knox.spring.data.neo4j.operations.OROperator;
import knox.spring.data.neo4j.operations.RepeatOperator;
import knox.spring.data.neo4j.exception.SBOLException;

public class SBOLConversion {

	private List<SBOLDocument> sbolDocs;

	private static final Logger LOG = LoggerFactory.getLogger(SBOLConversion.class);

	public List<SBOLDocument> getSbolDoc() {
		return sbolDocs;
	}

	public void setSbolDoc(List<SBOLDocument> sbolDocs) {
		this.sbolDocs = sbolDocs;
	}

	/**
	 * Iterates through all the uploaded SBOL documents and
	 * calls the appropriate SBOL parser by checking for CombinatorialDerivations
	 * @return
	 */
	public List<DesignSpace> convertSBOLsToSpaces() throws SBOLException{

		List<DesignSpace> allOutputSpaces = new ArrayList<>();

		for(SBOLDocument sbolDoc: sbolDocs){
			Set<CombinatorialDerivation> derivations = sbolDoc.getCombinatorialDerivations();
			if (derivations.isEmpty()){
				allOutputSpaces.addAll(convertSBOL(sbolDoc));
			}

			allOutputSpaces.addAll(convertCombinatorialSBOL(sbolDoc));
		}

		return allOutputSpaces;
	}

	/**
	 * Creates one outputSpace per root CombinatorialDerivation
	 * @param sbolDoc
	 * @return list of design spaces
	 */
	private List<DesignSpace> convertCombinatorialSBOL(SBOLDocument sbolDoc) throws SBOLException{

		List<DesignSpace> outputSpaces = new ArrayList<>();
		Set<CombinatorialDerivation> rootCVs = getRootCombinatorialDerivation(sbolDoc);

		for(CombinatorialDerivation rootCV: rootCVs){
			//iterate through variable components
			List<NodeSpace> inputSpace = recurseVariableComponents(rootCV);

			DesignSpace outputSpace = new DesignSpace(rootCV.getDisplayId());
			JoinOperator.apply(inputSpace, outputSpace);

			// Create version history for output design space - single branch and single commit that captures result of conversion
			outputSpace.createHeadBranch(rootCV.getDisplayId());
			outputSpace.commitToHead();
			outputSpaces.add(outputSpace);
		}

		return outputSpaces;
	}

	private List<NodeSpace> recurseVariableComponents(CombinatorialDerivation combinatorialDerivation) throws SBOLException{
		ComponentDefinition template = combinatorialDerivation.getTemplate();
		List<NodeSpace> inputSpace = new LinkedList<>();

		// order components by sequence constraints
		VariableComponent[] sortedVCs = sortVariableComponents(combinatorialDerivation);
		for (VariableComponent variableComponent : sortedVCs) {
			if (variableComponent != null) {
				// recurse through variant derivations
				Set<CombinatorialDerivation> variantDerivs = variableComponent.getVariantDerivations();

				Boolean hasVariants = !variableComponent.getVariants().isEmpty() || !variableComponent.getVariantCollections().isEmpty();

				//handle structure for just repeats
				if (variantDerivs.size() == 1 && !hasVariants){
					for (CombinatorialDerivation cv : variantDerivs) {
						inputSpace.add(applyOperator(variableComponent.getOperator(), recurseVariableComponents(cv)));
					}
				}

				//else handle collapsed complex ORs
				else if (variantDerivs.size() > 0){
					List<NodeSpace> orSpace = new LinkedList<>();
					NodeSpace outputSpace = new NodeSpace();

					if (hasVariants){
						orSpace.add(createNodeSpaceFromVariableComponent(variableComponent, template)); //add variants
					}

					for (CombinatorialDerivation cv : variantDerivs) {
						orSpace.add(applyOperator(OperatorType.ONE, recurseVariableComponents(cv)));
					}

					OROperator.apply(orSpace, outputSpace); //"or" all the elements in the list
					List<NodeSpace> tempSpace = new LinkedList<>();
					tempSpace.add(outputSpace);
					inputSpace.add(applyOperator(variableComponent.getOperator(), tempSpace));
				}

				else if (hasVariants){
					inputSpace.add(createNodeSpaceFromVariableComponent(variableComponent, template));
				}
			}
		}

		return inputSpace;
	}

	private VariableComponent[] sortVariableComponents(CombinatorialDerivation combinatorialDerivation){
		//make ordered components from sequence constraints
		List<Component> orderedComponents = new ArrayList<>();
		
		ComponentDefinition template = combinatorialDerivation.getTemplate();
		Set<SequenceConstraint> seqConstraints = template.getSequenceConstraints();
		
		//check if total ordering
		Set<URI> subjectURIs = new HashSet<URI>();
		Set<URI> objectURIs = new HashSet<URI>();
		
		Set<URI> firstURI = new HashSet<URI>();
		Set<URI> lastURI = new HashSet<URI>();
		
		HashMap<URI,URI> precedesMap = new HashMap<URI,URI>();
		
		for (SequenceConstraint constraint : seqConstraints) {
			if (constraint.getRestriction().equals(RestrictionType.PRECEDES)) {
				subjectURIs.add(constraint.getSubjectURI());
				objectURIs.add(constraint.getObjectURI());

				firstURI.add(constraint.getSubjectURI());
				lastURI.add(constraint.getObjectURI());
				
				precedesMap.put(constraint.getSubjectURI(), constraint.getObjectURI());
			}
		}
		
		firstURI.removeAll(objectURIs);
		lastURI.removeAll(subjectURIs);
		
		boolean totalOrdering;
		
		if (firstURI.size() == 1 && lastURI.size() == 1) {
			URI currentURI = firstURI.iterator().next();
			
			orderedComponents.add(template.getComponent(currentURI));
			
			while (precedesMap.containsKey(currentURI)) {
				currentURI = precedesMap.get(currentURI);
				
				orderedComponents.add(template.getComponent(currentURI));
			}
			
			totalOrdering = currentURI.equals(lastURI.iterator().next());
		} else {
			totalOrdering = false;
		}
		
		if (!totalOrdering) {
			orderedComponents.clear();
			
			for (SequenceConstraint constraint : seqConstraints) {
				//subject precedes object
				Component subject = constraint.getSubject();
				Component object = constraint.getObject();
				int subIndex = orderedComponents.indexOf(subject);
				int objIndex = orderedComponents.indexOf(object);
				if (subIndex == -1 && objIndex == -1){
					orderedComponents.add(subject);
					orderedComponents.add(object);
				}else if(subIndex > -1){
					orderedComponents.add(subIndex+1, object);
				}else if(objIndex > -1){
					orderedComponents.add(objIndex, subject);
				}
			}
		}

		//order variable components based on components array
		VariableComponent[] orderedVCs;
		Set<VariableComponent> vcSet = combinatorialDerivation.getVariableComponents();
		if (vcSet.size() > 1){
			orderedVCs = new VariableComponent[orderedComponents.size()];
			for (VariableComponent variableComponent : vcSet) {
				int index = orderedComponents.indexOf(variableComponent.getVariable());
				orderedVCs[index] = variableComponent;
			}
		}else{
			orderedVCs = new VariableComponent[1];
			orderedVCs[0] = vcSet.iterator().next();
		}

		return orderedVCs;
	}
	
	private Set<String> getSOTermsByAccessionURI(URI accessionURI) {
		Set<String> soTerms = new HashSet<String>();
		
		switch (accessionURI.toString()) {
		case "http://identifiers.org/so/SO:0000031":
			soTerms.add("aptamer");
			break;
		case "http://identifiers.org/so/SO:0001953":
			soTerms.add("restriction_enzyme_assembly_scar");
			break;
		case "http://identifiers.org/so/SO:0001691":
			soTerms.add("blunt_end_restriction_enzyme_cleavage_site");
			break;
		case "http://identifiers.org/so/SO:0000316":
			soTerms.add("CDS");
			break;
		case "http://identifiers.org/so/SO:0001955":
			soTerms.add("protein_stability_element");
			break;
		case "http://identifiers.org/so/SO:0000804":
			soTerms.add("engineered_region");
			break;
		case "http://identifiers.org/so/SO:0001932":
			soTerms.add("restriction_enzyme_five_prime_single_strand_overhang");
			break;
		case "http://identifiers.org/so/SO:0001975":
			soTerms.add("five_prime_sticky_end_restriction_enzyme_cleavage_site");
			break;
		case "http://identifiers.org/so/SO:0000627":
			soTerms.add("insulator");
			break;
		case "http://identifiers.org/so/SO:0001263":
			soTerms.add("ncRNA_gene");
			break;
		case "http://identifiers.org/so/SO:0000834":
			soTerms.add("mature_transcript_region");
			break;
		case "http://identifiers.org/so/SO:0001688":
			soTerms.add("restriction_enzyme_cleavage_junction");
			break;
		case "http://identifiers.org/so/SO:0001687":
			soTerms.add("restriction_enzyme_recognition_site");
			break;
		case "http://identifiers.org/so/SO:0000057":
			soTerms.add("operator");
			break;
		case "http://identifiers.org/so/SO:0000409":
			soTerms.add("binding_site");
			break;
		case "http://identifiers.org/so/SO:0000296":
			soTerms.add("origin_of_replication");
			break;
		case "http://identifiers.org/so/SO:0000724":
			soTerms.add("oriT");
			break;
		case "http://identifiers.org/so/SO:0000553":
			soTerms.add("polyA_site");
			break;
		case "http://identifiers.org/so/SO:0005850":
			soTerms.add("primer_binding_site");
			break;
		case "http://identifiers.org/so/SO:0000167":
			soTerms.add("promoter");
			break;
		case "http://identifiers.org/so/SO:0001956":
			soTerms.add("protease_site");
			break;
		case "http://identifiers.org/so/SO:0001546":
			soTerms.add("transcript_stability_variant");
			break;
		case "http://identifiers.org/so/SO:0001977":
			soTerms.add("ribonuclease_site");
			break;
		case "http://identifiers.org/so/SO:0000139":
			soTerms.add("ribosome_entry_site");
			break;
		case "http://identifiers.org/so/SO:0000374":
			soTerms.add("ribozyme");
			break;
		case "http://identifiers.org/so/SO:0001979":
			soTerms.add("RNA_stability_element");
			break;
		case "http://identifiers.org/so/SO:0001978":
			soTerms.add("signature");
			break;
		case "http://identifiers.org/so/SO:0000299":
			soTerms.add("specific_recombination_site");
			break;
		case "http://identifiers.org/so/SO:0000141":
			soTerms.add("terminator");
			break;
		case "http://identifiers.org/so/SO:0001933":
			soTerms.add("restriction_enzyme_three_prime_single_strand_overhang");
			break;
		case "http://identifiers.org/so/SO:0001976":
			soTerms.add("three_prime_sticky_end_restriction_enzyme_cleavage_site");
			break;
		case "http://identifiers.org/so/SO:0000616":
			soTerms.add("transcription_end_site");
			break;
		case "http://identifiers.org/so/SO:0000319":
			soTerms.add("stop_codon");
			break;
		case "http://identifiers.org/so/SO:0000327":
			soTerms.add("coding_end");
			break;
		}
		
		return soTerms;
	}
	
	private Set<URI> getAccessionURIsBySOTerm(String soTerm) {
		Set<URI> accessionURIs = new HashSet<URI>();
		
		try {
			switch (soTerm) {
			case "aptamer":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000031"));
				break;
			case "restriction_enzyme_assembly_scar":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001953"));
				break;
			case "blunt_end_restriction_enzyme_cleavage_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001691"));
				break;
			case "CDS":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000316"));
				break;
			case "protein_stability_element":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001955"));
				break;
			case "engineered_region":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000804"));
				break;
			case "restriction_enzyme_five_prime_single_strand_overhang":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001932"));
				break;
			case "five_prime_sticky_end_restriction_enzyme_cleavage_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001975"));
				break;
			case "ribozyme":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000374"));
				break;
			case "insulator":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000627"));
				break;
			case "ncRNA_gene":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001263"));
				break;
			case "mature_transcript_region":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000834"));
				break;
			case "restriction_enzyme_cleavage_junction":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001688"));
				break;
			case "restriction_enzyme_recognition_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001687"));
				break;
			case "operator":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000057"));
				break;
			case "binding_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000409"));
				break;
			case "origin_of_replication":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000296"));
				break;
			case "oriT":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000724"));
				break;
			case "polyA_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000553"));
				break;
			case "primer_binding_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0005850"));
				break;
			case "promoter":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000167"));
				break;
			case "protease_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001956"));
				break;
			case "transcript_stability_variant":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001546"));
				break;
			case "ribonuclease_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001977"));
				break;
			case "ribosome_entry_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000139"));
				break;
			case "RNA_stability_element":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001979"));
				break;
			case "signature":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001978"));
				break;
			case "specific_recombination_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000299"));
				break;
			case "terminator":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000141"));
				break;
			case "restriction_enzyme_three_prime_single_strand_overhang":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001933"));
				break;
			case "three_prime_sticky_end_restriction_enzyme_cleavage_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0001976"));
				break;
			case "transcription_end_site":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000616"));
				break;
			case "stop_codon":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000319"));
				break;
			case "coding_end":
				accessionURIs.add(new URI("http://identifiers.org/so/SO:0000327"));
				break;
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return accessionURIs;
	}
	
	private Set<String> getSOTermsByAccessionURIs(Set<URI> accessionURIs) {
		Set<String> soTerms = new HashSet<String>();
		
		for (URI accessionURI : accessionURIs) {
			soTerms.addAll(getSOTermsByAccessionURI(accessionURI));
		}
		
		return soTerms;
	}

	private NodeSpace createNodeSpaceFromVariableComponent(VariableComponent variableComponent, ComponentDefinition template) throws SBOLException{
		ArrayList<String> atomIDs = new ArrayList<>();
		ArrayList<String> atomRoles = new ArrayList<>();

		Component variable = variableComponent.getVariable();
		ComponentDefinition variableDefinition = variable.getDefinition();

		// Find variant roles
		for (ComponentDefinition variant : variableComponent.getVariants()) {
			Set<URI> roles = variant.getRoles().isEmpty()? variableDefinition.getRoles(): variant.getRoles();
			Set<String> roleTerms = getSOTermsByAccessionURIs(roles);
			
			if (roleTerms.contains(variant.getDisplayId()) && variant.getSequences().isEmpty()) {
				for (URI role : getAccessionURIsBySOTerm(variant.getDisplayId())) {
					atomRoles.add(role.toString());
				}
			} else {
				for (URI role : roles) {
					atomIDs.add(variant.getIdentity().toString());
					atomRoles.add(role.toString());
				}
			}
		}

		// Find collection roles
		for (org.sbolstandard.core2.Collection collection : variableComponent.getVariantCollections()) {
			for (TopLevel member: collection.getMembers()){
				if (member.getClass() == ComponentDefinition.class){
					ComponentDefinition def = (ComponentDefinition) member;
					
					Set<URI> roles = def.getRoles().isEmpty()? variableDefinition.getRoles(): def.getRoles();
					Set<String> roleTerms = getSOTermsByAccessionURIs(roles);
					
					if (roleTerms.contains(def.getDisplayId()) && def.getSequences().isEmpty()) {
						for (URI role : getAccessionURIsBySOTerm(def.getDisplayId())) {
							atomRoles.add(role.toString());
						}
					} else {
						for (URI role : roles) {
							atomIDs.add(def.getIdentity().toString());
							atomRoles.add(role.toString());
						}
					}
				}
			}
		}

		//find orientation
		Edge.Orientation orientation = getOrientation(variableComponent.getVariable(), template);

		//create space
		List<NodeSpace> inputSpace = new LinkedList<>();
		NodeSpace newSpace;

		// if unspecified, create both INLINE and REVERSE
		if(orientation == Edge.Orientation.UNDECLARED){
			newSpace = new NodeSpace(atomIDs, atomRoles, Edge.Orientation.INLINE);
			for(Edge edge : newSpace.getStartNode().getEdges()){
				Edge duplicateEdge = edge.copy(edge.getTail(), edge.getHead());
				duplicateEdge.reverseOrientation();
				newSpace.getStartNode().addEdge(duplicateEdge);
			}
		} else {
			newSpace = new NodeSpace(atomIDs, atomRoles, orientation);
		}

		inputSpace.add(newSpace);

		//check operator
		return applyOperator(variableComponent.getOperator(), inputSpace);
	}

	private Edge.Orientation getOrientation(Component component, ComponentDefinition template){
		SequenceAnnotation annotation = template.getSequenceAnnotation(component);

		if(Objects.nonNull(annotation)){
			// throw error if there is more than one location
			if(annotation.getLocations().size() > 1){
				throw new SBOLException("Cannot parse SBOL with more than one Location in SequenceAnnotation");
			}

			OrientationType orientation = annotation.getLocations().iterator().next().getOrientation();

			//if orientation does not exist, then it's implicit that the component can be inline or reverse complement
			if(orientation == null){
				return Edge.Orientation.UNDECLARED;
			}
			if(orientation == OrientationType.INLINE){
				return Edge.Orientation.INLINE;
			}
			if(orientation == OrientationType.REVERSECOMPLEMENT){
				return Edge.Orientation.REVERSE_COMPLEMENT;
			}
		}

		return Edge.Orientation.INLINE;
	}

	private NodeSpace applyOperator(OperatorType operator, List<NodeSpace> inputSpace){
		NodeSpace outputSpace = new NodeSpace();

		if (operator == OperatorType.ONEORMORE){
			RepeatOperator.apply(inputSpace, outputSpace, false);
		}
		if (operator == OperatorType.ZEROORMORE){
			RepeatOperator.apply(inputSpace, outputSpace, true);
		}
		if (operator == OperatorType.ZEROORONE){
			inputSpace.add(new NodeSpace(new ArrayList<>(), new ArrayList<>()));
			OROperator.apply(inputSpace, outputSpace);
		}
		if (operator == OperatorType.ONE){
			JoinOperator.apply(inputSpace, outputSpace);
		}
		return outputSpace;
	}

	private Set<CombinatorialDerivation> getRootCombinatorialDerivation(SBOLDocument doc){
		Set<CombinatorialDerivation> derivations = doc.getCombinatorialDerivations();
		for (CombinatorialDerivation combDerivation : doc.getCombinatorialDerivations()) {
			for (VariableComponent vc: combDerivation.getVariableComponents()){
				derivations.removeAll(vc.getVariantDerivations());
			}
		}
		return derivations;
	}

	private List<DesignSpace> convertSBOL(SBOLDocument sbolDoc) {
		List<DesignSpace> outputSpaces = new ArrayList<>();
		Set<ComponentDefinition> rootDefs = getRootDNAComponentDefinitions(sbolDoc);

		for (ComponentDefinition rootDef : rootDefs) {
			List<ComponentDefinition> leafDefs = flattenComponentDefinition(rootDef);
			NodeSpace unionSpace = new NodeSpace(0);
			Node currentNode = unionSpace.createStartNode();

			for (int i = 0; i < leafDefs.size(); i++) {
				ArrayList<String> compIDs = new ArrayList<String>();
				ArrayList<String> compRoles = new ArrayList<String>();
				
				for (URI role : leafDefs.get(i).getRoles()) {
					compIDs.add(leafDefs.get(i).getIdentity().toString());
					compRoles.add(role.toString());
				}

				Node nextNode;

				if (i < leafDefs.size() - 1) {
					nextNode = unionSpace.createNode();
				} else {
					nextNode = unionSpace.createAcceptNode();
				}

				currentNode.createEdge(nextNode, compIDs, compRoles);

				currentNode = nextNode;
			}

			// create a separate design space per root CD
			DesignSpace outputSpace = new DesignSpace(rootDef.getDisplayId());
			outputSpace.shallowCopyNodeSpace(unionSpace);

			// create version history
			outputSpace.createHeadBranch(rootDef.getDisplayId());
			outputSpace.commitToHead();

			outputSpaces.add(outputSpace);
		}

		return outputSpaces;
	}
	
	private static List<ComponentDefinition> flattenComponentDefinition(ComponentDefinition rootDef) {
		List<ComponentDefinition> leafDefs = new LinkedList<ComponentDefinition>();
		
		if (rootDef.getSequences().isEmpty() || rootDef.getSequenceAnnotations().isEmpty()) {
			leafDefs.add(rootDef);
			
			return leafDefs;
		} else {
			List<SequenceAnnotation> sortedSeqAnnos = rootDef.getSortedSequenceAnnotations();
			
			List<SequenceAnnotation> sortedCompAnnos = new LinkedList<SequenceAnnotation>();
			
			for (SequenceAnnotation seqAnno : sortedSeqAnnos) {
				if (seqAnno.isSetComponent()) {
					sortedCompAnnos.add(seqAnno);
				}
			}
			
			if (!sortedCompAnnos.isEmpty()) {
				SequenceAnnotation seqAnno = sortedCompAnnos.get(0);
				
				List<Range> sortedRanges = new LinkedList<Range>();
				
				for (Location loc : seqAnno.getSortedLocations()) {
					if (loc instanceof Range) {
						sortedRanges.add((Range) loc);
					}
				}
				
				if (sortedRanges.isEmpty() || sortedRanges.get(0).getStart() > 1) {
					leafDefs.add(rootDef);
					
					return leafDefs;
				}
			}
			
			List<Component> leafComps = new LinkedList<Component>();
			
			int lastEnd = 0;
			
			for (SequenceAnnotation compAnno : sortedCompAnnos) {
				List<Range> sortedRanges = new LinkedList<Range>();
				
				for (Location loc : compAnno.getSortedLocations()) {
					if (loc instanceof Range) {
						sortedRanges.add((Range) loc);
					}
				}
				
				if (lastEnd > 0 && sortedRanges.get(0).getStart() != lastEnd + 1) {
					leafDefs.add(rootDef);
					
					return leafDefs;
				}
				
				for (int i = 1; i < sortedRanges.size(); i++) {
					if (sortedRanges.get(i).getStart() != sortedRanges.get(i - 1).getEnd() + 1) {
						leafDefs.add(rootDef);
						
						return leafDefs;
					}
				}

				leafComps.add(compAnno.getComponent());
				
				lastEnd = sortedRanges.get(sortedRanges.size() - 1).getEnd();
			}
			
			if (lastEnd != rootDef.getSequenceByEncoding(Sequence.IUPAC_DNA).getElements().length()) {
				leafDefs.add(rootDef);
			
				return leafDefs;
			}
			
			for (Component leafComp : leafComps) {
				ComponentDefinition leafDef = leafComp.getDefinition();
				
				if (leafDef == null) {
					leafDefs = new LinkedList<ComponentDefinition>();
					
					leafDefs.add(rootDef);
					
					return leafDefs;
				} else {
					leafDefs.addAll(flattenComponentDefinition(leafDef));
				}
			}
			
			return leafDefs;
		}
	}
	
	private static Set<ComponentDefinition> getRootDNAComponentDefinitions(SBOLDocument sbolDoc) {
    	Set<ComponentDefinition> dnaCompDefs = new HashSet<ComponentDefinition>();
    	
    	for (ComponentDefinition compDef : sbolDoc.getRootComponentDefinitions()) {
			if (compDef.getTypes().contains(ComponentDefinition.DNA)) {
				dnaCompDefs.add(compDef);
			}
		}
    	return dnaCompDefs;
    }
}
