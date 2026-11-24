package com.internshield.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByResearchReportId(Long researchReportId);

    Optional<Evidence> findByIdAndResearchReportId(Long id, Long researchReportId);

    boolean existsBySourceUrlAndResearchReportId(String sourceUrl, Long researchReportId);

    long countByResearchReportId(Long researchReportId);

    List<Evidence> findByCategoryAndResearchReportId(EvidenceCategory category, Long researchReportId);
}
