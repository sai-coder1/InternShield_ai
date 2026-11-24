package com.internshield.internship;

import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.company.Company;
import com.internshield.company.CompanyRepository;
import com.internshield.internship.dto.InternshipRequest;
import com.internshield.internship.dto.InternshipResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InternshipService {

    private final InternshipRepository internshipRepository;
    private final CompanyRepository companyRepository;

    public InternshipService(InternshipRepository internshipRepository, CompanyRepository companyRepository) {
        this.internshipRepository = internshipRepository;
        this.companyRepository = companyRepository;
    }

    public InternshipResponse createInternship(InternshipRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + request.companyId()));

        Internship internship = new Internship(
                request.title(),
                request.description(),
                request.listingUrl(),
                request.location(),
                request.workMode(),
                request.stipendDetails(),
                request.internshipType(),
                request.applicationDeadline(),
                company
        );

        Internship saved = internshipRepository.save(internship);
        return InternshipResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InternshipResponse> getAllInternships() {
        return internshipRepository.findAll()
                .stream()
                .map(InternshipResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InternshipResponse getInternshipById(Long id) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Internship not found with id: " + id));
        return InternshipResponse.fromEntity(internship);
    }
}
