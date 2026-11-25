package com.internshield.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResearchReportRepository extends JpaRepository<ResearchReport, Long> {

    List<ResearchReport> findByCompanyName(String companyName);

    List<ResearchReport> findByStatus(ResearchStatus status);

    List<ResearchReport> findAllByOrderByCreatedAtDesc();

    List<ResearchReport> findByUserIdOrderByCreatedAtDesc(String userId);

    java.util.Optional<ResearchReport> findByIdAndUserId(Long id, String userId);
}
