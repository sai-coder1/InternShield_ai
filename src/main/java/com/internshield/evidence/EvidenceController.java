package com.internshield.evidence;

import com.internshield.evidence.dto.EvidenceRequest;
import com.internshield.evidence.dto.EvidenceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping
    public ResponseEntity<EvidenceResponse> createEvidence(@Valid @RequestBody EvidenceRequest request) {
        EvidenceResponse response = evidenceService.createEvidence(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvidenceResponse> getEvidenceById(@PathVariable Long id) {
        EvidenceResponse response = evidenceService.getEvidenceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceByReportId(@PathVariable Long reportId) {
        List<EvidenceResponse> responses = evidenceService.getEvidenceByReportId(reportId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/report/{reportId}/category/{category}")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceByCategory(
            @PathVariable Long reportId,
            @PathVariable EvidenceCategory category) {
        List<EvidenceResponse> responses = evidenceService.getEvidenceByCategory(reportId, category);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/report/{reportId}/count")
    public ResponseEntity<Long> getEvidenceCount(@PathVariable Long reportId) {
        long count = evidenceService.getEvidenceCount(reportId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable Long id) {
        evidenceService.deleteEvidence(id);
        return ResponseEntity.noContent().build();
    }
}
