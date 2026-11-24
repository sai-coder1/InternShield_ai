package com.internshield.internship.dto;

import com.internshield.internship.Internship;

import java.time.LocalDate;

public record InternshipResponse(
        Long id,
        String title,
        String description,
        String listingUrl,
        String location,
        String workMode,
        String stipendDetails,
        String internshipType,
        LocalDate applicationDeadline,
        Long companyId,
        String companyName
) {
    public static InternshipResponse fromEntity(Internship internship) {
        return new InternshipResponse(
                internship.getId(),
                internship.getTitle(),
                internship.getDescription(),
                internship.getListingUrl(),
                internship.getLocation(),
                internship.getWorkMode(),
                internship.getStipendDetails(),
                internship.getInternshipType(),
                internship.getApplicationDeadline(),
                internship.getCompany() != null ? internship.getCompany().getId() : null,
                internship.getCompany() != null ? internship.getCompany().getName() : null
        );
    }
}
