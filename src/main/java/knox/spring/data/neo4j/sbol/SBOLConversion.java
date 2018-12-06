package knox.spring.data.neo4j.sbol;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Location;
import org.sbolstandard.core2.Range;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.Sequence;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceOntology;
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
		// Create a list of component IDs and a list of component roles and use them to create a node space with a single edge for each atom
		// Should replace with code that extracts this information from sbolDocs
		
		ArrayList<String> atom1IDs = new ArrayList<String>();
		atom1IDs.add("pTac");
		
		ArrayList<String> atom1Roles = new ArrayList<String>();
		atom1Roles.add("promoter");
		
		ArrayList<String> atom2IDs = new ArrayList<String>();
		atom1IDs.add("gfp");
		
		ArrayList<String> atom2Roles = new ArrayList<String>();
		atom1Roles.add("CDS");
		
		List<NodeSpace> inputSpaces = new LinkedList<NodeSpace>();
		
		inputSpaces.add(new NodeSpace(atom1IDs, atom1Roles));
		inputSpaces.add(new NodeSpace(atom2IDs, atom2Roles));
		
		// Create output space and apply operators based on combinatorial SBOL
		// Should replace with code that extracts this information from sbolDocs
		// The final output should be a design space rather than a node space so that version history can be added to it
		
		DesignSpace outputSpace = new DesignSpace(outputSpaceID);
		
		JoinOperator.apply(inputSpaces, outputSpace); // Note that an input space can also be the output space of an operator - its graph will be overwritten by the result of applying the operator
		
		// Create version history for output design space - single branch and single commit that captures result of conversion
		
		outputSpace.createHeadBranch(outputSpaceID);
		
		outputSpace.commitToHead();
		
		return outputSpace;
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
