package com.internshield.internship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record InternshipRequest(
        @NotBlank(message = "Internship title must not be blank")
        String title,

        String description,

        @URL(message = "Listing URL must be a valid URL")
        String listingUrl,

        String location,

        String workMode,

        String stipendDetails,

        String internshipType,

        LocalDate applicationDeadline,

        @NotNull(message = "Company ID must be provided")
        Long companyId
) {
}
