package com.internshield.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.common.GlobalExceptionHandler;
import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.internship.dto.InternshipRequest;
import com.internshield.internship.dto.InternshipResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternshipController.class)
@Import(GlobalExceptionHandler.class)
class InternshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InternshipService internshipService;

    @Test
    void testCreateInternship_success() throws Exception {
        InternshipRequest request = new InternshipRequest(
                "Software Engineer Intern",
                "Backend role",
                "https://example.com/job/1",
                "Bangalore",
                "HYBRID",
                "30,000 INR / month",
                "FULL_TIME",
                LocalDate.of(2025, 7, 31),
                1L
        );

        InternshipResponse response = new InternshipResponse(
                10L,
                "Software Engineer Intern",
                "Backend role",
                "https://example.com/job/1",
                "Bangalore",
                "HYBRID",
                "30,000 INR / month",
                "FULL_TIME",
                LocalDate.of(2025, 7, 31),
                1L,
                "Acme Corp"
        );

        when(internshipService.createInternship(any(InternshipRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/internships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Software Engineer Intern"))
                .andExpect(jsonPath("$.location").value("Bangalore"))
                .andExpect(jsonPath("$.workMode").value("HYBRID"))
                .andExpect(jsonPath("$.stipendDetails").value("30,000 INR / month"))
                .andExpect(jsonPath("$.companyId").value(1))
                .andExpect(jsonPath("$.companyName").value("Acme Corp"));
    }

    @Test
    void testCreateInternship_validationFailure_blankTitle() throws Exception {
        InternshipRequest request = new InternshipRequest(
                "",
                "Backend role",
                "https://example.com/job/1",
                "Bangalore",
                "HYBRID",
                "30,000 INR / month",
                "FULL_TIME",
                LocalDate.of(2025, 7, 31),
                1L
        );

        mockMvc.perform(post("/api/internships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.title").value("Internship title must not be blank"));
    }

    @Test
    void testCreateInternship_validationFailure_nullCompanyId() throws Exception {
        InternshipRequest request = new InternshipRequest(
                "Software Engineer Intern",
                "Backend role",
                "https://example.com/job/1",
                "Bangalore",
                "HYBRID",
                "30,000 INR / month",
                "FULL_TIME",
                LocalDate.of(2025, 7, 31),
                null
        );

        mockMvc.perform(post("/api/internships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.companyId").value("Company ID must be provided"));
    }

    @Test
    void testCreateInternship_validationFailure_invalidUrl() throws Exception {
        InternshipRequest request = new InternshipRequest(
                "Software Engineer Intern",
                "Backend role",
                "invalid-url-string",
                "Bangalore",
                "HYBRID",
                "30,000 INR / month",
                "FULL_TIME",
                LocalDate.of(2025, 7, 31),
                1L
        );

        mockMvc.perform(post("/api/internships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.listingUrl").value("Listing URL must be a valid URL"));
    }

    @Test
    void testCreateInternship_companyNotFound() throws Exception {
        InternshipRequest request = new InternshipRequest(
                "Software Engineer Intern",
                "Backend role",
                "https://example.com/job/1",
                "Bangalore",
                "HYBRID",
                "30,000 INR / month",
                "FULL_TIME",
                LocalDate.of(2025, 7, 31),
                999L
        );

        when(internshipService.createInternship(any(InternshipRequest.class)))
                .thenThrow(new ResourceNotFoundException("Company not found with id: 999"));

        mockMvc.perform(post("/api/internships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Company not found with id: 999"));
    }

    @Test
    void testGetAllInternships_success() throws Exception {
        InternshipResponse i1 = new InternshipResponse(1L, "SWE Intern", "Desc 1", "https://example.com/1", "Remote", "REMOTE", "30k", "FULL_TIME", null, 1L, "Acme");
        InternshipResponse i2 = new InternshipResponse(2L, "PM Intern", "Desc 2", "https://example.com/2", "Bangalore", "ON_SITE", "25k", "PART_TIME", null, 1L, "Acme");

        when(internshipService.getAllInternships()).thenReturn(List.of(i1, i2));

        mockMvc.perform(get("/api/internships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("SWE Intern"))
                .andExpect(jsonPath("$[1].title").value("PM Intern"));
    }

    @Test
    void testGetInternshipById_success() throws Exception {
        InternshipResponse response = new InternshipResponse(1L, "SWE Intern", "Desc 1", "https://example.com/1", "Remote", "REMOTE", "30k", "FULL_TIME", null, 1L, "Acme");
        when(internshipService.getInternshipById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/internships/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("SWE Intern"));
    }

    @Test
    void testGetInternshipById_notFound() throws Exception {
        when(internshipService.getInternshipById(999L))
                .thenThrow(new ResourceNotFoundException("Internship not found with id: 999"));

        mockMvc.perform(get("/api/internships/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Internship not found with id: 999"));
    }
}
