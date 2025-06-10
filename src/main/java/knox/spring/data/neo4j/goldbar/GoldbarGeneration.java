package knox.spring.data.neo4j.goldbar;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.json.JSONObject;
import org.neo4j.ogm.json.JSONArray;
import org.neo4j.ogm.json.JSONException;

//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;

public class GoldbarGeneration {
    
    JSONArray goldbar;

    JSONObject categories;

    Boolean promoters;

    Boolean terminators;

    Boolean ribosomeBindingSite;

    Boolean cds;

    ArrayList<String> partTypes;

    Map<String, ArrayList<String>> partTypesMap;

    ArrayList<String> rules;

    ArrayList<String> ruleOptions;

    Map<String, ArrayList<String>> columnValues;

    public GoldbarGeneration(ArrayList<String> rules, InputStream inputCSVStream) {
        this.goldbar = new JSONArray();
        
        this.categories = new JSONObject();

        this.partTypes = new ArrayList<>();

        this.partTypesMap = new HashMap<String, ArrayList<String>>();

        this.rules = rules;

        this.ruleOptions = new ArrayList<>();
        this.ruleOptions.add("R");
        this.ruleOptions.add("T");
        this.ruleOptions.add("I");
        this.ruleOptions.add("M");
        this.ruleOptions.add("O");
        this.ruleOptions.add("N");
        this.ruleOptions.add("L");
        this.ruleOptions.add("P");

        this.columnValues = new HashMap<>();
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputCSVStream));
        processCSV(csvReader);

        System.out.println("\nData:");
        System.out.println(columnValues + "\n\n");

        System.out.println("\npartTypeMaps:");
        System.out.println(partTypesMap + "\n\n");

        try {
            createBaseCategories();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processCSV(BufferedReader csvReader) {
        Map<String, Integer> columnIndices = new HashMap<>();

        try {
            String line;
            String[] headers = csvReader.readLine().split(",");

            // Get Column Indices
            for (int i = 0; i < headers.length; i++) {

                boolean isRule = false;
                for (String column : this.rules) {
                    if (headers[i].equals(column)) {
                        columnIndices.put(column, i);
                        isRule = true;
                    }
                }

                if (ruleOptions.contains(headers[i])) {
                    isRule = true;
                }

                if (!isRule) {
                    columnIndices.put(headers[i], i);
                    partTypes.add(headers[i]);
                }
            }

            // Get Column Values
            int i = 0;
            while ((line = csvReader.readLine()) != null) {
                String[] values = line.split(",");
                
                for (String column : partTypes) {
                    
                    Integer columnIndex = columnIndices.get(column);
                    if (columnIndex != null && values.length > columnIndex && values[columnIndex].length()>=1) {
                        
                        ArrayList<String> value = new ArrayList<>();
                        
                        if (i > 0) {
                            value = this.columnValues.get(column);
                        }

                        value.add(values[columnIndex]);

                        this.columnValues.put(column, value);
                        this.partTypesMap.put(column, value);
                    }
                }

                for (String column : this.rules) {
                    
                    Integer columnIndex = columnIndices.get(column);
                    if (columnIndex != null && values.length > columnIndex && values[columnIndex].length()>=1) {
                        
                        ArrayList<String> value = new ArrayList<>();
                        
                        if (i > 0) {
                            value = this.columnValues.get(column);
                        }

                        value.add(values[columnIndex]);
                        
                        this.columnValues.put(column, value);
                    }
                }

                i++;
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createBaseCategories() throws JSONException {

        // Add any part concrete
        JSONObject categoryValue = new JSONObject();
        for (String partType : this.partTypesMap.keySet()) {
            JSONArray values = new JSONArray(this.partTypesMap.get(partType));
            try {
                categoryValue.put(partType, values);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            this.categories.put("any_part_concrete", categoryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Add any part abstract
        categoryValue = new JSONObject();
        for (String partType : this.partTypesMap.keySet()) {
            JSONArray values = new JSONArray();
            try {
                categoryValue.put(partType, values);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            this.categories.put("any_part_abstract", categoryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // Add Part Types
        for (String partType : this.partTypesMap.keySet()) {
            categoryValue = new JSONObject();
            JSONArray values = new JSONArray(this.partTypesMap.get(partType));
            try {
                categoryValue.put(partType, values);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            
            try {
                this.categories.put(partType + "_any", categoryValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        
        // Add Individual Parts
        for (String partType : this.partTypesMap.keySet()) {
            for (String part : this.partTypesMap.get(partType)) {
                
                categoryValue = new JSONObject();

                JSONArray values = new JSONArray();
                values.put(part);
                
                try {
                    categoryValue.put(partType, values);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
                try {
                    this.categories.put(part, categoryValue);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        
        // Add Any Except Part
        for (String partType : this.partTypesMap.keySet()) {
            for (String part : this.partTypesMap.get(partType)) {
                
                categoryValue = new JSONObject();

                for (String partType2 : this.partTypesMap.keySet()) {
                    
                    JSONArray values = new JSONArray();
                    
                    if (partType == partType2) {
                        for (String part2 : this.partTypesMap.get(partType)) {
                            if (part != part2) {
                                values.put(part2);
                            }
                        }
                        
                    } else {
                        values = new JSONArray(this.partTypesMap.get(partType2));
                    }
                
                    try {
                        categoryValue.put(partType2, values);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                
                try {
                    this.categories.put("any_except_" + part, categoryValue);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    public Map<String, ArrayList<String>> createRuleR() {
        // Do Not Repeat Rule
        // if part is present, then part is present once

        Map<String, ArrayList<String>> rGoldbar = new HashMap<>();

        for (String part : this.columnValues.get("R")) {

            ArrayList<String> rgol = new ArrayList<>();

            String g1 = String.format(
                "(zero-or-more(any_except_%1$s) then %1$s then zero-or-more(any_except_%1$s))", 
                part
            );
            rgol.add(g1);

            String g2 = String.format(
                "(zero-or-more(any_except_%1$s))", 
                part
            );
            rgol.add(g2);

            this.goldbar.put(g1 + " or " + g2);
            rGoldbar.put(part, rgol);
        }

        return rGoldbar;
    }

    public Map<String, ArrayList<String>> createRuleT() {
        /* Together Rule
        if part1 is present, then part2 is present 
        (Converse is also true)
        */
        
        // add to categories
        multiplePartsCategory("T");

        // goldbar
        Map<String, ArrayList<String>> tGoldbar = new HashMap<>();
        for (String data : this.columnValues.get("T")) {
            // data example = "PP1andPP2:P1andP2"

            ArrayList<String> tbar = new ArrayList<>();

            String[] dataSplit = data.split(":");

            String name = String.join("and", dataSplit);

            String key = String.join("_", dataSplit);

            // ... part1 ... part2 then except_part1
            String g1 = String.format(
                "(zero-or-more(any_part_abstract) then %1$s then " +
                "zero-or-more(any_except_%3$s) then %2$s " +
                "then zero-or-more(any_except_%1$s))", 
                dataSplit[0], dataSplit[1], name
            );
            tbar.add(g1);

            // ... part2 ... part1 then except_part2
            String g2 = String.format(
                "(zero-or-more(any_part_abstract) then %2$s then " +
                "zero-or-more(any_except_%3$s) then %1$s " +
                "then zero-or-more(any_except_%2$s))", 
                dataSplit[0], dataSplit[1], name
            );
            tbar.add(g2);

            // any_except_part1andpart2
            String g3 = String.format(
                "(zero-or-more(any_except_%1$s)))", 
                name
            );
            tbar.add(g3);

            tGoldbar.put(key, tbar);

            goldbar.put(g1 + " or " + g2 + " or " + g3);

        }
    
        return tGoldbar;
    }

    public Map<String, String> createRuleI() {
        /* Part Junction Interference Rule
        if part1 is present, then part2 is not directly downstream of part1 
        (Converse is not true)
        */
        
        // add to categories
        multiplePartsCategory("I");

        // goldbar
        Map<String, String> pjiGoldbar = new HashMap<>();
        for (String data : this.columnValues.get("I")) {
            // data example = "PP1andPP2:P1andP2"

            String[] dataSplit = data.split(":");

            String name = String.join("and", dataSplit);

            String key = String.join("_", dataSplit);

            // ... part1 ... part2 then except_part1
            String g = String.format(
                "zero-or-more((any_except_%1$s then any_except_%1$s) " +
                "or (%1$s then any_except_%2$s))", 
                dataSplit[0], name
            );
            pjiGoldbar.put(key, g);
            this.goldbar.put(g);

        }
    
        return pjiGoldbar;
    }

    public Map<String, String> createRuleM() {
        Map<String, String> mGoldbar = new HashMap<>();

        for (String part : this.columnValues.get("M")) {
            String g = String.format(
                "zero-or-more(any_except_%1$s or %1$s) then %1$s then zero-or-more(any_except_%1$s)", 
                part
            );

            mGoldbar.put(part, g);
            this.goldbar.put(g);
        }

        return mGoldbar;
    }

    public Map<String, ArrayList<String>> createRuleO() {
        /* Not Orthogonal Rule
        if part1 is present, then part2 is not
        (Converse is true)
        */
        
        // add to categories
        multiplePartsCategory("O");

        // goldbar
        Map<String, ArrayList<String>> oGoldbar = new HashMap<>();
        for (String data : this.columnValues.get("O")) {
            // data example = "PP1andPP2:P1andP2"

            ArrayList<String> ogol = new ArrayList<>();

            String[] dataSplit = data.split(":");

            String name = String.join("and", dataSplit);

            String key = String.join("_", dataSplit);

            String g1 = String.format(
                "zero-or-more(any_except_%3$s)", 
                dataSplit[0], dataSplit[1], name
            );
            ogol.add(g1);

            String g2 = String.format(
                "zero-or-more(any_except_%3$s or %1$s) then %1$s then zero-or-more(any_except_%3$s)", 
                dataSplit[0], dataSplit[1], name
            );
            ogol.add(g2);

            String g3 = String.format(
                "zero-or-more(any_except_%3$s or %2$s) then %2$s then zero-or-more(any_except_%3$s)", 
                dataSplit[0], dataSplit[1], name
            );
            ogol.add(g3);


            oGoldbar.put(key, ogol);
            this.goldbar.put(g1 + " or " + g2 + " or " + g3);

        }
    
        return oGoldbar;
    }

    public Map<String, String> createRuleN(ArrayList<String> lengths) {
        // Length of Design Rule

        Map<String, String> nGoldbar = new HashMap<>();

        for (String length : lengths) {
            Integer len = Integer.parseInt(length);
            String g = "";
            for (int i = 0; i < (len-1); i++) {
                g = g + "any_part_concrete then ";
            }
            g = g + "any_part_concrete";

            nGoldbar.put(length, g);

            this.goldbar.put(g);
        }

        return nGoldbar;
    }

    public String createRuleL() {
        // Leaky Terminators Rule
        // if leaky terminator present, must be at end of circuit

        // add to categories
        // Add Any Except Leaky Terminators
        JSONObject categroryValue = new JSONObject();

        for (String partType : this.partTypes) {
            if (partType == "terminator") {
                ArrayList<String> nonLeaky = this.partTypesMap.get(partType);
                nonLeaky.removeAll(columnValues.get("L"));
                try {
                    categroryValue.put(partType, nonLeaky);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (this.partTypesMap.containsKey(partType)) {
                ArrayList<String> values = this.partTypesMap.get(partType);
                try {
                    categroryValue.put(partType, values);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        
        try {
            categories.put("any_except_terminator_leaky", categroryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // goldbar
        String g = "(zero-or-more(any_except_terminator_leaky) then zero-or-one(terminator_any))";

        this.goldbar.put(g);

        return g;
    }

    public String createRuleP() {
        // Promoter Road Blocking Rule
        // promoter can not be followed by road blocking promoter

        // add to categories
        // Add promoter_notroadblocking
        JSONObject categroryValue = new JSONObject();

        ArrayList<String> nonRoadBlocking = this.partTypesMap.get("promoter");
        nonRoadBlocking.removeAll(columnValues.get("P"));
        
        try {
            categroryValue.put("promoter", nonRoadBlocking);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        try {
            categories.put("promoter_notroadblocking", categroryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Add Any except roadBlockingPromoter
        categroryValue = new JSONObject();

        for (String partType : this.partTypes) {
            if (partType == "promoter") {
                try {
                    categroryValue.put(partType, nonRoadBlocking);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (this.partTypesMap.containsKey(partType)) {
                ArrayList<String> values = this.partTypesMap.get(partType);
                try {
                    categroryValue.put(partType, values);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        
        try {
            categories.put("any_except_roadBlockingPromoter", categroryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Add Any except Promoter
        categroryValue = new JSONObject();

        for (String partType : this.partTypes) {
            if (partType != "promoter") {
                ArrayList<String> values = this.partTypesMap.get(partType);
                try {
                    categroryValue.put(partType, values);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        
        try {
            categories.put("any_except_promoter", categroryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // goldbar
        String g = "zero-or-more((any_except_roadBlockingPromoter then any_except_roadBlockingPromoter) " +
                   "or (promoter_any then any_except_roadBlockingPromoter))";

        this.goldbar.put(g);

        return g;
    }

    private void multiplePartsCategory(String rule) {
        // add to categories
        for (String data : this.columnValues.get(rule)) {
            String[] dataSplit = data.split(":");

            String[] partList1 = {dataSplit[0]};
            if (dataSplit[0].contains("and")) {
                partList1 = dataSplit[0].split("and");
                createMultiplePartsCategory(dataSplit[0], partList1);
                createExceptMultiplePartsCategory(dataSplit[0], partList1);
            }

            String[] partList2 = {dataSplit[1]};
            if (dataSplit[1].contains("and")) {
                partList2 = dataSplit[1].split("and");
                createMultiplePartsCategory(dataSplit[1], partList2);
                createExceptMultiplePartsCategory(dataSplit[1], partList2);
            }

            String name = String.join("and", dataSplit);
            String[] fullPartList = data.split(":|and");
            createExceptMultiplePartsCategory(name, fullPartList);
        }
    }

    private void createMultiplePartsCategory(String name, String[] partList) {
        Map<String, ArrayList<String>> partTypeMap2 = new HashMap<String, ArrayList<String>>();
        
        for (String partType : this.partTypesMap.keySet()) {

            // add part1andpart2...andpartn

            for (String part : partList) {
                
                if (partTypesMap.get(partType).contains(part)) {
                    
                    if (partTypeMap2.containsKey(partType)) {
                        ArrayList<String> values = partTypeMap2.get(partType);
                        values.add(part);
                        partTypeMap2.put(partType, values);

                    } else {
                        ArrayList<String> values = new ArrayList<>();
                        values.add(part);
                        partTypeMap2.put(partType, values);
                    }
                }
            }
        }

        JSONObject categoryValue = new JSONObject(partTypeMap2);

        try {
            categories.put(name, categoryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void createExceptMultiplePartsCategory(String name, String[] partList) {
        Map<String, ArrayList<String>> partTypeMap2 = new HashMap<String, ArrayList<String>>();
        
        // Deep Copy partTypesMap to partTypeMap2
        for (Map.Entry<String, ArrayList<String>> entry : this.partTypesMap.entrySet()) {
            partTypeMap2.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        
        for (String partType : this.partTypesMap.keySet()) {

            // add part1andpart2...andpartn

            for (String part : partList) {
                
                if (this.partTypesMap.get(partType).contains(part)) {
                        
                    ArrayList<String> values = partTypeMap2.get(partType);
                    values.remove(part);
                    partTypeMap2.put(partType, values);

                } 
            }
        }

        JSONObject categoryValue = new JSONObject(partTypeMap2);

        try {
            categories.put("any_except_" + name, categoryValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getGoldbar() {
        return this.goldbar;
    }

    public String getGoldbarString() {
        return this.goldbar.toString();
    }

    public JSONObject getCategories() {
        return this.categories;
    }

    public String getCategoriesString() {
        return this.categories.toString();
    }
}
