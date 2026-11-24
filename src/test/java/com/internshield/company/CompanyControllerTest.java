package com.internshield.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.common.GlobalExceptionHandler;
import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.company.dto.CompanyRequest;
import com.internshield.company.dto.CompanyResponse;
import com.internshield.internship.dto.InternshipResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
@Import(GlobalExceptionHandler.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    @Test
    void testCreateCompany_success() throws Exception {
        CompanyRequest request = new CompanyRequest(
                "Acme Corp",
                "https://acme.com",
                "acme.com",
                "Leading supplier of anvils"
        );
        CompanyResponse response = new CompanyResponse(
                1L,
                "Acme Corp",
                "https://acme.com",
                "acme.com",
                "Leading supplier of anvils"
        );

        when(companyService.createCompany(any(CompanyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.website").value("https://acme.com"))
                .andExpect(jsonPath("$.domain").value("acme.com"))
                .andExpect(jsonPath("$.description").value("Leading supplier of anvils"));
    }

    @Test
    void testCreateCompany_validationFailure_blankName() throws Exception {
        CompanyRequest request = new CompanyRequest(
                "",
                "https://acme.com",
                "acme.com",
                "Description"
        );

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.name").value("Company name must not be blank"));
    }

    @Test
    void testGetAllCompanies_success() throws Exception {
        CompanyResponse company1 = new CompanyResponse(1L, "Acme Corp", "https://acme.com", "acme.com", "Desc 1");
        CompanyResponse company2 = new CompanyResponse(2L, "Beta Inc", "https://beta.com", "beta.com", "Desc 2");

        when(companyService.getAllCompanies()).thenReturn(List.of(company1, company2));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Acme Corp"))
                .andExpect(jsonPath("$[1].name").value("Beta Inc"));
    }

    @Test
    void testGetCompanyById_success() throws Exception {
        CompanyResponse company = new CompanyResponse(1L, "Acme Corp", "https://acme.com", "acme.com", "Desc");
        when(companyService.getCompanyById(1L)).thenReturn(company);

        mockMvc.perform(get("/api/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Acme Corp"));
    }

    @Test
    void testGetCompanyById_notFound() throws Exception {
        when(companyService.getCompanyById(999L))
                .thenThrow(new ResourceNotFoundException("Company not found with id: 999"));

        mockMvc.perform(get("/api/companies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Company not found with id: 999"));
    }

    @Test
    void testGetInternshipsByCompanyId_success() throws Exception {
        InternshipResponse internship = new InternshipResponse(
                10L,
                "Backend Engineer Intern",
                "Work on APIs",
                "https://acme.com/careers/10",
                "Remote",
                "REMOTE",
                "25,000 INR / month",
                "FULL_TIME",
                LocalDate.of(2025, 6, 30),
                1L,
                "Acme Corp"
        );

        when(companyService.getInternshipsByCompanyId(1L)).thenReturn(List.of(internship));

        mockMvc.perform(get("/api/companies/1/internships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Backend Engineer Intern"))
                .andExpect(jsonPath("$[0].companyId").value(1));
    }

    @Test
    void testGetInternshipsByCompanyId_companyNotFound() throws Exception {
        when(companyService.getInternshipsByCompanyId(999L))
                .thenThrow(new ResourceNotFoundException("Company not found with id: 999"));

        mockMvc.perform(get("/api/companies/999/internships"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Company not found with id: 999"));
    }
}
