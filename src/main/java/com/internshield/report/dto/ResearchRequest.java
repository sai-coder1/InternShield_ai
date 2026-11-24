package com.internshield.report.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record ResearchRequest(
        @NotBlank(message = "Company name must not be blank")
        String companyName,

        @URL(message = "Internship URL must be a valid URL")
        String internshipUrl
) {
}
