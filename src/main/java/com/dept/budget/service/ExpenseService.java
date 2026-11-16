package com.dept.budget.service;

import com.dept.budget.controller.ExpenseRequest;
import com.dept.budget.entity.Allocation;
import com.dept.budget.entity.BudgetYear;
import com.dept.budget.entity.Expense;
import com.dept.budget.entity.ExpenseType;
import com.dept.budget.entity.NonRecurringExpense;
import com.dept.budget.entity.RecurringExpense;
import com.dept.budget.exception.DuplicateReceiptException;
import com.dept.budget.exception.InvalidInputException;
import com.dept.budget.repository.AllocationRepository;
import com.dept.budget.repository.BudgetYearRepository;
import com.dept.budget.repository.ExpenseRepository;
import com.dept.budget.repository.ExpenseTypeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Member 2: adding and editing expenses, all expense validation,
 * the duplicate receipt check, the category suggestion and receipt uploads.
 */
@Service
public class ExpenseService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");
    private static final Set<String> FREQUENCIES = Set.of("MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY");
    private static final Set<String> FILE_TYPES = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;   // 5 MB

    private final ExpenseRepository expenseRepository;
    private final AllocationRepository allocationRepository;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final BudgetYearRepository budgetYearRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public ExpenseService(ExpenseRepository expenseRepository,
                          AllocationRepository allocationRepository,
                          ExpenseTypeRepository expenseTypeRepository,
                          BudgetYearRepository budgetYearRepository) {
        this.expenseRepository = expenseRepository;
        this.allocationRepository = allocationRepository;
        this.expenseTypeRepository = expenseTypeRepository;
        this.budgetYearRepository = budgetYearRepository;
    }

    // ---------- Reading ----------

    public List<ExpenseType> getExpenseTypes() {
        return expenseTypeRepository.findAllByOrderByNameAsc();
    }

    public List<Expense> getExpenses(Integer budgetYearId) {
        return expenseRepository.findByAllocation_BudgetYear_BudgetYearIdOrderByExpenseDateDescExpenseIdDesc(budgetYearId);
    }

    // ---------- Add ----------

    @Transactional
    public Expense addExpense(ExpenseRequest request) {
        Allocation allocation = findAllocation(request.getAllocationId());
        ExpenseType type = findType(request.getTypeId());
        BigDecimal available = allocation.getRemaining();

        // Factory decision: the form says which child class to create
        Expense expense = request.isRecurring() ? new RecurringExpense() : new NonRecurringExpense();
        fillAndValidate(expense, request, allocation, type, null);

        checkBalance(allocation, available, expense.getAmount());
        return expenseRepository.save(expense);
    }

    // ---------- Edit ----------

    @Transactional
    public Expense editExpense(Integer expenseId, ExpenseRequest request) {
        Expense old = findExpense(expenseId);
        Allocation oldAllocation = old.getAllocation();
        BigDecimal oldAmount = old.getAmount();

        Allocation newAllocation = findAllocation(request.getAllocationId());
        ExpenseType type = findType(request.getTypeId());

        // Work out the balance BEFORE changing anything.
        // If the category stays the same, the old amount is given back first,
        // so only the difference has to fit.
        BigDecimal available = newAllocation.getRemaining();
        if (newAllocation.getAllocationId().equals(oldAllocation.getAllocationId())) {
            available = available.add(oldAmount);
        }

        // Changing recurring <-> non-recurring means a different class,
        // so a new object replaces the old one.
        boolean kindChanged = request.isRecurring() != old.isRecurring();
        Expense target = old;
        if (kindChanged) {
            target = request.isRecurring() ? new RecurringExpense() : new NonRecurringExpense();
            target.setReceiptFile(old.getReceiptFile());
            target.setCreatedAt(old.getCreatedAt());
        }

        fillAndValidate(target, request, newAllocation, type, old.getExpenseId());
        checkBalance(newAllocation, available, target.getAmount());

        target.setUpdatedAt(LocalDateTime.now());
        if (kindChanged) {
            expenseRepository.delete(old);
        }
        return expenseRepository.save(target);
    }

    // ---------- Category suggestion ----------

    public List<Map<String, Object>> suggestCategories(Integer budgetYearId, Integer typeId, String amountText) {
        BudgetYear year = budgetYearRepository.findById(budgetYearId)
                .orElseThrow(() -> new InvalidInputException("Budget year not found"));
        ExpenseType type = findType(typeId);

        BigDecimal amount = BigDecimal.ZERO;
        if (amountText != null && amountText.trim().matches("\\d+(\\.\\d{1,2})?")) {
            amount = new BigDecimal(amountText.trim());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        boolean recommendedGiven = false;
        for (Allocation a : year.suggestCategories(type)) {   // already sorted: most money left first
            boolean fits = a.canAccommodate(amount);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("allocationId", a.getAllocationId());
            item.put("code", a.getCategory().getCode());
            item.put("name", a.getCategory().getName());
            item.put("remaining", a.getRemaining());
            item.put("fits", fits);
            item.put("recommended", fits && !recommendedGiven);
            if (fits) {
                recommendedGiven = true;
            }
            result.add(item);
        }
        return result;
    }

    // ---------- Receipt files ----------

    @Transactional
    public void saveReceipt(Integer expenseId, MultipartFile file) {
        Expense expense = findExpense(expenseId);
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("Choose a receipt file to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidInputException("The receipt file must be under 5 MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!FILE_TYPES.contains(extension)) {
            throw new InvalidInputException("Only PDF, JPG or PNG receipt files are allowed");
        }
        try (InputStream in = file.getInputStream()) {
            Path folder = Paths.get(uploadDir);
            Files.createDirectories(folder);
            // The file is named after the expense id, never after the uploaded name
            Path target = folder.resolve("expense-" + expenseId + "." + extension);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            expense.setReceiptFile(target.toString());
            expenseRepository.save(expense);
        } catch (IOException e) {
            throw new InvalidInputException("The receipt file could not be saved. Please try again.");
        }
    }

    public Path getReceiptPath(Integer expenseId) {
        Expense expense = findExpense(expenseId);
        if (expense.getReceiptFile() == null) {
            throw new InvalidInputException("No receipt file was uploaded for this expense");
        }
        Path path = Paths.get(expense.getReceiptFile());
        if (!Files.exists(path)) {
            throw new InvalidInputException("The receipt file is missing from the uploads folder");
        }
        return path;
    }

    // ---------- Validation ----------

    private void fillAndValidate(Expense expense, ExpenseRequest request, Allocation allocation,
                                 ExpenseType type, Integer excludeExpenseId) {
        BudgetYear year = allocation.getBudgetYear();
        if (!year.isFixed()) {
            throw new InvalidInputException("Fix the budget proposal before entering expenses");
        }
        if (!type.isEligible(allocation.getCategory())) {
            throw new InvalidInputException("A \"" + type.getName() + "\" cannot go under "
                    + allocation.getCategory().getLabel() + ". Pick one of the suggested categories.");
        }

        // Amount
        String amountText = request.getAmount() == null ? "" : request.getAmount().trim();
        if (amountText.isEmpty()) {
            throw new InvalidInputException("Amount is required");
        }
        if (!amountText.matches("\\d+(\\.\\d{1,2})?")) {
            throw new InvalidInputException("Amount must be a number like 2500 or 2500.50 (no letters or minus sign)");
        }
        BigDecimal amount = new BigDecimal(amountText);
        if (amount.signum() <= 0) {
            throw new InvalidInputException("Amount must be greater than zero");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidInputException("This amount looks too large, please check it");
        }

        // Date
        LocalDate date;
        try {
            date = LocalDate.parse(request.getExpenseDate() == null ? "" : request.getExpenseDate().trim());
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Enter a valid date");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidInputException("The date cannot be in the future");
        }
        if (!year.containsDate(date)) {
            throw new InvalidInputException("The date must be between " + year.getStartDate()
                    + " and " + year.getEndDate() + " (academic year " + year.getAcademicYear() + ")");
        }

        // Description
        String description = request.getDescription() == null ? "" : request.getDescription().trim();
        if (description.isEmpty()) {
            throw new InvalidInputException("Description is required");
        }
        if (!description.matches(".*[A-Za-z].*")) {
            throw new InvalidInputException("Description must contain words, not only numbers or symbols");
        }
        if (description.length() > 255) {
            throw new InvalidInputException("Keep the description under 255 characters");
        }

        // Receipt number
        String receipt = request.getReceiptNumber() == null ? "" : request.getReceiptNumber().trim();
        if (receipt.isEmpty()) {
            throw new InvalidInputException("Receipt number is required");
        }
        if (!receipt.matches("[A-Za-z0-9-]+")) {
            throw new InvalidInputException("Receipt number can only have letters, numbers and hyphens");
        }
        if (receipt.length() > 50) {
            throw new InvalidInputException("Receipt number is too long");
        }
        if (!request.isConfirmedDuplicate()) {
            for (Expense other : expenseRepository.findByReceiptNumber(receipt)) {
                if (excludeExpenseId == null || !other.getExpenseId().equals(excludeExpenseId)) {
                    throw new DuplicateReceiptException("Receipt " + receipt + " was already entered on "
                            + other.getExpenseDate() + " under " + other.getAllocation().getCategory().getLabel()
                            + " for Rs. " + other.getAmount().toPlainString());
                }
            }
        }

        // Frequency (only for recurring expenses)
        if (expense instanceof RecurringExpense) {
            String frequency = request.getFrequency() == null ? "" : request.getFrequency().trim().toUpperCase();
            if (!FREQUENCIES.contains(frequency)) {
                throw new InvalidInputException("Choose how often this expense repeats");
            }
            ((RecurringExpense) expense).setFrequency(frequency);
        }

        expense.setAllocation(allocation);
        expense.setExpenseType(type);
        expense.setAmount(amount);
        expense.setExpenseDate(date);
        expense.setDescription(description);
        expense.setReceiptNumber(receipt);
    }

    private void checkBalance(Allocation allocation, BigDecimal available, BigDecimal amount) {
        if (amount.compareTo(available) > 0) {
            throw new InvalidInputException("Not enough balance in " + allocation.getCategory().getLabel()
                    + " (Rs. " + available.toPlainString() + " left). Use a suggested category or move funds first.");
        }
    }

    private Allocation findAllocation(Integer allocationId) {
        if (allocationId == null) {
            throw new InvalidInputException("Choose a category");
        }
        return allocationRepository.findById(allocationId)
                .orElseThrow(() -> new InvalidInputException("Category not found"));
    }

    private ExpenseType findType(Integer typeId) {
        if (typeId == null) {
            throw new InvalidInputException("Choose an expense type");
        }
        return expenseTypeRepository.findById(typeId)
                .orElseThrow(() -> new InvalidInputException("Expense type not found"));
    }

    private Expense findExpense(Integer expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new InvalidInputException("Expense not found"));
    }
}
