package com.internshield.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Serializes persisted Evidence records into structured JSON formatted for Gemini AI research analysis.
 */
@Component
public class EvidenceJsonExporter {

    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public EvidenceJsonExporter() {
        this.objectMapper = new ObjectMapper();
    }

    public EvidenceJsonExporter(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    /**
     * Exports a list of Evidence entities to a standardized JSON string for AI processing.
     *
     * @param companyName the target company name
     * @param reportId the associated research report ID
     * @param evidenceList list of persisted evidence entities
     * @return structured JSON string representation
     */
    public String exportToJson(String companyName, Long reportId, List<Evidence> evidenceList) {
        return exportToJson(companyName, reportId, evidenceList, null);
    }

    /**
     * Exports Evidence records and extracted listing details to structured JSON for AI processing.
     *
     * @param companyName the target company name
     * @param reportId the associated research report ID
     * @param evidenceList list of persisted evidence entities
     * @param listing extracted details from listing or pasted text
     * @return structured JSON string representation
     */
    public String exportToJson(String companyName, Long reportId, List<Evidence> evidenceList, com.internshield.internship.dto.ExtractedInternshipDetails listing) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("company_name", companyName != null ? companyName : "Unknown");
        if (reportId != null) {
            root.put("report_id", reportId);
        }

        if (listing != null) {
            ObjectNode listingNode = root.putObject("internship_listing");
            listingNode.put("role", listing.role() != null ? listing.role() : "");
            listingNode.put("stipend", listing.stipend() != null ? listing.stipend() : "");
            listingNode.put("fee_demanded", listing.feeDemanded());
            listingNode.put("communication_channel", listing.communicationChannel() != null ? listing.communicationChannel() : "UNKNOWN");
            if (listing.paymentKeywordsFound() != null && !listing.paymentKeywordsFound().isEmpty()) {
                ArrayNode kwArray = listingNode.putArray("payment_keywords_detected");
                listing.paymentKeywordsFound().forEach(kwArray::add);
            }
            if (listing.rawContent() != null && !listing.rawContent().isBlank()) {
                listingNode.put("listing_description", listing.rawContent());
            }
        }

        root.put("evidence_count", evidenceList != null ? evidenceList.size() : 0);

        ArrayNode itemsArray = root.putArray("evidence_items");
        if (evidenceList != null) {
            for (Evidence ev : evidenceList) {
                if (ev == null) continue;
                ObjectNode item = itemsArray.addObject();
                if (ev.getId() != null) {
                    item.put("id", ev.getId());
                }
                item.put("title", ev.getTitle() != null ? ev.getTitle() : "");
                item.put("url", ev.getSourceUrl() != null ? ev.getSourceUrl() : "");
                item.put("domain", ev.getSourceDomain() != null ? ev.getSourceDomain() : "");
                item.put("category", ev.getCategory() != null ? ev.getCategory().name() : "OTHER");
                item.put("credibility", ev.getSourceCredibility() != null ? ev.getSourceCredibility() : "MEDIUM");
                item.put("details", ev.getDescription() != null ? ev.getDescription() : "");
            }
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return root.toString();
        }
    }
}
