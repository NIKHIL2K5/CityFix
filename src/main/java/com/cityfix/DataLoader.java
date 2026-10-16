package com.cityfix;

import com.cityfix.model.Category;
import com.cityfix.model.Report;
import com.cityfix.model.ReportStatus;
import com.cityfix.model.Upvote;
import com.cityfix.repository.ReportRepository;
import com.cityfix.repository.UpvoteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final ReportRepository reportRepository;
    private final UpvoteRepository upvoteRepository;

    public DataLoader(ReportRepository reportRepository,
                      UpvoteRepository upvoteRepository) {
        this.reportRepository = reportRepository;
        this.upvoteRepository = upvoteRepository;
    }

    @Override
    public void run(String... args) {
        if (reportRepository.count() > 0) {
            return;
        }

        addReport("Large pothole on Main Street", "The hole is about half a metre wide and damages car tyres.",
                Category.POTHOLE, "Downtown", "Main St near no. 42", ReportStatus.REPORTED, 12, 9, 0);
        addReport("Streetlight not working", "The light has been off for two weeks and the road is very dark at night.",
                Category.STREETLIGHT, "Riverside", "Park Road, corner with Mill Lane", ReportStatus.IN_PROGRESS, 7, 6, 0);
        addReport("Garbage bin overflowing", "The bin is full and rubbish is spreading onto the pavement.",
                Category.GARBAGE, "Old Town", "Market Square, next to the bus stop", ReportStatus.REPORTED, 4, 3, 0);
        addReport("Graffiti on the school wall", "Large paint marks cover the wall along the playground.",
                Category.GRAFFITI, "Hillside", "Oak Avenue, Hillside Primary School", ReportStatus.REJECTED, 1, 8, 5);
        addReport("Water leak at the crossing", "Water is coming out of the road and forming a puddle every morning.",
                Category.WATER_LEAK, "Downtown", "Station Road crossing", ReportStatus.FIXED, 15, 14, 4);
        addReport("Broken streetlight near the park", "The lamp head is hanging down and looks unsafe.",
                Category.STREETLIGHT, "Riverside", "Riverside Park, north gate", ReportStatus.FIXED, 5, 20, 12);
        addReport("Deep pothole after the rain", "The pothole fills with water and cyclists cannot see it.",
                Category.POTHOLE, "Old Town", "Church Lane no. 8", ReportStatus.IN_PROGRESS, 9, 5, 1);
        addReport("Fallen tree branch on the path", "A large branch is blocking half of the footpath.",
                Category.OTHER, "Hillside", "Footpath behind Oak Avenue", ReportStatus.REPORTED, 2, 1, 0);
    }

    private void addReport(String title, String description, Category category,
                           String area, String address, ReportStatus status,
                           int upvoteCount, int daysAgo, int daysUntilUpdate) {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(daysAgo);
        LocalDateTime updatedAt = createdAt.plusDays(daysUntilUpdate);

        Report report = new Report();
        report.setTitle(title);
        report.setDescription(description);
        report.setCategory(category);
        report.setArea(area);
        report.setAddress(address);
        report.setStatus(status);
        report.setReporterName("Sample Resident");
        report.setReporterEmail("resident@example.com");
        report.setUpvoteCount(upvoteCount);
        report.setCreatedAt(createdAt);
        report.setUpdatedAt(updatedAt);

        if (status == ReportStatus.FIXED) {
            report.setFixedAt(updatedAt);
        }

        Report savedReport = reportRepository.save(report);

        for (int i = 1; i <= upvoteCount; i++) {
            Upvote upvote = new Upvote();
            upvote.setReport(savedReport);
            upvote.setVoterEmail(
                    "sample-voter-" + i + "-report-" + savedReport.getId() + "@example.com");
            upvote.setCreatedAt(createdAt);
            upvoteRepository.save(upvote);
        }
    }
}
