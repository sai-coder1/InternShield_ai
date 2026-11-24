package com.internshield.evidence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.internshield.report.ResearchReport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Evidence title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Source URL is required")
    @Column(name = "source_url", length = 1024, nullable = false)
    private String sourceUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_domain")
    private String sourceDomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvidenceCategory category;

    @Column(name = "source_credibility", length = 64)
    private String sourceCredibility;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "research_report_id", nullable = false)
    @JsonIgnoreProperties({"evidenceList", "hibernateLazyInitializer", "handler"})
    private ResearchReport researchReport;

    public Evidence() {
    }

    public Evidence(String title, String sourceUrl, String description, String sourceDomain,
                    EvidenceCategory category, String sourceCredibility, ResearchReport researchReport) {
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.description = description;
        this.sourceDomain = sourceDomain;
        this.category = category;
        this.sourceCredibility = sourceCredibility;
        this.researchReport = researchReport;
    }

    @PrePersist
    protected void onCreate() {
        if (this.collectedAt == null) {
            this.collectedAt = LocalDateTime.now();
        }
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

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceDomain() {
        return sourceDomain;
    }

    public void setSourceDomain(String sourceDomain) {
        this.sourceDomain = sourceDomain;
    }

    public EvidenceCategory getCategory() {
        return category;
    }

    public void setCategory(EvidenceCategory category) {
        this.category = category;
    }

    public String getSourceCredibility() {
        return sourceCredibility;
    }

    public void setSourceCredibility(String sourceCredibility) {
        this.sourceCredibility = sourceCredibility;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public ResearchReport getResearchReport() {
        return researchReport;
    }

    public void setResearchReport(ResearchReport researchReport) {
        this.researchReport = researchReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Evidence evidence = (Evidence) o;
        return Objects.equals(id, evidence.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
