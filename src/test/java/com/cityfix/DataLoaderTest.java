package com.cityfix;

import com.cityfix.model.Report;
import com.cityfix.model.Upvote;
import com.cityfix.repository.ReportRepository;
import com.cityfix.repository.UpvoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DataLoaderTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UpvoteRepository upvoteRepository;

    private DataLoader dataLoader;

    @BeforeEach
    void setUp() {
        dataLoader = new DataLoader(reportRepository, upvoteRepository);
        upvoteRepository.deleteAll();
        reportRepository.deleteAll();
    }

    @Test
    void sampleReportsHaveMatchingUpvoteCountsAndRecords() {
        dataLoader.run();

        var reports = reportRepository.findAll();
        var upvotes = upvoteRepository.findAll();

        assertThat(reports).hasSize(8);
        assertThat(upvotes).hasSize(55);

        for (Report report : reports) {
            long actualUpvotes = upvotes.stream()
                    .filter(upvote -> upvote.getReport().getId().equals(report.getId()))
                    .count();

            assertThat(actualUpvotes)
                    .as("Upvote rows for report: %s", report.getTitle())
                    .isEqualTo(report.getUpvoteCount());
        }
    }

    @Test
    void doesNotLoadSampleDataWhenReportsAlreadyExist() {
        Report existingReport = new Report();
        existingReport.setTitle("Existing report");
        reportRepository.save(existingReport);

        dataLoader.run();

        assertThat(reportRepository.findAll()).hasSize(1);
        assertThat(upvoteRepository.findAll()).isEmpty();
    }
}
