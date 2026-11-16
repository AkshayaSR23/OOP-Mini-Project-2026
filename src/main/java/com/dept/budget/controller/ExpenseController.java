package com.dept.budget.controller;

import com.dept.budget.entity.Expense;
import com.dept.budget.entity.ExpenseType;
import com.dept.budget.service.ExpenseService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Member 2: expenses, receipt files and the category suggestion. */
@RestController
@RequestMapping("/api")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/expense-types")
    public List<Map<String, Object>> getExpenseTypes() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ExpenseType type : expenseService.getExpenseTypes()) {
            list.add(type.toMap());
        }
        return list;
    }

    @GetMapping("/expenses")
    public List<Map<String, Object>> getExpenses(@RequestParam Integer budgetYearId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Expense e : expenseService.getExpenses(budgetYearId)) {
            list.add(e.toMap());
        }
        return list;
    }

    @PostMapping("/expenses")
    public Map<String, Object> addExpense(@RequestBody ExpenseRequest request) {
        return expenseService.addExpense(request).toMap();
    }

    @PutMapping("/expenses/{id}")
    public Map<String, Object> editExpense(@PathVariable Integer id, @RequestBody ExpenseRequest request) {
        return expenseService.editExpense(id, request).toMap();
    }

    @PostMapping("/expenses/{id}/receipt")
    public Map<String, Object> uploadReceipt(@PathVariable Integer id,
                                             @RequestParam(value = "file", required = false) MultipartFile file) {
        expenseService.saveReceipt(id, file);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", "Receipt uploaded");
        return map;
    }

    @GetMapping("/expenses/{id}/receipt")
    public ResponseEntity<Resource> viewReceipt(@PathVariable Integer id) {
        Path path = expenseService.getReceiptPath(id);
        String name = path.getFileName().toString();
        MediaType type = name.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : name.endsWith(".png") ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                .body(new FileSystemResource(path));
    }

    @GetMapping("/suggestions")
    public List<Map<String, Object>> suggestCategories(@RequestParam Integer budgetYearId,
                                                       @RequestParam Integer typeId,
                                                       @RequestParam(required = false) String amount) {
        return expenseService.suggestCategories(budgetYearId, typeId, amount);
    }
}
