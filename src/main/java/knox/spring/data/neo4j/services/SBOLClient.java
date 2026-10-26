package knox.spring.data.neo4j.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.List;


@Service
public class SBOLClient {

    private final RestTemplate restTemplate;

    @Value("${knox.sbol.url}")
    private String knoxSBOLUrl; // KnoxSBOL API URL

    public SBOLClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // GOLDBAR to Combinatorial SBOL Request/Response
    public record GOLDBARToSBOLRequest(
        Map<String, Object> goldbar_tree,
        Map<String, Object> categories,
        String name,
        String types,
        String sbol_version,
        String return_type
    ) {}
    public record GOLDBARToSBOLResponse(String sbol) {}

    public GOLDBARToSBOLResponse convertGOLDBARToSBOL(
            Map<String, Object> goldbar_tree,
            Map<String, Object> categories,
            String name,
            String types,
            String sbol_version) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        GOLDBARToSBOLRequest request = new GOLDBARToSBOLRequest(goldbar_tree, categories, name, types, sbol_version, "str");
        HttpEntity<GOLDBARToSBOLRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<GOLDBARToSBOLResponse> response = restTemplate.exchange(
                knoxSBOLUrl + "/goldbar_to_sbol",
                HttpMethod.POST,
                entity,
                GOLDBARToSBOLResponse.class
        );
        return response.getBody();
    }

    // Combinatorial SBOL to GOLDBAR Request/Response
    public record SBOLToGOLDBARRequest(MultipartFile file, String sbol_version) {}
    public record SBOLToGOLDBARResponse(String goldbar, Map<String, Object> categories, String name) {}

    public SBOLToGOLDBARResponse convertSBOLToGOLDBAR(MultipartFile file, String sbol_version) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            SBOLToGOLDBARRequest request = new SBOLToGOLDBARRequest(file, sbol_version);
            MultipartFile workbook = request.file();
            ByteArrayResource fileResource = new ByteArrayResource(workbook.getBytes()) {
                @Override
                public String getFilename() {
                    return workbook.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource); // key must match the field the knoxSBOL endpoint expects

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<SBOLToGOLDBARResponse> response = restTemplate.exchange(
                    knoxSBOLUrl + "/sbol_to_goldbar?sbol_version=" + sbol_version,
                    HttpMethod.POST,
                    entity,
                    SBOLToGOLDBARResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert SBOL to GOLDBAR", e);
        }
    }
}
