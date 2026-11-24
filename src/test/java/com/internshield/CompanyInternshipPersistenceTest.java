package com.internshield;

import com.internshield.company.Company;
import com.internshield.company.CompanyRepository;
import com.internshield.internship.Internship;
import com.internshield.internship.InternshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CompanyInternshipPersistenceTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private InternshipRepository internshipRepository;

    @Test
    void testCreateCompanyAndInternshipsRelationship() {
        Company company = new Company("Acme Tech", "https://acme.example.com", "acme.example.com", "Tech Solutions");
        Company savedCompany = companyRepository.save(company);
        assertNotNull(savedCompany.getId());

        Internship internship1 = new Internship(
                "Software Engineer Intern",
                "Work on backend systems",
                "https://acme.example.com/careers/intern-1",
                "Bangalore",
                "HYBRID",
                "30,000 INR / month",
                "FULL_TIME",
                LocalDate.now().plusMonths(2),
                savedCompany
        );

        Internship internship2 = new Internship(
                "Data Science Intern",
                "Work on ML models",
                "https://acme.example.com/careers/intern-2",
                "Remote",
                "REMOTE",
                "25,000 INR / month",
                "PART_TIME",
                LocalDate.now().plusMonths(1),
                savedCompany
        );

        internshipRepository.save(internship1);
        internshipRepository.save(internship2);

        List<Internship> companyInternships = internshipRepository.findByCompanyId(savedCompany.getId());
        assertEquals(2, companyInternships.size());

        Optional<Company> fetchedCompany = companyRepository.findByName("Acme Tech");
        assertTrue(fetchedCompany.isPresent());
        assertEquals("acme.example.com", fetchedCompany.get().getDomain());
    }
}
