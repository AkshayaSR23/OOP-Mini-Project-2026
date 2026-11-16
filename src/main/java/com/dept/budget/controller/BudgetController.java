package com.dept.budget.controller;

import com.dept.budget.entity.AllocationHistory;
import com.dept.budget.entity.BudgetYear;
import com.dept.budget.entity.Category;
import com.dept.budget.entity.Staff;
import com.dept.budget.service.BudgetService;
import com.dept.budget.service.ExportUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Member 1: login, budget proposal and moving funds. */
@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    // ---------- Login ----------

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpSession session) {
        Staff staff = budgetService.login(body.get("username"), body.get("password"));
        session.setAttribute("staffId", staff.getStaffId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("username", staff.getUsername());
        return map;
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return message("Logged out");
    }

    // ---------- Categories and budget years ----------

    @GetMapping("/categories")
    public List<Map<String, Object>> getCategories() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Category c : budgetService.getCategories()) {
            list.add(c.toMap());
        }
        return list;
    }

    @GetMapping("/budget-years")
    public List<Map<String, Object>> getYears() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BudgetYear year : budgetService.getAllYears()) {
            list.add(year.toMap(false));
        }
        return list;
    }

    @GetMapping("/budget-years/{id}")
    public Map<String, Object> getYear(@PathVariable Integer id) {
        return budgetService.getYear(id).toMap(true);
    }

    // ---------- Proposal ----------

    @PostMapping("/budget-years")
    public Map<String, Object> createProposal(@RequestBody Map<String, String> body, HttpSession session) {
        Integer staffId = (Integer) session.getAttribute("staffId");
        return budgetService.createProposal(body.get("academicYear"), staffId).toMap(true);
    }

    @PutMapping("/budget-years/{id}/allocations")
    public Map<String, Object> updateProposal(@PathVariable Integer id, @RequestBody Map<String, String> amounts) {
        return budgetService.updateProposal(id, amounts).toMap(true);
    }

    @PostMapping("/budget-years/{id}/fix")
    public Map<String, Object> fixProposal(@PathVariable Integer id) {
        return budgetService.fixProposal(id).toMap(true);
    }

    @GetMapping("/budget-years/{id}/export")
    public ResponseEntity<byte[]> exportProposal(@PathVariable Integer id, @RequestParam String format) {
        boolean pdf = ExportUtil.isPdf(format);
        String academicYear = budgetService.getYear(id).getAcademicYear();
        byte[] file = budgetService.exportProposal(id, pdf);
        return ExportUtil.fileResponse(file, "Budget_Proposal_" + academicYear + (pdf ? ".pdf" : ".xlsx"));
    }

    // ---------- Moving funds ----------

    @PostMapping("/allocations/move")
    public Map<String, Object> moveFunds(@RequestBody Map<String, String> body) {
        budgetService.moveFunds(toId(body.get("fromAllocationId")), toId(body.get("toAllocationId")),
                body.get("amount"), body.get("reason"));
        return message("Funds moved");
    }

    @GetMapping("/budget-years/{id}/history")
    public List<Map<String, Object>> getHistory(@PathVariable Integer id) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AllocationHistory h : budgetService.getHistory(id)) {
            list.add(h.toMap());
        }
        return list;
    }

    // ---------- Helpers ----------

    private Integer toId(String text) {
        if (text == null || !text.trim().matches("\\d+")) {
            return null;
        }
        return Integer.valueOf(text.trim());
    }

    private Map<String, Object> message(String text) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", text);
        return map;
    }
}
