package com.internshield.report;

import com.internshield.report.dto.ResearchRequest;
import com.internshield.report.dto.ResearchResponse;
import com.internshield.report.dto.ResearchResultUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private final ResearchService researchService;

    public ResearchController(ResearchService researchService) {
        this.researchService = researchService;
    }

    @PostMapping
    public ResponseEntity<ResearchResponse> createResearchRequest(@Valid @RequestBody ResearchRequest request) {
        ResearchResponse response = researchService.createResearchRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResearchResponse>> getAllResearchReports() {
        List<ResearchResponse> reports = researchService.getAllResearchReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResearchResponse> getResearchReportById(@PathVariable Long id) {
        ResearchResponse report = researchService.getResearchReportById(id);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/company/{companyName}")
    public ResponseEntity<List<ResearchResponse>> getResearchReportsByCompany(@PathVariable String companyName) {
        List<ResearchResponse> reports = researchService.getResearchReportsByCompany(companyName);
        return ResponseEntity.ok(reports);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResearchResponse> updateResearchStatus(
            @PathVariable Long id,
            @RequestBody ResearchStatus status) {
        ResearchResponse response = researchService.updateResearchStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/results")
    public ResponseEntity<ResearchResponse> updateResearchResults(
            @PathVariable Long id,
            @Valid @RequestBody ResearchResultUpdateRequest request) {
        ResearchResponse response = researchService.updateResearchResults(id, request);
        return ResponseEntity.ok(response);
    }
}
