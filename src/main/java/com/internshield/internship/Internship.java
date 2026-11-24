package com.internshield.internship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.internshield.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "internships")
public class Internship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Internship title is required")
    @Column(nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "listing_url")
    private String listingUrl;

    @Column(name = "location")
    private String location;

    @Column(name = "work_mode")
    private String workMode;

    @Column(name = "stipend_details")
    private String stipendDetails;

    @Column(name = "internship_type")
    private String internshipType;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonIgnoreProperties({"internships", "hibernateLazyInitializer", "handler"})
    private Company company;

    public Internship() {
    }

    public Internship(String title, String description, String listingUrl, String location,
                      String workMode, String stipendDetails, String internshipType,
                      LocalDate applicationDeadline, Company company) {
        this.title = title;
        this.description = description;
        this.listingUrl = listingUrl;
        this.location = location;
        this.workMode = workMode;
        this.stipendDetails = stipendDetails;
        this.internshipType = internshipType;
        this.applicationDeadline = applicationDeadline;
        this.company = company;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getListingUrl() {
        return listingUrl;
    }

    public void setListingUrl(String listingUrl) {
        this.listingUrl = listingUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public String getStipendDetails() {
        return stipendDetails;
    }

    public void setStipendDetails(String stipendDetails) {
        this.stipendDetails = stipendDetails;
    }

    public String getInternshipType() {
        return internshipType;
    }

    public void setInternshipType(String internshipType) {
        this.internshipType = internshipType;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Internship that = (Internship) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
