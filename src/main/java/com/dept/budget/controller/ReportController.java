package com.dept.budget.controller;

import com.dept.budget.exception.InvalidInputException;
import com.dept.budget.service.ExportUtil;
import com.dept.budget.service.Report;
import com.dept.budget.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Member 3: dashboard, unused funds and reports. */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard(@RequestParam Integer budgetYearId) {
        return reportService.getDashboard(budgetYearId);
    }

    @GetMapping("/unused-funds")
    public Map<String, Object> getUnusedFunds(@RequestParam Integer budgetYearId) {
        return reportService.getUnusedFunds(budgetYearId);
    }

    /** type is "period" or "category" */
    @GetMapping("/reports/{type}")
    public Map<String, Object> getReport(@PathVariable String type,
                                         @RequestParam(required = false) String categoryId,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false) String kind) {
        checkType(type);
        Report report = reportService.buildReport(type, categoryId, from, to, kind);
        return reportService.runReport(report);
    }

    @GetMapping("/reports/{type}/export")
    public ResponseEntity<byte[]> exportReport(@PathVariable String type,
                                               @RequestParam(required = false) String categoryId,
                                               @RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) String kind,
                                               @RequestParam String format) {
        checkType(type);
        boolean pdf = ExportUtil.isPdf(format);
        Report report = reportService.buildReport(type, categoryId, from, to, kind);
        byte[] file = reportService.exportReport(report, pdf);
        String name = (type.equals("category") ? "Category_Report_" : "Period_Report_") + from + "_to_" + to;
        return ExportUtil.fileResponse(file, name + (pdf ? ".pdf" : ".xlsx"));
    }

    private void checkType(String type) {
        if (!type.equals("period") && !type.equals("category")) {
            throw new InvalidInputException("Unknown report type");
        }
    }
}
