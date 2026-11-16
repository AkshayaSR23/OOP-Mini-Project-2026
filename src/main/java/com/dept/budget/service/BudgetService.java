package com.dept.budget.service;

import com.dept.budget.entity.Allocation;
import com.dept.budget.entity.AllocationHistory;
import com.dept.budget.entity.BudgetYear;
import com.dept.budget.entity.Category;
import com.dept.budget.entity.Staff;
import com.dept.budget.exception.InvalidInputException;
import com.dept.budget.repository.AllocationHistoryRepository;
import com.dept.budget.repository.AllocationRepository;
import com.dept.budget.repository.BudgetYearRepository;
import com.dept.budget.repository.CategoryRepository;
import com.dept.budget.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Member 1: login, the budget proposal (create, edit, fix, download)
 * and moving money between categories after the budget is fixed.
 */
@Service
public class BudgetService {

    // 10 crore: anything bigger is almost certainly a typing mistake
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");

    private final StaffRepository staffRepository;
    private final BudgetYearRepository budgetYearRepository;
    private final CategoryRepository categoryRepository;
    private final AllocationRepository allocationRepository;
    private final AllocationHistoryRepository historyRepository;

    public BudgetService(StaffRepository staffRepository,
                         BudgetYearRepository budgetYearRepository,
                         CategoryRepository categoryRepository,
                         AllocationRepository allocationRepository,
                         AllocationHistoryRepository historyRepository) {
        this.staffRepository = staffRepository;
        this.budgetYearRepository = budgetYearRepository;
        this.categoryRepository = categoryRepository;
        this.allocationRepository = allocationRepository;
        this.historyRepository = historyRepository;
    }

    // ---------- Login ----------

