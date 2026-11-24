package com.internshield.company;

import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.company.dto.CompanyRequest;
import com.internshield.company.dto.CompanyResponse;
import com.internshield.internship.InternshipRepository;
import com.internshield.internship.dto.InternshipResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final InternshipRepository internshipRepository;

    public CompanyService(CompanyRepository companyRepository, InternshipRepository internshipRepository) {
        this.companyRepository = companyRepository;
        this.internshipRepository = internshipRepository;
    }

    public CompanyResponse createCompany(CompanyRequest request) {
        Company company = new Company(
                request.name(),
                request.website(),
                request.domain(),
                request.description()
        );
        Company savedCompany = companyRepository.save(company);
        return CompanyResponse.fromEntity(savedCompany);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(CompanyResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
        return CompanyResponse.fromEntity(company);
    }

    @Transactional(readOnly = true)
    public List<InternshipResponse> getInternshipsByCompanyId(Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found with id: " + companyId);
        }
        return internshipRepository.findByCompanyId(companyId)
                .stream()
                .map(InternshipResponse::fromEntity)
                .toList();
    }
}
