package com.internshield.company.dto;

import com.internshield.company.Company;

public record CompanyResponse(
        Long id,
        String name,
        String website,
        String domain,
        String description
) {
    public static CompanyResponse fromEntity(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getDomain(),
                company.getDescription()
        );
    }
}
