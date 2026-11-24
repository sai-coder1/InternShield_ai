package com.internshield.company.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank(message = "Company name must not be blank")
        String name,
        String website,
        String domain,
        String description
) {
}
