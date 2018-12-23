package knox.spring.data.neo4j.sbol;

import java.net.URI;
import java.util.*;

import org.sbolstandard.core2.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.operations.JoinOperator;
import knox.spring.data.neo4j.operations.OROperator;
import knox.spring.data.neo4j.operations.RepeatOperator;
import knox.spring.data.neo4j.operations.Union;

public class SBOLConversion {
	
	List<SBOLDocument> sbolDocs;
	
	private static final Logger LOG = LoggerFactory.getLogger(SBOLConversion.class);
	
	public SBOLConversion(List<SBOLDocument> sbolDocs) {
		this.sbolDocs = sbolDocs;
	}
	
	public DesignSpace convertCombinatorialSBOLToSpace(String outputSpaceID) {
		SBOLDocument doc = sbolDocs.get(0);

		CombinatorialDerivation rootCV = getRootCombinatorialDerivation(doc);

		//iterate through variable components
		List<NodeSpace> inputSpace = recurseVariableComponents(rootCV);
		
		DesignSpace outputSpace = new DesignSpace(outputSpaceID);
		JoinOperator.apply(inputSpace, outputSpace);
		
		// Create version history for output design space - single branch and single commit that captures result of conversion
		outputSpace.createHeadBranch(outputSpaceID);
		outputSpace.commitToHead();
		
		return outputSpace;
	}

	private List<NodeSpace> recurseVariableComponents(CombinatorialDerivation combinatorialDerivation){
		List<NodeSpace> inputSpace = new LinkedList<>();

		// order components by sequence constraints
		VariableComponent[] sortedVCs = sortVariableComponents(combinatorialDerivation);
		for (VariableComponent variableComponent : sortedVCs) {
			//get variant derivations first to find nested components
			Set<CombinatorialDerivation> variantDerivs = variableComponent.getVariantDerivations();
			if (variantDerivs.size() > 0){
				for (CombinatorialDerivation cv : variantDerivs) {
					inputSpace.add(applyOperator(variableComponent.getOperator(), recurseVariableComponents(cv)));
				}
			}else{
				inputSpace.add(createNodeSpaceFromVariableComponent(variableComponent));
			}
		}

		return inputSpace;
	}

	private VariableComponent[] sortVariableComponents(CombinatorialDerivation combinatorialDerivation){
		//make ordered components from sequence constraints
		List<Component> orderedComponents = new ArrayList<>();
		Set<SequenceConstraint> seqConstraints = combinatorialDerivation.getTemplate().getSequenceConstraints();
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

	private NodeSpace createNodeSpaceFromVariableComponent(VariableComponent variableComponent){
		ArrayList<String> atomIDs = new ArrayList<>();
		Set<String> atomRolesSet = new HashSet<>();
		for (ComponentDefinition variant : variableComponent.getVariants()) {
			atomIDs.add(variant.getDisplayId());
			atomRolesSet.addAll(convertSOIdentifiersToNames(variableComponent.getVariable().getDefinition().getRoles()));
		}

		//convert set to list
		ArrayList<String> atomRoles = new ArrayList<>(atomRolesSet);

		System.out.println(atomIDs);
		System.out.println(atomRoles);

		//create space
		List<NodeSpace> inputSpace = new LinkedList<>();
		inputSpace.add(new NodeSpace(atomIDs, atomRoles));

		//check operator
		return applyOperator(variableComponent.getOperator(), inputSpace);
	}

	private NodeSpace applyOperator(OperatorType operator, List<NodeSpace> inputSpace){
		NodeSpace outputSpace = new NodeSpace();
		if (operator == OperatorType.ONEORMORE){
			RepeatOperator.apply(inputSpace, outputSpace, false);
		}
		if (operator == OperatorType.ZEROORMORE){
			RepeatOperator.apply(inputSpace, outputSpace, true);
		}
		if (operator == OperatorType.ONE){
			JoinOperator.apply(inputSpace, outputSpace);
		}
		return outputSpace;
	}

	private CombinatorialDerivation getRootCombinatorialDerivation(SBOLDocument doc){
		Set<CombinatorialDerivation> derivations = doc.getCombinatorialDerivations();
		for (CombinatorialDerivation combDerivation : doc.getCombinatorialDerivations()) {
			for (VariableComponent vc: combDerivation.getVariableComponents()){
				derivations.removeAll(vc.getVariantDerivations());
			}
		}
		return derivations.iterator().next();
	}
	
	public DesignSpace convertSBOLToSpace(String outputSpaceID) {
		NodeSpace unionSpace = new NodeSpace(0);
		
		for (SBOLDocument sbolDoc : sbolDocs) {
    		Set<ComponentDefinition> rootDefs = getRootDNAComponentDefinitions(sbolDoc);

    		for (ComponentDefinition rootDef : rootDefs) {
    			List<ComponentDefinition> leafDefs = flattenComponentDefinition(rootDef);

    			Node currentNode = unionSpace.createStartNode();

    			for (int i = 0; i < leafDefs.size(); i++) {
    				ArrayList<String> compIDs = new ArrayList<String>();

    				compIDs.add(leafDefs.get(i).getIdentity().toString());

    				ArrayList<String> compRoles = new ArrayList<String>(
    						convertSOIdentifiersToNames(leafDefs.get(i).getRoles()));

    				Node nextNode;
    				
    				if (i < leafDefs.size() - 1) {
    					nextNode = unionSpace.createNode();
    				} else {
    					nextNode = unionSpace.createAcceptNode();
    				}

    				currentNode.createEdge(nextNode, compIDs, compRoles);

    				currentNode = nextNode;
    			}
    			
    		}
    	}
		
		DesignSpace outputSpace = new DesignSpace(outputSpaceID);
		
		if (unionSpace.getNumStartNodes() > 1) {
			Union union = new Union(unionSpace);

			union.apply();

			union.getSpace().minimizeEdges();
			
			outputSpace.shallowCopyNodeSpace(union.getSpace());
		} else {
			outputSpace.shallowCopyNodeSpace(unionSpace);
		}
		
		outputSpace.createHeadBranch(outputSpaceID);
		
		outputSpace.commitToHead();
		
		return outputSpace;
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
	
	private static Set<String> convertSOIdentifiersToNames(Set<URI> soIdentifiers) {
        Set<String> roleNames = new HashSet<String>();
        
        if (soIdentifiers.isEmpty()) {
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
                } else if (soIdentifier.equals(
                               SequenceOntology.RIBOSOME_ENTRY_SITE)) {
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

}
