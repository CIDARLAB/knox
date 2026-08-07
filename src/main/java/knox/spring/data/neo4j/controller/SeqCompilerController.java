package knox.spring.data.neo4j.controller;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;

import knox.spring.data.neo4j.services.DesignSpaceService;

@RestController
public class SeqCompilerController {
    private final RestTemplate restTemplate;
    final DesignSpaceService designSpaceService;

    @Value("${seq.compiler.url}")
    private String seqCompilerUrl; // SeqCompiler API URL

    public SeqCompilerController(RestTemplate restTemplate, DesignSpaceService designSpaceService) {
        this.restTemplate = restTemplate;
        this.designSpaceService = designSpaceService;
    }

    public record CompileResponse(
        String dna_template,
        String rna_template,
        List<String> dna_part_IDs,
        List<String> rna_part_IDs,
        List<String> dna_part_types,
        List<String> rna_part_types,
        String genbank_dna,
        String genbank_rna) {}
        
    public record CompileRequest(
        String name,
        String Rz,
        String L,
        String term,
        String hp5,
        String prom,
        String eI,
        String eO,
        String s,
        int invert,
        String invL,
        String agL,
        int AGiloop,
        int otype,
        int rna,
        List<String> us,
        List<String> ds,
        int temp_len,
        String cp,
        String n,
        int c,
        String d,
        String CDS,
        String rflap) {}

    @PostMapping("/seqcompiler/compile")
    public CompileResponse compileSequence(
        @RequestParam(value = "spaceID", required = true) String spaceID,
        @RequestParam(value = "groupID", required = true) String groupID,
        @RequestParam(value = "weight", required = false, defaultValue = "0.0") Double weight,

        @RequestParam(value = "name", required = true) String name,
        @RequestParam(value = "Rz", required = false, defaultValue = "Ro") String Rz,
        @RequestParam(value = "L", required = false, defaultValue = "L") String L,
        @RequestParam(value = "term", required = false, defaultValue = "T7t") String term,
        @RequestParam(value = "hp5", required = false, defaultValue = "5hp") String hp5,
        @RequestParam(value = "prom", required = false, defaultValue = "T7p") String prom,
        @RequestParam(value = "eI", required = false, defaultValue = "") String eI,
        @RequestParam(value = "eO", required = false, defaultValue = "") String eO,
        @RequestParam(value = "s", required = false, defaultValue = "") String s,
        @RequestParam(value = "invert", required = false, defaultValue = "0") int invert,
        @RequestParam(value = "invL", required = false, defaultValue = "A") String invL,
        @RequestParam(value = "agL", required = false, defaultValue = "TA") String agL,
        @RequestParam(value = "AGiloop", required = false, defaultValue = "5") int AGiloop,
        @RequestParam(value = "otype", required = false, defaultValue = "1") int otype,
        @RequestParam(value = "rna", required = false, defaultValue = "0") int rna,
        @RequestParam(value = "us", required = false, defaultValue = "") List<String> us,
        @RequestParam(value = "ds", required = false, defaultValue = "") List<String> ds,
        @RequestParam(value = "temp_len", required = false, defaultValue = "0") int temp_len,
        @RequestParam(value = "cp", required = false, defaultValue = "") String cp,
        @RequestParam(value = "n", required = false, defaultValue = "") String n,
        @RequestParam(value = "c", required = false, defaultValue = "0") int c,
        @RequestParam(value = "d", required = false, defaultValue = "") String d,
        @RequestParam(value = "CDS", required = false, defaultValue = "") String CDS,
        @RequestParam(value = "rflap", required = false, defaultValue = "") String rflap
    ) {
        System.out.println("Received seqcompile request:");

        CompileRequest request = new CompileRequest(
            name, Rz, L, term, hp5, prom, eI, eO, s, invert, invL, agL,
            AGiloop, otype, rna, us, ds, temp_len, cp, n, c, d, CDS, rflap
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<CompileRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<CompileResponse> response = restTemplate.exchange(
                    seqCompilerUrl + "/compile",
                    HttpMethod.POST,
                    entity,
                    CompileResponse.class
            );

            if (rna == 1) {
                designSpaceService.createDesignSpace(
                    spaceID, 
                    groupID, 
                    weight, 
                    response.getBody().rna_part_IDs(), 
                    response.getBody().rna_part_types()
                );
            } else {
                designSpaceService.createDesignSpace(spaceID, groupID, weight, response.getBody().dna_part_IDs(), response.getBody().dna_part_types());
            }

            return response.getBody();
        } catch (Exception e) {
            System.out.println("Error calling SeqCompiler compile endpoint: " + e.getMessage());
            throw new RuntimeException("Failed to call SeqCompiler compile endpoint", e);
        }
    }
    
}
