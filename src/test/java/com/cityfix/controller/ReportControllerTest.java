package com.cityfix.controller;

import com.cityfix.dto.CreateReportRequest;
import com.cityfix.exception.ReportNotFoundException;
import com.cityfix.model.Category;
import com.cityfix.model.Report;
import com.cityfix.model.ReportStatus;
import com.cityfix.service.ReportService;
import com.cityfix.exception.DuplicateUpvoteException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void createReportReturns201() throws Exception {
        Report report = new Report();
        report.setId(1L);
        report.setTitle("Pothole");
        report.setCategory(Category.POTHOLE);
        report.setStatus(ReportStatus.REPORTED);
        when(reportService.createReport(any(CreateReportRequest.class))).thenReturn(report);

        String json = """
                {
                  "title": "Pothole",
                  "description": "Deep hole in the road",
                  "category": "POTHOLE",
                  "area": "Downtown",
                  "address": "Main St near no. 42",
                  "reporterName": "Ana",
                  "reporterEmail": "ana@example.com"
                }
                """;

        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    @Test
    void getMissingReportReturns404() throws Exception {
        when(reportService.findReportById(99L)).thenThrow(new ReportNotFoundException(99L));

        mockMvc.perform(get("/api/reports/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Report with id 99 not found"));
    }

    @Test
    void createReportWithoutTitleReturns400() throws Exception {
        String json = """
                {
                  "description": "Deep hole in the road",
                  "category": "POTHOLE",
                  "area": "Downtown",
                  "reporterName": "Ana",
                  "reporterEmail": "ana@example.com"
                }
                """;

        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void duplicateUpvoteReturns409Conflict() throws Exception {
        when(reportService.upvoteReport(1L, "ben@example.com"))
                .thenThrow(new DuplicateUpvoteException("ben@example.com"));

        String json = """
              {
              "voterEmail": "ben@example.com"
               }
                """;

        mockMvc.perform(post("/api/reports/1/upvotes")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("ben@example.com has already upvoted this report"));
    }

}
