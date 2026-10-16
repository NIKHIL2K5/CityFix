
package com.cityfix.service;

import com.cityfix.dto.CreateReportRequest;
import com.cityfix.exception.DuplicateUpvoteException;
import com.cityfix.exception.ReportNotFoundException;
import com.cityfix.model.Category;
import com.cityfix.model.Report;
import com.cityfix.model.ReportStatus;
import com.cityfix.model.Upvote;
import com.cityfix.repository.ReportRepository;
import com.cityfix.repository.UpvoteRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final UpvoteRepository upvoteRepository;

    public ReportService(ReportRepository reportRepository, UpvoteRepository upvoteRepository) {
        this.reportRepository = reportRepository;
        this.upvoteRepository = upvoteRepository;
    }

    public Report createReport(CreateReportRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Report report = new Report();
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setCategory(request.category());
        report.setArea(request.area());
        report.setAddress(request.address());
        report.setReporterName(request.reporterName());
        report.setReporterEmail(request.reporterEmail());
        report.setStatus(ReportStatus.REPORTED);
        report.setUpvoteCount(0);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        Report savedReport = reportRepository.save(report);
        log.info("Report {} created in area {}", savedReport.getId(), savedReport.getArea());
        return savedReport;
    }

    public Report findReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    public List<Report> findReports(String area, ReportStatus status, Category category, String sort) {
        return reportRepository.findAll(createSort(sort)).stream()
                .filter(report -> area == null || report.getArea().equalsIgnoreCase(area))
                .filter(report -> status == null || report.getStatus() == status)
                .filter(report -> category == null || report.getCategory() == category)
                .toList();
    }

    public Report updateStatus(Long id, ReportStatus newStatus) {
        Report report = findReportById(id);
        LocalDateTime now = LocalDateTime.now();

        report.setStatus(newStatus);
        report.setUpdatedAt(now);
        if (newStatus == ReportStatus.FIXED) {
            report.setFixedAt(now);
        }

        Report savedReport = reportRepository.save(report);
        log.info("Report {} status changed to {}", id, newStatus);
        return savedReport;
    }

    @Transactional
    public Report upvoteReport(Long id, String voterEmail) {
        Report report = findReportById(id);

        if (upvoteRepository.existsByReportIdAndVoterEmail(id, voterEmail)) {
            throw new DuplicateUpvoteException(voterEmail);
        }

        Upvote upvote = new Upvote();
        upvote.setReport(report);
        upvote.setVoterEmail(voterEmail);
        upvote.setCreatedAt(LocalDateTime.now());

        try {
            // Force the insert now so a unique-constraint violation
            // can be translated into a duplicate-upvote exception.
            upvoteRepository.saveAndFlush(upvote);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueConstraintViolation(exception)) {
                throw new DuplicateUpvoteException(voterEmail);
            }
            throw exception;
        }

        // Increment the stored count atomically in the database.
        int updatedRows = reportRepository.incrementUpvoteCount(id);

        if (updatedRows == 0) {
            throw new ReportNotFoundException(id);
        }

        // The bulk update clears the persistence context.
        // Reload the report to return its current count.
        return findReportById(id);
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        for (Throwable cause = exception;
             cause != null;
             cause = cause.getCause()) {

            if (cause instanceof ConstraintViolationException violation
                    && isUniqueViolation(violation.getSQLException())) {
                return true;
            }

            if (cause instanceof SQLException sqlException
                    && isUniqueViolation(sqlException)) {
                return true;
            }
        }

        return false;
    }

    private boolean isUniqueViolation(SQLException exception) {
        return "23505".equals(exception.getSQLState())
                || ("23000".equals(exception.getSQLState())
                    && exception.getErrorCode() == 1062);
    }

    public Map<String, Map<String, Long>> getStats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReportStatus status : ReportStatus.values()) {
            byStatus.put(status.name(), reportRepository.countByStatus(status));
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            byCategory.put(category.name(), reportRepository.countByCategory(category));
        }

        return Map.of("byStatus", byStatus, "byCategory", byCategory);
    }

    private Sort createSort(String sort) {
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("votes".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "upvoteCount").and(newestFirst);
        }
        return newestFirst;
    }
}
