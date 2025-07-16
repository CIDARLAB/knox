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

public class GoldbarGeneration {
    
    Map<String, String> goldbar;

    JSONObject categories;

    ArrayList<String> partTypes;

    Map<String, ArrayList<String>> partTypesMap;

    ArrayList<String> rules;

    ArrayList<String> ruleOptions;

    Map<String, ArrayList<String>> columnValues;

    Boolean reverse;

    public GoldbarGeneration(ArrayList<String> rules, InputStream inputCSVStream, Boolean reverse) {
        this.goldbar = new HashMap<>();
        
        this.categories = new JSONObject();

        this.partTypes = new ArrayList<>();

        this.partTypesMap = new HashMap<String, ArrayList<String>>();

        this.rules = rules;

        this.ruleOptions = new ArrayList<>();
        this.ruleOptions.add("R");
        this.ruleOptions.add("B");
        this.ruleOptions.add("T");
        this.ruleOptions.add("I");
        this.ruleOptions.add("M");
        this.ruleOptions.add("O");
        this.ruleOptions.add("N");
        this.ruleOptions.add("L");
        this.ruleOptions.add("P");
        this.ruleOptions.add("E");
        this.ruleOptions.add("goldbar");

        this.columnValues = new HashMap<>();
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputCSVStream));
        processCSV(csvReader);

        this.reverse = reverse;

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
        boolean containsCategories = false;

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
                this.categories.put(partType, categoryValue);
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

    public Map<String, String> createRuleGoldbar() {
        // Do Not Repeat Rule
        // if part is present, then part is present once

        Map<String, String> gGoldbar = new HashMap<>();

        Integer i = 0;
        for (String g : this.columnValues.get("goldbar")) {

            this.goldbar.put("_" + i + "_Rule_misc", g);
            gGoldbar.put(i.toString(), g);

            i = i + 1;
        }

        return gGoldbar;
    }
    
    public Map<String, String> createRuleR() {
        // Do Not Repeat Rule
        // if part is present, then part is present once

        Map<String, String> rGoldbar = new HashMap<>();

        for (String part : this.columnValues.get("R")) {

            String g = new String();
            if (!reverse) {
                g = String.format(
                "(zero-or-more(any_except_%1$s) then %1$s then zero-or-more(any_except_%1$s)) or (zero-or-more(any_except_%1$s))", 
                part);
            
            } else {
                g = String.format(
                "(zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s)) then (%1$s or reverse-comp(%1$s)) then zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s))) " +
                "or (zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s)))", 
                part
                );
            }

            this.goldbar.put("_" + part + "_Rule_R", g);
            rGoldbar.put(part, g);
        }

        return rGoldbar;
    }

    public Map<String, String> createRuleB() {
        // Before Rule
        // if partA and partB are present, partA must be anywhere before partB

        Map<String, String> bGoldbar = new HashMap<>();

        // add to categories
        multiplePartsCategory("B");

        for (String data : this.columnValues.get("B")) {

            String[] dataSplit = data.split(":");

            String name = String.join("and", dataSplit);

            String key = String.join("_", dataSplit);

            String g = new String();
            if (!reverse) {
                g = String.format(
                    "(zero-or-more(any_except_%2$s) then %1$s then zero-or-more(any_except_%1$s)) or (zero-or-more(any_except_%1$s))", 
                    dataSplit[0], dataSplit[1], name
                );
            
            } else {
                g = String.format(
                    "(zero-or-more(any_except_%2$s or reverse-comp(any_except_%2$s)) " +
                    "then (%1$s or reverse-comp(%1$s)) then zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s))) " +
                    "or (zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s)))", 
                    dataSplit[0], dataSplit[1], name
                );
            }

            this.goldbar.put("_" + key + "_Rule_B", g);
            bGoldbar.put(key, g);
        }

        return bGoldbar;
    }

    public Map<String, String> createRuleT() {
        /* Together Rule
        if part1 is present, then part2 is present 
        (Converse is also true)
        */
        
        // add to categories
        multiplePartsCategory("T");

        // goldbar
        Map<String, String> tGoldbar = new HashMap<>();
        for (String data : this.columnValues.get("T")) {
            // data example = "PP1andPP2:P1andP2"

            String[] dataSplit = data.split(":");

            String name = String.join("and", dataSplit);

            String key = String.join("_", dataSplit);

            String g1 = new String();
            String g2 = new String();
            String g3 = new String();
            if (!reverse) {
                // ... part1 ... part2 then except_part1
                g1 = String.format(
                    "(zero-or-more(any_part_concrete) then %1$s then " +
                    "zero-or-more(any_except_%3$s) then %2$s " +
                    "then zero-or-more(any_except_%1$s))", 
                    dataSplit[0], dataSplit[1], name
                );

                // ... part2 ... part1 then except_part2
                g2 = String.format(
                    "(zero-or-more(any_part_concrete) then %2$s then " +
                    "zero-or-more(any_except_%3$s) then %1$s " +
                    "then zero-or-more(any_except_%2$s))", 
                    dataSplit[0], dataSplit[1], name
                );

                // any_except_part1andpart2
                g3 = String.format(
                    "(zero-or-more(any_except_%1$s)))", 
                    name
                );

            } else {
                // ... part1 ... part2 then except_part1
                g1 = String.format(
                    "(zero-or-more(any_part_concrete or reverse-comp(any_part_concrete)) then (%1$s or reverse-comp(%1$s)) then " +
                    "zero-or-more(any_except_%3$s or reverse-comp(any_except_%3$s)) then (%2$s or reverse-comp(%2$s)) " +
                    "then zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s)))", 
                    dataSplit[0], dataSplit[1], name
                );

                // ... part2 ... part1 then except_part2
                g2 = String.format(
                    "(zero-or-more(any_part_concrete or reverse-comp(any_part_concrete)) then (%2$s or reverse-comp(%2$s)) then " +
                    "zero-or-more(any_except_%3$s or reverse-comp(any_except_%3$s)) then (%1$s or reverse-comp(%1$s)) " +
                    "then zero-or-more(any_except_%2$s or reverse-comp(any_except_%2$s)))", 
                    dataSplit[0], dataSplit[1], name
                );

                // any_except_part1andpart2
                g3 = String.format(
                    "(zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s))))", 
                    name
                );
            }

            tGoldbar.put(key, g1 + " or " + g2 + " or " + g3);

            goldbar.put("_" + key + "_Rule_T", g1 + " or " + g2 + " or " + g3);

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

            String g = new String();
            if (!reverse) {
                // ... part1 ... part2 then except_part1
                g = String.format(
                    "(zero-or-more((any_except_%1$s) or (%1$s then any_except_%3$s))) then zero-or-more(%1$s)", 
                    dataSplit[0], dataSplit[1], name
                );

            } else {
                // ... part1 ... part2 then except_part1
                g = String.format(
                    "zero-or-more((one-or-more(%1$s) then any_except_%3$s) or any_except_%1$s or reverse-comp((one-or-more(%1$s) then any_except_%3$s)) or reverse-comp(any_except_%1$s)) then zero-or-one(%1$s or reverse-comp(%1$s))", 
                    dataSplit[0], dataSplit[1], name
                );
            }

            pjiGoldbar.put(key, g);
            this.goldbar.put("_" + key + "_Rule_I", g);
        }
    
        return pjiGoldbar;
    }

    public Map<String, String> createRuleM() {
        Map<String, String> mGoldbar = new HashMap<>();

        for (String part : this.columnValues.get("M")) {
            
            String g = new String();
            if (!reverse) {
                g = String.format(
                    "zero-or-more(zero-or-more(any_except_%1$s) then one-or-more(%1$s then zero-or-more(any_except_%1$s)))", 
                    part
                );

            } else {
                g = String.format(
                    "zero-or-more(zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s)) then one-or-more((%1$s or reverse-comp(%1$s)) then zero-or-more(any_except_%1$s or reverse-comp(any_except_%1$s))))", 
                    part
                );
            }

            mGoldbar.put(part, g);
            this.goldbar.put("_" + part + "_Rule_M", g);
        }

        return mGoldbar;
    }

    public Map<String, String> createRuleE() {
        Map<String, String> eGoldbar = new HashMap<>();

        for (String part : this.columnValues.get("E")) {
            
            String g = new String();
            if (!reverse) {
                g = String.format(
                    "zero-or-more(any_part_concrete) then %1$s", 
                    part
                );

            } else {
                g = String.format(
                    "zero-or-more(any_part_concrete or reverse-comp(any_part_concrete)) then (part or reverse-comp(%1$s))", 
                    part
                );
            }

            eGoldbar.put(part, g);
            this.goldbar.put("_" + part + "_Rule_E", g);
        }

        return eGoldbar;
    }

    public Map<String, String> createRuleO() {
        /* Not Orthogonal Rule
        if part1 is present, then part2 is not
        (Converse is true)
        */
        
        // add to categories
        multiplePartsCategory("O");

        // goldbar
        Map<String, String> oGoldbar = new HashMap<>();
        for (String data : this.columnValues.get("O")) {
            // data example = "PP1andPP2:P1andP2"

            String[] dataSplit = data.split(":");

            String name = String.join("and", dataSplit);

            String key = String.join("_", dataSplit);

            String g1 = new String();
            String g2 = new String();
            String g3 = new String();
            if (!reverse) {
                g1 = String.format(
                    "zero-or-more(any_except_%3$s)", 
                    dataSplit[0], dataSplit[1], name
                );

                g2 = String.format(
                    "zero-or-more(any_except_%3$s or %1$s) then %1$s then zero-or-more(any_except_%3$s)", 
                    dataSplit[0], dataSplit[1], name
                );

                g3 = String.format(
                    "zero-or-more(any_except_%3$s or %2$s) then %2$s then zero-or-more(any_except_%3$s)", 
                    dataSplit[0], dataSplit[1], name
                );

            } else {
                g1 = String.format(
                    "zero-or-more(any_except_%3$s or reverse-comp(any_except_%3$s))", 
                    dataSplit[0], dataSplit[1], name
                );

                g2 = String.format(
                    "zero-or-more(any_except_%3$s or %1$s or reverse-comp(any_except_%3$s) or reverse-comp(%1$s)) " +
                    "then (%1$s or reverse-comp(%1$s)) then zero-or-more(any_except_%3$s or reverse-comp(any_except_%3$s))", 
                    dataSplit[0], dataSplit[1], name
                );

                g3 = String.format(
                    "zero-or-more(any_except_%3$s or %2$s or reverse-comp(any_except_%3$s) or reverse-comp(%2$s)) " +
                    "then (%2$s or reverse-comp(%2$s)) then zero-or-more(any_except_%3$s or reverse-comp(any_except_%3$s))", 
                    dataSplit[0], dataSplit[1], name
                );
            }
            
            oGoldbar.put(key, g1 + " or " + g2 + " or " + g3);
            this.goldbar.put("_" + key + "_Rule_O", g1 + " or " + g2 + " or " + g3);
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
                if (!reverse) {
                    g = g + "any_part_concrete then ";
                } else {
                    g = g + "(any_part_concrete or reverse-comp(any_part_concrete)) then ";
                }
            }

            if (!reverse) {
                g = g + "any_part_concrete";
            } else {
                g = g + "(any_part_concrete or reverse-comp(any_part_concrete))";
            }

            nGoldbar.put(length, g);

            this.goldbar.put("_" + length + "_Rule_N", g);
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
        String g = new String();
        if (!reverse) {
            g = "(zero-or-more(any_except_terminator_leaky) then zero-or-one(terminator))";
        } else {
            g = "(zero-or-more(any_except_terminator_leaky or reverse-comp(any_except_terminator_leaky)) " +
                "then zero-or-one(terminator or reverse-comp(terminator)))";
        }

        this.goldbar.put("_LeakyTerminators_Rule_L", g);

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
        String g = new String();
        if (!reverse) {
            g = "zero-or-more((any_except_roadBlockingPromoter then any_except_roadBlockingPromoter) " +
                "or (promoter then any_except_roadBlockingPromoter))";
        } else {
            g = "zero-or-more((any_except_roadBlockingPromoter then any_except_roadBlockingPromoter) " +
                "or reverse-comp((any_except_roadBlockingPromoter then any_except_roadBlockingPromoter)) " +
                "or (promoter then any_except_roadBlockingPromoter)) " +
                "or reverse-comp(promoter then any_except_roadBlockingPromoter))";
        }

        this.goldbar.put("_PromoterRoadBlocking_Rule_P", g);

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

    public Map<String, String> getGoldbar() {
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
