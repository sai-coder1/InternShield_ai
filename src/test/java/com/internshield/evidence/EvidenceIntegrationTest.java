package com.internshield.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.evidence.dto.EvidenceRequest;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.report.ResearchReport;
import com.internshield.report.ResearchReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EvidenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResearchReportRepository researchReportRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    private ResearchReport testReport1;
    private ResearchReport testReport2;

    @BeforeEach
    void setUp() {
        evidenceRepository.deleteAll();
        researchReportRepository.deleteAll();

        testReport1 = researchReportRepository.save(new ResearchReport("Apex Solutions", "https://apex.example.com/careers/intern-1"));
        testReport2 = researchReportRepository.save(new ResearchReport("Nexus Dynamics", "https://nexus.example.com/careers/intern-2"));
    }

    @Test
    void testCreateEvidenceAndVerifyDatabasePersistence() throws Exception {
        EvidenceRequest request = new EvidenceRequest(
                "MCA Company Registration Filing",
                "https://mca.gov.in/company/apex-solutions",
                "Official ministry registry search shows registration date and active status",
                "mca.gov.in",
                EvidenceCategory.OFFICIAL_REGISTRY,
                "HIGH",
                testReport1.getId()
        );

        MvcResult result = mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("MCA Company Registration Filing"))
                .andExpect(jsonPath("$.sourceUrl").value("https://mca.gov.in/company/apex-solutions"))
                .andExpect(jsonPath("$.category").value("OFFICIAL_REGISTRY"))
                .andExpect(jsonPath("$.researchReportId").value(testReport1.getId()))
                .andReturn();

        EvidenceResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), EvidenceResponse.class);
        assertNotNull(response.id());

        // Verify actual database state
        Optional<Evidence> persistedOpt = evidenceRepository.findById(response.id());
        assertTrue(persistedOpt.isPresent());
        Evidence persisted = persistedOpt.get();
        assertEquals("MCA Company Registration Filing", persisted.getTitle());
        assertEquals("https://mca.gov.in/company/apex-solutions", persisted.getSourceUrl());
        assertEquals(EvidenceCategory.OFFICIAL_REGISTRY, persisted.getCategory());
        assertEquals(testReport1.getId(), persisted.getResearchReport().getId());
        assertNotNull(persisted.getCollectedAt());
    }

    @Test
    void testRetrieveEvidenceById() throws Exception {
        Evidence evidence = evidenceRepository.save(new Evidence(
                "Glassdoor Reviews",
                "https://glassdoor.example.com/apex-reviews",
                "Warning of training fees",
                "glassdoor.example.com",
                EvidenceCategory.REVIEW_PLATFORM,
                "MEDIUM",
                testReport1
        ));

        mockMvc.perform(get("/api/evidence/" + evidence.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evidence.getId()))
                .andExpect(jsonPath("$.title").value("Glassdoor Reviews"))
                .andExpect(jsonPath("$.sourceUrl").value("https://glassdoor.example.com/apex-reviews"))
                .andExpect(jsonPath("$.category").value("REVIEW_PLATFORM"))
                .andExpect(jsonPath("$.researchReportId").value(testReport1.getId()));
    }

    @Test
    void testRetrieveEvidenceByReportId_isolatesReports() throws Exception {
        evidenceRepository.save(new Evidence("E1", "https://e.com/1", "D1", "e.com", EvidenceCategory.NEWS_ARTICLE, "HIGH", testReport1));
        evidenceRepository.save(new Evidence("E2", "https://e.com/2", "D2", "e.com", EvidenceCategory.COMMUNITY_REPORT, "MEDIUM", testReport1));
        evidenceRepository.save(new Evidence("E3", "https://e.com/3", "D3", "e.com", EvidenceCategory.JOB_BOARD, "LOW", testReport2));

        mockMvc.perform(get("/api/evidence/report/" + testReport1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].researchReportId").value(testReport1.getId()))
                .andExpect(jsonPath("$[1].researchReportId").value(testReport1.getId()));

        mockMvc.perform(get("/api/evidence/report/" + testReport2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("E3"))
                .andExpect(jsonPath("$[0].researchReportId").value(testReport2.getId()));
    }

    @Test
    void testFilterEvidenceByCategoryAndCount() throws Exception {
        evidenceRepository.save(new Evidence("R1", "https://r.com/1", "D1", "r.com", EvidenceCategory.COMMUNITY_REPORT, "MED", testReport1));
        evidenceRepository.save(new Evidence("R2", "https://r.com/2", "D2", "r.com", EvidenceCategory.COMMUNITY_REPORT, "MED", testReport1));
        evidenceRepository.save(new Evidence("R3", "https://r.com/3", "D3", "r.com", EvidenceCategory.OFFICIAL_SOURCE, "HIGH", testReport1));

        mockMvc.perform(get("/api/evidence/report/" + testReport1.getId() + "/category/COMMUNITY_REPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].category").value("COMMUNITY_REPORT"))
                .andExpect(jsonPath("$[1].category").value("COMMUNITY_REPORT"));

        mockMvc.perform(get("/api/evidence/report/" + testReport1.getId() + "/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void testDeleteEvidenceAndVerifyDatabaseState() throws Exception {
        Evidence evidence = evidenceRepository.save(new Evidence(
                "Temporary Evidence",
                "https://temp.example.com",
                "To be deleted",
                "temp.example.com",
                EvidenceCategory.OTHER,
                "LOW",
                testReport1
        ));
        Long evidenceId = evidence.getId();

        mockMvc.perform(delete("/api/evidence/" + evidenceId))
                .andExpect(status().isNoContent());

        Optional<Evidence> deleted = evidenceRepository.findById(evidenceId);
        assertFalse(deleted.isPresent());
    }

    @Test
    void testMissingResourceResponses() throws Exception {
        mockMvc.perform(get("/api/evidence/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence not found with id: 999999"));

        mockMvc.perform(get("/api/evidence/report/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Research report not found with id: 999999"));

        mockMvc.perform(delete("/api/evidence/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence not found with id: 999999"));

        EvidenceRequest nonExistentReportRequest = new EvidenceRequest(
                "Title", "https://example.com/test", "Desc", "example.com",
                EvidenceCategory.OTHER, "LOW", 999999L
        );
        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentReportRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Research report not found with id: 999999"));
    }

    @Test
    void testDuplicateSourceUrlRejection() throws Exception {
        evidenceRepository.save(new Evidence(
                "Existing Evidence",
                "https://duplicate.example.com/check",
                "First copy",
                "duplicate.example.com",
                EvidenceCategory.COMMUNITY_REPORT,
                "MED",
                testReport1
        ));

        EvidenceRequest duplicateRequest = new EvidenceRequest(
                "Second Evidence with duplicate URL",
                "https://duplicate.example.com/check",
                "Second copy",
                "duplicate.example.com",
                EvidenceCategory.NEWS_ARTICLE,
                "HIGH",
                testReport1.getId()
        );

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Evidence with source URL already exists for research report: https://duplicate.example.com/check"));
    }

    @Test
    void testValidationFailuresHandledByGlobalExceptionHandler() throws Exception {
        EvidenceRequest invalidRequest = new EvidenceRequest(
                "",                     // blank title
                "not-a-valid-url",      // invalid URL
                "Description",
                "domain",
                null,                   // null category
                "LOW",
                null                    // null report ID
        );

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.title").value("Title must not be blank"))
                .andExpect(jsonPath("$.details.sourceUrl").value("Source URL must be a valid URL"))
                .andExpect(jsonPath("$.details.category").value("Evidence category must be provided"))
                .andExpect(jsonPath("$.details.researchReportId").value("Research report ID must be provided"));
    }
}
