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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UpvoteRepository upvoteRepository;

    private ReportService reportService;

    private Report createTestReport(
            Long id,
            String area,
            ReportStatus status,
            Category category,
            int upvotes) {

        Report report = new Report();
        report.setId(id);
        report.setArea(area);
        report.setStatus(status);
        report.setCategory(category);
        report.setUpvoteCount(upvotes);
        report.setCreatedAt(LocalDateTime.now());

        return report;
    }

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, upvoteRepository);
    }

    @Test
    void createReportStartsAsReportedWithZeroVotes() {
        CreateReportRequest request = new CreateReportRequest("Pothole", "Deep hole in the road",
                Category.POTHOLE, "Downtown", "Main St near no. 42", "Ana", "ana@example.com");
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report report = reportService.createReport(request);

        assertThat(report.getTitle()).isEqualTo("Pothole");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REPORTED);
        assertThat(report.getUpvoteCount()).isZero();
        assertThat(report.getCreatedAt()).isNotNull();
        assertThat(report.getUpdatedAt()).isNotNull();
    }

    @Test
    void findReportByIdThrowsWhenReportDoesNotExist() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.findReportById(99L))
                .isInstanceOf(ReportNotFoundException.class)
                .hasMessage("Report with id 99 not found");
    }

    @Test
    void upvoteReportIncreasesUpvoteCount() {
        Report report = new Report();
        report.setId(1L);
        report.setUpvoteCount(2);

        Report updatedReport = new Report();
        updatedReport.setId(1L);
        updatedReport.setUpvoteCount(3);

        when(reportRepository.findById(1L))
                .thenReturn(Optional.of(report), Optional.of(updatedReport));

        when(upvoteRepository.existsByReportIdAndVoterEmail(
                1L, "ben@example.com")).thenReturn(false);

        when(upvoteRepository.saveAndFlush(any(Upvote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(reportRepository.incrementUpvoteCount(1L)).thenReturn(1);

        Report result = reportService.upvoteReport(1L, "ben@example.com");

        assertThat(result.getUpvoteCount()).isEqualTo(3);

        verify(upvoteRepository).saveAndFlush(any(Upvote.class));
        verify(reportRepository).incrementUpvoteCount(1L);
    }

    @Test
    void upvoteReportThrowsWhenEmailAlreadyVoted() {
        Report report = new Report();
        report.setId(1L);
        report.setUpvoteCount(2);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(upvoteRepository.existsByReportIdAndVoterEmail(1L, "ben@example.com")).thenReturn(true);

        assertThatThrownBy(() -> reportService.upvoteReport(1L, "ben@example.com"))
                .isInstanceOf(DuplicateUpvoteException.class);

        assertThat(report.getUpvoteCount()).isEqualTo(2);
        verify(upvoteRepository, never()).save(any(Upvote.class));
    }

    @Test
    void updateStatusChangesStatusAndUpdatedAt() {
        Report report = new Report();
        report.setId(1L);
        report.setStatus(ReportStatus.REPORTED);

        LocalDateTime previousUpdatedAt = LocalDateTime.now().minusDays(1);
        report.setUpdatedAt(previousUpdatedAt);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Report result = reportService.updateStatus(1L, ReportStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        assertThat(result.getUpdatedAt()).isAfter(previousUpdatedAt);

        verify(reportRepository).save(report);
    }

    @Test
    void updateStatusSetsFixedAtWhenStatusIsFixed() {
        Report report = new Report();
        report.setId(1L);
        report.setStatus(ReportStatus.IN_PROGRESS);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Report result = reportService.updateStatus(1L, ReportStatus.FIXED);

        assertThat(result.getStatus()).isEqualTo(ReportStatus.FIXED);
        assertThat(result.getFixedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getFixedAt()).isEqualTo(result.getUpdatedAt());

        verify(reportRepository).save(report);
    }

    @Test
    void updateStatusThrowsWhenReportDoesNotExist() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reportService.updateStatus(99L, ReportStatus.FIXED))
                .isInstanceOf(ReportNotFoundException.class)
                .hasMessage("Report with id 99 not found");

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void findReportsFiltersByAreaIgnoringCase() {
        Report downtown = createTestReport(
                1L, "Downtown", ReportStatus.REPORTED, Category.POTHOLE, 2);
        Report uptown = createTestReport(
                2L, "Uptown", ReportStatus.REPORTED, Category.POTHOLE, 1);

        when(reportRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(downtown, uptown));

        List<Report> results = reportService.findReports(
                "dOwNtOwN", null, null, "newest");

        assertThat(results)
                .extracting(Report::getArea)
                .containsExactly("Downtown");
    }

    @Test
    void findReportsFiltersByStatus() {
        Report reported = createTestReport(
                1L, "Downtown", ReportStatus.REPORTED, Category.POTHOLE, 2);
        Report fixed = createTestReport(
                2L, "Downtown", ReportStatus.FIXED, Category.POTHOLE, 5);

        when(reportRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(reported, fixed));

        List<Report> results = reportService.findReports(
                null, ReportStatus.FIXED, null, "newest");

        assertThat(results).containsExactly(fixed);
    }

    @Test
    void findReportsFiltersByCategory() {
        Report pothole = createTestReport(
                1L, "Downtown", ReportStatus.REPORTED, Category.POTHOLE, 2);
        Report garbage = createTestReport(
                2L, "Downtown", ReportStatus.REPORTED, Category.GARBAGE, 3);

        when(reportRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(pothole, garbage));

        List<Report> results = reportService.findReports(
                null, null, Category.GARBAGE, "newest");

        assertThat(results).containsExactly(garbage);
    }

    @Test
    void findReportsSortsByVotesDescending() {
        Report highVotes = createTestReport(
                1L, "Downtown", ReportStatus.REPORTED, Category.POTHOLE, 10);
        Report mediumVotes = createTestReport(
                2L, "Downtown", ReportStatus.REPORTED, Category.GARBAGE, 5);
        Report lowVotes = createTestReport(
                3L, "Uptown", ReportStatus.FIXED, Category.STREETLIGHT, 2);

        when(reportRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(highVotes, mediumVotes, lowVotes));

        List<Report> results = reportService.findReports(
                null, null, null, "votes");

        assertThat(results)
                .extracting(Report::getUpvoteCount)
                .containsExactly(10, 5, 2);

        verify(reportRepository).findAll(
                Sort.by(Sort.Direction.DESC, "upvoteCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt")));
    }
    @Test
    void getStatsIncludesAllStatusesAndCategories() {
        for (ReportStatus status : ReportStatus.values()) {
            when(reportRepository.countByStatus(status)).thenReturn(1L);
        }

        for (Category category : Category.values()) {
            when(reportRepository.countByCategory(category)).thenReturn(1L);
        }

        var stats = reportService.getStats();

        assertThat(stats).containsKeys("byStatus", "byCategory");

        assertThat(stats.get("byStatus"))
                .containsOnlyKeys(
                        "REPORTED", "IN_PROGRESS", "FIXED", "REJECTED");

        assertThat(stats.get("byCategory"))
                .containsOnlyKeys(
                        "POTHOLE", "STREETLIGHT", "GARBAGE",
                        "GRAFFITI", "WATER_LEAK", "OTHER");
    }

    @Test
    void getStatsIncludesZeroCounts() {
        for (ReportStatus status : ReportStatus.values()) {
            when(reportRepository.countByStatus(status)).thenReturn(0L);
        }

        for (Category category : Category.values()) {
            when(reportRepository.countByCategory(category)).thenReturn(0L);
        }

        var stats = reportService.getStats();

        assertThat(stats.get("byStatus").values())
                .containsOnly(0L);

        assertThat(stats.get("byCategory").values())
                .containsOnly(0L);
    }
}
