package com.fintrack.service;

import com.fintrack.dto.BudgetRequest;
import com.fintrack.dto.BudgetResponse;
import com.fintrack.dto.BudgetStatusResponse;
import com.fintrack.entity.Budget;
import com.fintrack.entity.Category;
import com.fintrack.entity.CategoryType;
import com.fintrack.entity.User;
import com.fintrack.exception.BudgetNotFoundException;
import com.fintrack.exception.DuplicateBudgetException;
import com.fintrack.exception.InvalidBudgetException;
import com.fintrack.repository.BudgetRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public BudgetResponse createBudget(BudgetRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Category category = getExpenseCategoryForUser(request.getCategoryId(), user.getId());

        if (budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(
                user.getId(), category.getId(), request.getYear(), request.getMonth())) {
            throw new DuplicateBudgetException("Budget already exists for this category, year and month");
        }

        Budget budget = new Budget(
                user,
                category,
                request.getLimitAmount(),
                request.getYear(),
                request.getMonth()
        );

        Budget savedBudget = budgetRepository.save(budget);
        return mapToResponse(savedBudget);
    }

    public List<BudgetResponse> getBudgets(String userEmail, Integer year, Integer month) {
        User user = getUserByEmail(userEmail);
        List<Budget> budgets = budgetRepository.findByUserIdAndFilters(user.getId(), year, month);

        List<BudgetResponse> responses = new ArrayList<>();
        for (Budget budget : budgets) {
            responses.add(mapToResponse(budget));
        }
        return responses;
    }

    public BudgetResponse getBudgetById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Budget budget = getBudgetForUser(id, user.getId());
        return mapToResponse(budget);
    }

    public BudgetResponse updateBudget(Long id, BudgetRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Budget budget = getBudgetForUser(id, user.getId());
        Category category = getExpenseCategoryForUser(request.getCategoryId(), user.getId());

        Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(
                user.getId(), category.getId(), request.getYear(), request.getMonth());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new DuplicateBudgetException("Budget already exists for this category, year and month");
        }

        budget.setCategory(category);
        budget.setLimitAmount(request.getLimitAmount());
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());

        Budget updatedBudget = budgetRepository.save(budget);
        return mapToResponse(updatedBudget);
    }

    public void deleteBudget(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Budget budget = getBudgetForUser(id, user.getId());
        budgetRepository.delete(budget);
    }

    public List<BudgetStatusResponse> getBudgetStatus(Integer year, Integer month, String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Budget> budgets = budgetRepository.findByUserIdAndYearAndMonth(user.getId(), year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        List<BudgetStatusResponse> statusList = new ArrayList<>();
        for (Budget budget : budgets) {
            BigDecimal spent = transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                    user.getId(), budget.getCategory().getId(), CategoryType.EXPENSE, startDate, endDate);

            if (spent == null) {
                spent = BigDecimal.ZERO;
            }

            BigDecimal remaining = budget.getLimitAmount().subtract(spent);
            boolean exceeded = spent.compareTo(budget.getLimitAmount()) > 0;

            statusList.add(new BudgetStatusResponse(
                    budget.getId(),
                    budget.getCategory().getId(),
                    budget.getCategory().getName(),
                    budget.getLimitAmount(),
                    spent,
                    remaining,
                    exceeded
            ));
        }

        return statusList;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Category getExpenseCategoryForUser(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new InvalidBudgetException("Category does not belong to user"));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new InvalidBudgetException("Budget category must be an EXPENSE category");
        }

        return category;
    }

    private Budget getBudgetForUser(Long budgetId, Long userId) {
        return budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new BudgetNotFoundException("Budget not found"));
    }

    private BudgetResponse mapToResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getLimitAmount(),
                budget.getYear(),
                budget.getMonth()
        );
    }
}
