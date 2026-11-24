package com.internshield.report;

import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.report.dto.ResearchRequest;
import com.internshield.report.dto.ResearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ResearchService {

    private final ResearchReportRepository researchReportRepository;

    public ResearchService(ResearchReportRepository researchReportRepository) {
        this.researchReportRepository = researchReportRepository;
    }

    public ResearchResponse createResearchRequest(ResearchRequest request) {
        ResearchReport report = new ResearchReport(
                request.companyName(),
                request.internshipUrl()
        );
        report.setStatus(ResearchStatus.PENDING);

        ResearchReport saved = researchReportRepository.save(report);
        return ResearchResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ResearchResponse getResearchReportById(Long id) {
        ResearchReport report = researchReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research report not found with id: " + id));
        return ResearchResponse.fromEntity(report);
    }

    @Transactional(readOnly = true)
    public List<ResearchResponse> getAllResearchReports() {
        return researchReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ResearchResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResearchResponse> getResearchReportsByCompany(String companyName) {
        return researchReportRepository.findByCompanyName(companyName)
                .stream()
                .map(ResearchResponse::fromEntity)
                .toList();
    }

    public ResearchResponse updateResearchStatus(Long id, ResearchStatus status) {
        ResearchReport report = researchReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research report not found with id: " + id));

        report.setStatus(status);
        ResearchReport updated = researchReportRepository.save(report);
        return ResearchResponse.fromEntity(updated);
    }
}
