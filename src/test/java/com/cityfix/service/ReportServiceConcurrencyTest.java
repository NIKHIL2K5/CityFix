package com.cityfix.service;

import com.cityfix.model.Category;
import com.cityfix.model.Report;
import com.cityfix.model.ReportStatus;
import com.cityfix.repository.ReportRepository;
import com.cityfix.repository.UpvoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportServiceConcurrencyTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UpvoteRepository upvoteRepository;

    @Test
    void simultaneousUpvotesFromDifferentVotersAreBothCounted() throws Exception {
        Report report = new Report();
        report.setTitle("Concurrent upvote test");
        report.setDescription("Testing concurrent upvotes");
        report.setCategory(Category.POTHOLE);
        report.setArea("Downtown");
        report.setAddress("Main Street");
        report.setStatus(ReportStatus.REPORTED);
        report.setReporterName("Test User");
        report.setReporterEmail("reporter@example.com");
        report.setUpvoteCount(0);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        Report savedReport = reportRepository.saveAndFlush(report);
        Long reportId = savedReport.getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> firstVote = executor.submit(() -> {
                ready.countDown();
                start.await();
                reportService.upvoteReport(reportId, "alice@example.com");
                return null;
            });

            Future<?> secondVote = executor.submit(() -> {
                ready.countDown();
                start.await();
                reportService.upvoteReport(reportId, "bob@example.com");
                return null;
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            firstVote.get(10, TimeUnit.SECONDS);
            secondVote.get(10, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Report updatedReport = reportRepository.findById(reportId).orElseThrow();

        long storedUpvotes = upvoteRepository.findAll().stream()
                .filter(upvote -> upvote.getReport().getId().equals(reportId))
                .count();

        assertThat(storedUpvotes).isEqualTo(2);
        assertThat(updatedReport.getUpvoteCount()).isEqualTo(2);
    }
    @Test
    void simultaneousUpvotesFromSameVoterOnlyCountOnce() throws Exception {
        Report report = new Report();
        report.setTitle("Duplicate vote test");
        report.setDescription("Testing simultaneous duplicate votes");
        report.setCategory(Category.POTHOLE);
        report.setArea("Downtown");
        report.setAddress("Main Street");
        report.setStatus(ReportStatus.REPORTED);
        report.setReporterName("Test User");
        report.setReporterEmail("reporter@example.com");
        report.setUpvoteCount(0);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        Long reportId = reportRepository.saveAndFlush(report).getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> firstVote = executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    reportService.upvoteReport(reportId, "same@example.com");
                    return true;
                } catch (com.cityfix.exception.DuplicateUpvoteException exception) {
                    return false;
                }
            });

            Future<Boolean> secondVote = executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    reportService.upvoteReport(reportId, "same@example.com");
                    return true;
                } catch (com.cityfix.exception.DuplicateUpvoteException exception) {
                    return false;
                }
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean firstSucceeded = firstVote.get(10, TimeUnit.SECONDS);
            boolean secondSucceeded = secondVote.get(10, TimeUnit.SECONDS);

            assertThat(firstSucceeded ^ secondSucceeded).isTrue();
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Report updatedReport = reportRepository.findById(reportId).orElseThrow();

        long storedUpvotes = upvoteRepository.findAll().stream()
                .filter(upvote -> upvote.getReport().getId().equals(reportId))
                .count();

        assertThat(storedUpvotes).isEqualTo(1);
        assertThat(updatedReport.getUpvoteCount()).isEqualTo(1);
    }

}
