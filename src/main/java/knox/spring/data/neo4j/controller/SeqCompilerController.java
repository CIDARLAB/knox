package knox.spring.data.neo4j.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;

import knox.spring.data.neo4j.services.DesignSpaceService;

@RestController
public class SeqCompilerController {
    private final RestTemplate restTemplate;
    final DesignSpaceService designSpaceService;

    private static final Logger LOG = LoggerFactory.getLogger(SeqCompilerController.class);

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

    public record CompileExcelResponse(
        List<List<String>> name,
        List<List<String>> dna_template,
        List<List<String>> rna_template,
        List<List<List<String>>> dna_part_IDs,
        List<List<List<String>>> rna_part_IDs,
        List<List<List<String>>> dna_part_types,
        List<List<List<String>>> rna_part_types,
        List<List<String>> genbank_dna,
        List<List<String>> genbank_rna) {}

    public record CompileExcelRequest(MultipartFile workbook) {}


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
        LOG.info("Received seqcompile request:");

        CompileRequest request = new CompileRequest(
            name, Rz, L, term, hp5, prom, eI, eO, s, invert, invL, agL,
            AGiloop, otype, rna, us, ds, temp_len, cp, n, c, d, CDS, rflap
        );

        CompileResponse response = callSeqCompiler(request);

        if (rna == 1) {
            designSpaceService.createDesignSpace(
                spaceID, 
                groupID, 
                weight, 
                response.rna_part_IDs(), 
                response.rna_part_types()
            );
        } else {
            designSpaceService.createDesignSpace(spaceID, groupID, weight, response.dna_part_IDs(), response.dna_part_types());
        }

        return response;
    }

    @PostMapping("/seqcompiler/compile/excel")
    public ResponseEntity<String> compileSequenceExcel(
            @RequestParam(value = "outputSpacePrefix", required = true) String outputSpacePrefix,
            @RequestParam(value = "groupID", required = true) String groupID,
            @RequestParam(value = "inputCSVFile[]", required = true) MultipartFile file
    ) {
        long startTime = System.nanoTime();
        LOG.info("Received seqcompile EXCEL request:");

        try {
            CompileExcelRequest request = new CompileExcelRequest(file);
            CompileExcelResponse response = callSeqCompilerExcel(request);

            List<Double> weights = readWeightsFromExcel(file);

            for (int i = 0; i < response.rna_part_IDs().size(); i++) {
                String name = String.join("_", response.name().get(i));
                String spaceID = outputSpacePrefix + "_(" + (i + 1) + ")_" + name;

                Double weight = (i < weights.size()) ? weights.get(i) : 0.0;

                designSpaceService.createDesignSpace(
                    spaceID, 
                    groupID, 
                    weight, 
                    response.rna_part_IDs().get(i).stream()
                        .filter(innerList -> innerList != null) // avoid NullPointerException
                        .flatMap(List::stream)
                        .collect(Collectors.toList()), 
                    response.rna_part_types().get(i).stream()
                        .filter(innerList -> innerList != null) // avoid NullPointerException
                        .flatMap(List::stream)
                        .collect(Collectors.toList())
                );
            }

            
        } catch (Exception e) {
            LOG.error("Error processing Excel file: {}", e.getMessage(), e);
            return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
        }
        long endTime = System.nanoTime();
        LOG.info("Seqcompile EXCEL request processing time: {} ms", (endTime - startTime) / 1_000_000);
        return new ResponseEntity<String>("No content", HttpStatus.NO_CONTENT);
    }

    private CompileResponse callSeqCompiler(CompileRequest request) {
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

            return response.getBody();
        } catch (Exception e) {
            LOG.error("Error calling SeqCompiler compile endpoint: " + e.getMessage(), e);
            throw new RuntimeException("Failed to call SeqCompiler compile endpoint", e);
        }
    }

    private CompileExcelResponse callSeqCompilerExcel(CompileExcelRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            MultipartFile workbook = request.workbook();
            ByteArrayResource fileResource = new ByteArrayResource(workbook.getBytes()) {
                @Override
                public String getFilename() {
                    return workbook.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("workbook", fileResource); // key must match the field the SeqCompiler endpoint expects

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<CompileExcelResponse> response = restTemplate.exchange(
                    seqCompilerUrl + "/compile/excel",
                    HttpMethod.POST,
                    entity,
                    CompileExcelResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("SeqCompiler compile/excel endpoint returned status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            LOG.error("Error calling SeqCompiler compile/excel endpoint: " + e.getMessage(), e);
            throw new RuntimeException("Failed to call SeqCompiler compile/excel endpoint", e);
        }
    }

    private List<Double> readWeightsFromExcel(MultipartFile file) {
        try {
            // find sheet with "weights" in its name
            List<String> sheetNames = EasyExcel.read(file.getInputStream())
                    .build()
                    .excelExecutor()
                    .sheetList()
                    .stream()
                    .map(sheet -> sheet.getSheetName())
                    .toList();
            
            String weightsSheetName = sheetNames.stream()
                    .filter(name -> name.toLowerCase().contains("weights"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Weights sheet not found")); 

            List<WeightDTO> weights = EasyExcel.read(file.getInputStream())
                    .head(WeightDTO.class)
                    .sheet(weightsSheetName)
                    .doReadSync();

            return weights.stream()
                    .map(WeightDTO::getWeights)
                    .toList();
        } catch (Exception e) {
            LOG.error("Error reading weights from Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read weights from Excel file", e);
        }
    }

    public static class WeightDTO {
        @ExcelProperty("weights")
        public Double weights;

        public WeightDTO() {}

        public Double getWeights() {
            return weights;
        }

        public void setWeights(Double weights) { this.weights = weights; }
    }

}
