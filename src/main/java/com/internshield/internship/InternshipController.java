package com.internshield.internship;

import com.internshield.internship.dto.InternshipRequest;
import com.internshield.internship.dto.InternshipResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internships")
public class InternshipController {

    private final InternshipService internshipService;

    public InternshipController(InternshipService internshipService) {
        this.internshipService = internshipService;
    }

    @PostMapping
    public ResponseEntity<InternshipResponse> createInternship(@Valid @RequestBody InternshipRequest request) {
        InternshipResponse created = internshipService.createInternship(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<InternshipResponse>> getAllInternships() {
        List<InternshipResponse> internships = internshipService.getAllInternships();
        return ResponseEntity.ok(internships);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternshipResponse> getInternshipById(@PathVariable Long id) {
        InternshipResponse internship = internshipService.getInternshipById(id);
        return ResponseEntity.ok(internship);
    }
}
