package com.fintrack.controller;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.SummaryReportResponse;
import com.fintrack.entity.CategoryType;
import com.fintrack.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryReportResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        SummaryReportResponse response = reportService.getSummary(from, to, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<CategoryReportResponse>> getByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam CategoryType type,
            Authentication authentication) {
        List<CategoryReportResponse> response = reportService.getByCategory(from, to, type, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyReportResponse>> getMonthly(
            @RequestParam Integer year,
            Authentication authentication) {
        List<MonthlyReportResponse> response = reportService.getMonthly(year, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
