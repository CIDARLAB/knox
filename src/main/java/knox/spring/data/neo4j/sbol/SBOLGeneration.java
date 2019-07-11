package knox.spring.data.neo4j.sbol;

import knox.spring.data.neo4j.domain.DesignSpace;
import org.sbolstandard.core2.SBOLDocument;

public class SBOLGeneration {

    private String namespace = "http://knox.org";

    private DesignSpace targetSpace;


    public SBOLGeneration(DesignSpace targetSpace, String namespace) {
        if (namespace != null && namespace.length() != 0) {
            this.namespace = namespace;
        }
        this.targetSpace = targetSpace;
    }

    public SBOLDocument createSBOLDocument(){
        SBOLDocument document = new SBOLDocument();
        document.setDefaultURIprefix(namespace);

        // TODO convert targetSpace to sbol doc

        return document;
    }

}
