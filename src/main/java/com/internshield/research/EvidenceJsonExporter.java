
package com.internshield.research;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class EvidenceJsonExporter {

    private final ObjectMapper objectMapper;

    public EvidenceJsonExporter() {
        this.objectMapper = new ObjectMapper();

        this.objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT);
    }

    public String export(
            String companyName, //// company being investigated
            List<Evidence> evidenceList) throws Exception {

        var output = objectMapper.createObjectNode(); // This creates an empty JSON object.

        output.put("company", companyName); // adds a key-val pair to our json obj

        output.set(
                "evidence",
                objectMapper.valueToTree(evidenceList));

        return objectMapper.writeValueAsString(output);
    }
}
