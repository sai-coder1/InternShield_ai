package com.internshield.evidence;

import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.evidence.dto.EvidenceRequest;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.report.ResearchReport;
import com.internshield.report.ResearchReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final ResearchReportRepository researchReportRepository;

    public EvidenceService(EvidenceRepository evidenceRepository, ResearchReportRepository researchReportRepository) {
        this.evidenceRepository = evidenceRepository;
        this.researchReportRepository = researchReportRepository;
    }

    public EvidenceResponse createEvidence(EvidenceRequest request) {
        ResearchReport report = researchReportRepository.findById(request.researchReportId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Research report not found with id: " + request.researchReportId()));

        if (evidenceRepository.existsBySourceUrlAndResearchReportId(request.sourceUrl(), request.researchReportId())) {
            throw new IllegalArgumentException(
                    "Evidence with source URL already exists for research report: " + request.sourceUrl());
        }

        Evidence evidence = new Evidence(
                request.title(),
                request.sourceUrl(),
                request.description(),
                request.sourceDomain(),
                request.category(),
                request.sourceCredibility(),
                report
        );

        Evidence saved = evidenceRepository.save(evidence);
        return EvidenceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public EvidenceResponse getEvidenceById(Long id) {
        Evidence evidence = evidenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with id: " + id));
        return EvidenceResponse.fromEntity(evidence);
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceByReportId(Long reportId) {
        if (!researchReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Research report not found with id: " + reportId);
        }

        return evidenceRepository.findByResearchReportId(reportId)
                .stream()
                .map(EvidenceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceByCategory(Long reportId, EvidenceCategory category) {
        if (!researchReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Research report not found with id: " + reportId);
        }

        return evidenceRepository.findByCategoryAndResearchReportId(category, reportId)
                .stream()
                .map(EvidenceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getEvidenceCount(Long reportId) {
        if (!researchReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Research report not found with id: " + reportId);
        }

        return evidenceRepository.countByResearchReportId(reportId);
    }

    public void deleteEvidence(Long id) {
        Evidence evidence = evidenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with id: " + id));

        evidenceRepository.delete(evidence);
    }
}