    public Staff login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new InvalidInputException("Enter both username and password");
        }
        Staff staff = staffRepository.findByUsername(username.trim())
                .orElseThrow(() -> new InvalidInputException("Invalid username or password"));
        if (!staff.login(password)) {
            // same message on purpose: do not reveal which part was wrong
            throw new InvalidInputException("Invalid username or password");
        }
        return staff;
    }

    // ---------- Reading ----------

    public List<Category> getCategories() {
        return categoryRepository.findAllByOrderByCodeAsc();
    }

    public List<BudgetYear> getAllYears() {
        return budgetYearRepository.findAllByOrderByAcademicYearDesc();
    }

    public BudgetYear getYear(Integer budgetYearId) {
        if (budgetYearId == null) {
            throw new InvalidInputException("Choose a budget year");
        }
        return budgetYearRepository.findById(budgetYearId)
                .orElseThrow(() -> new InvalidInputException("Budget year not found"));
    }

    public List<AllocationHistory> getHistory(Integer budgetYearId) {
        return historyRepository.findByAllocation_BudgetYear_BudgetYearIdOrderByChangedAtDesc(budgetYearId);
    }

    // ---------- Proposal ----------

    @Transactional
    public BudgetYear createProposal(String academicYear, Integer staffId) {
        String year = academicYear == null ? "" : academicYear.trim();
        if (!year.matches("\\d{4}-\\d{2}")) {
            throw new InvalidInputException("Academic year must look like 2026-27");
        }
        int startYear = Integer.parseInt(year.substring(0, 4));
        int endPart = Integer.parseInt(year.substring(5));
        if ((startYear + 1) % 100 != endPart) {
            throw new InvalidInputException("The second year must follow the first, for example 2026-27");
        }
        if (budgetYearRepository.existsByAcademicYear(year)) {
            throw new InvalidInputException("A proposal for " + year + " already exists");
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new InvalidInputException("Please log in again"));

        BudgetYear budgetYear = new BudgetYear(year, staff);
        for (Category category : categoryRepository.findAllByOrderByCodeAsc()) {
            budgetYear.getAllocations().add(new Allocation(budgetYear, category, BigDecimal.ZERO));
        }
        return budgetYearRepository.save(budgetYear);   // also saves the 7 allocations (cascade)
    }

    /** amounts: category id -> amount typed by the staff */
    @Transactional
    public BudgetYear updateProposal(Integer budgetYearId, Map<String, String> amounts) {
        BudgetYear year = getYear(budgetYearId);
        if (year.isFixed()) {
            throw new InvalidInputException("This proposal is fixed and can no longer be edited");
        }
        if (amounts == null) {
            throw new InvalidInputException("Enter the amounts for all categories");
        }
        for (Allocation allocation : year.getAllocations()) {
            Category category = allocation.getCategory();
            String typed = amounts.get(String.valueOf(category.getCategoryId()));
            allocation.setAmount(parseAmount(typed, "Category " + category.getCode()));
        }
        return budgetYearRepository.save(year);
    }

    @Transactional
    public BudgetYear fixProposal(Integer budgetYearId) {
        BudgetYear year = getYear(budgetYearId);
        year.fixProposal();   // the rules live in BudgetYear
        return budgetYearRepository.save(year);
    }

    public byte[] exportProposal(Integer budgetYearId, boolean pdf) {
        BudgetYear year = getYear(budgetYearId);
        return pdf ? year.exportToPdf() : year.exportToExcel();
    }

    // ---------- Moving funds ----------

    @Transactional
    public void moveFunds(Integer fromAllocationId, Integer toAllocationId, String amountText, String reasonText) {
        if (fromAllocationId == null || toAllocationId == null) {
            throw new InvalidInputException("Choose both categories");
        }
        if (fromAllocationId.equals(toAllocationId)) {
            throw new InvalidInputException("Choose two different categories");
        }
        Allocation from = allocationRepository.findById(fromAllocationId)
                .orElseThrow(() -> new InvalidInputException("Category not found"));
        Allocation to = allocationRepository.findById(toAllocationId)
                .orElseThrow(() -> new InvalidInputException("Category not found"));

        if (!from.getBudgetYear().getBudgetYearId().equals(to.getBudgetYear().getBudgetYearId())) {
            throw new InvalidInputException("Both categories must be in the same budget year");
        }
        if (!from.getBudgetYear().isFixed()) {
            throw new InvalidInputException("Funds can be moved only after the proposal is fixed. Edit the proposal instead.");
        }

        BigDecimal amount = parseAmount(amountText, "Amount");
        if (amount.signum() == 0) {
            throw new InvalidInputException("Amount to move must be greater than zero");
        }
        String reason = reasonText == null ? "" : reasonText.trim();
        if (reason.isEmpty() || !reason.matches(".*[A-Za-z].*")) {
            throw new InvalidInputException("Write a reason for moving the funds");
        }
        if (reason.length() > 200) {
            throw new InvalidInputException("Keep the reason under 200 characters");
        }
        if (!from.canAccommodate(amount)) {
            throw new InvalidInputException(from.getCategory().getLabel() + " has only Rs. "
                    + from.getRemaining().toPlainString() + " unused");
        }

        AllocationHistory taken = from.changeAmount(from.getAmount().subtract(amount),
                "Moved to " + to.getCategory().getLabel() + ": " + reason);
        AllocationHistory given = to.changeAmount(to.getAmount().add(amount),
                "Moved from " + from.getCategory().getLabel() + ": " + reason);

        allocationRepository.save(from);
        allocationRepository.save(to);
        historyRepository.save(taken);
        historyRepository.save(given);
    }

    // ---------- Validation helper ----------

    /** Accepts 0 or a positive number with at most 2 decimals. Rejects letters and minus signs. */
    private BigDecimal parseAmount(String text, String label) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException(label + ": amount is required");
        }
        String value = text.trim();
        if (!value.matches("\\d+(\\.\\d{1,2})?")) {
            throw new InvalidInputException(label + ": enter a number like 50000 or 50000.50 (no letters or minus sign)");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidInputException(label + ": this amount looks too large, please check it");
        }
        return amount;
    }
}
