package knox.spring.data.neo4j.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

import knox.spring.data.neo4j.services.DesignSpaceService;

@RestController
public class TableController {
    final DesignSpaceService designSpaceService;

    public TableController(DesignSpaceService designSpaceService) {
        this.designSpaceService = designSpaceService;
    }

    @GetMapping("/table/purity")
    public List<Map<String, Object>> getPurityTable(@RequestParam(value = "evaluationName", required = true) String evaluationName) {
        Map<String, Map<String, Object>> results = designSpaceService.getEvaluationResults(evaluationName);
        Map<String, Object> evaluationResults = results.get("evaluationResults");

        List<Map<String, Object>> purityTable = new ArrayList<>();
        for (String ruleSpaceID : evaluationResults.keySet()) {
            Map<String, Object> metrics = (Map<String, Object>) evaluationResults.get(ruleSpaceID);
            purityTable.add(metrics);
        }

        //System.out.println("Purity Table: " + purityTable);

        return purityTable;
    }

}
