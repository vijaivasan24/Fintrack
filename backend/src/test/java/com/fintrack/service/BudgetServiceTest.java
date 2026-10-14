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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User sampleUser;
    private Category expenseCategory;
    private Category incomeCategory;
    private Budget sampleBudget;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Jane Doe", "jane@example.com", "hashedPassword");
        sampleUser.setId(1L);

        expenseCategory = new Category("Groceries", CategoryType.EXPENSE, sampleUser);
        expenseCategory.setId(10L);

        incomeCategory = new Category("Salary", CategoryType.INCOME, sampleUser);
        incomeCategory.setId(20L);

        sampleBudget = new Budget(
                sampleUser,
                expenseCategory,
                new BigDecimal("500.00"),
                2026,
                3
        );
        sampleBudget.setId(100L);
    }

    @Test
    void shouldCreateBudgetSuccessfully() {
        BudgetRequest request = new BudgetRequest(10L, new BigDecimal("500.00"), 2026, 3);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(1L, 10L, 2026, 3)).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);

        BudgetResponse response = budgetService.createBudget(request, "jane@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getCategoryId());
        assertEquals("Groceries", response.getCategoryName());
        assertEquals(new BigDecimal("500.00"), response.getLimitAmount());
        assertEquals(2026, response.getYear());
        assertEquals(3, response.getMonth());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCategoryDoesNotBelongToUser() {
        BudgetRequest request = new BudgetRequest(99L, new BigDecimal("500.00"), 2026, 3);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        InvalidBudgetException exception = assertThrows(InvalidBudgetException.class, () ->
                budgetService.createBudget(request, "jane@example.com"));

        assertEquals("Category does not belong to user", exception.getMessage());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCategoryIsNotExpense() {
        BudgetRequest request = new BudgetRequest(20L, new BigDecimal("500.00"), 2026, 3);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(incomeCategory));

        InvalidBudgetException exception = assertThrows(InvalidBudgetException.class, () ->
                budgetService.createBudget(request, "jane@example.com"));

        assertEquals("Budget category must be an EXPENSE category", exception.getMessage());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenBudgetAlreadyExistsForCategoryYearMonth() {
        BudgetRequest request = new BudgetRequest(10L, new BigDecimal("500.00"), 2026, 3);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(1L, 10L, 2026, 3)).thenReturn(true);

        DuplicateBudgetException exception = assertThrows(DuplicateBudgetException.class, () ->
                budgetService.createBudget(request, "jane@example.com"));

        assertEquals("Budget already exists for this category, year and month", exception.getMessage());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void shouldGetBudgetsWithOptionalFilters() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByUserIdAndFilters(1L, 2026, 3)).thenReturn(List.of(sampleBudget));

        List<BudgetResponse> responses = budgetService.getBudgets("jane@example.com", 2026, 3);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Groceries", responses.get(0).getCategoryName());
    }

    @Test
    void shouldGetBudgetByIdSuccessfully() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleBudget));

        BudgetResponse response = budgetService.getBudgetById(100L, "jane@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Groceries", response.getCategoryName());
    }

    @Test
    void shouldThrowWhenBudgetNotFoundById() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () ->
                budgetService.getBudgetById(999L, "jane@example.com"));
    }

    @Test
    void shouldThrowWhenBudgetBelongsToAnotherUser() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () ->
                budgetService.getBudgetById(200L, "jane@example.com"));
    }

    @Test
    void shouldUpdateBudgetSuccessfully() {
        BudgetRequest updateRequest = new BudgetRequest(10L, new BigDecimal("600.00"), 2026, 4);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleBudget));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(1L, 10L, 2026, 4)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);

        BudgetResponse response = budgetService.updateBudget(100L, updateRequest, "jane@example.com");

        assertNotNull(response);
        assertEquals(new BigDecimal("600.00"), sampleBudget.getLimitAmount());
        assertEquals(4, sampleBudget.getMonth());
        verify(budgetRepository).save(sampleBudget);
    }

    @Test
    void shouldAllowUpdatingBudgetWhenCategoryAndMonthUnchanged() {
        BudgetRequest updateRequest = new BudgetRequest(10L, new BigDecimal("700.00"), 2026, 3);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleBudget));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(1L, 10L, 2026, 3)).thenReturn(Optional.of(sampleBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);

        BudgetResponse response = budgetService.updateBudget(100L, updateRequest, "jane@example.com");

        assertNotNull(response);
        assertEquals(new BigDecimal("700.00"), sampleBudget.getLimitAmount());
    }

    @Test
    void shouldThrowWhenUpdatingBudgetCausesDuplicate() {
        Budget existingOtherBudget = new Budget(sampleUser, expenseCategory, new BigDecimal("400.00"), 2026, 4);
        existingOtherBudget.setId(101L);

        BudgetRequest updateRequest = new BudgetRequest(10L, new BigDecimal("600.00"), 2026, 4);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleBudget));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(1L, 10L, 2026, 4)).thenReturn(Optional.of(existingOtherBudget));

        assertThrows(DuplicateBudgetException.class, () ->
                budgetService.updateBudget(100L, updateRequest, "jane@example.com"));

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void shouldDeleteBudgetSuccessfully() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleBudget));

        budgetService.deleteBudget(100L, "jane@example.com");

        verify(budgetRepository).delete(sampleBudget);
    }

    @Test
    void shouldThrowWhenDeletingBudgetOfAnotherUser() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () ->
                budgetService.deleteBudget(200L, "jane@example.com"));

        verify(budgetRepository, never()).delete(any(Budget.class));
    }

    @Test
    void shouldGetBudgetStatusWithinLimit() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByUserIdAndYearAndMonth(1L, 2026, 3)).thenReturn(List.of(sampleBudget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                eq(1L), eq(10L), eq(CategoryType.EXPENSE), eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(new BigDecimal("350.00"));

        List<BudgetStatusResponse> statusList = budgetService.getBudgetStatus(2026, 3, "jane@example.com");

        assertNotNull(statusList);
        assertEquals(1, statusList.size());
        BudgetStatusResponse status = statusList.get(0);
        assertEquals("Groceries", status.getCategoryName());
        assertEquals(new BigDecimal("500.00"), status.getLimitAmount());
        assertEquals(new BigDecimal("350.00"), status.getAmountSpent());
        assertEquals(new BigDecimal("150.00"), status.getRemainingAmount());
        assertFalse(status.isExceeded());
    }

    @Test
    void shouldGetBudgetStatusExceeded() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByUserIdAndYearAndMonth(1L, 2026, 3)).thenReturn(List.of(sampleBudget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                eq(1L), eq(10L), eq(CategoryType.EXPENSE), eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(new BigDecimal("550.00"));

        List<BudgetStatusResponse> statusList = budgetService.getBudgetStatus(2026, 3, "jane@example.com");

        assertNotNull(statusList);
        assertEquals(1, statusList.size());
        BudgetStatusResponse status = statusList.get(0);
        assertEquals(new BigDecimal("550.00"), status.getAmountSpent());
        assertEquals(new BigDecimal("-50.00"), status.getRemainingAmount());
        assertTrue(status.isExceeded());
    }

    @Test
    void shouldGetBudgetStatusWithZeroExpensesWhenNoTransactions() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(budgetRepository.findByUserIdAndYearAndMonth(1L, 2026, 3)).thenReturn(List.of(sampleBudget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                eq(1L), eq(10L), eq(CategoryType.EXPENSE), eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(null);

        List<BudgetStatusResponse> statusList = budgetService.getBudgetStatus(2026, 3, "jane@example.com");

        assertNotNull(statusList);
        assertEquals(1, statusList.size());
        BudgetStatusResponse status = statusList.get(0);
        assertEquals(BigDecimal.ZERO, status.getAmountSpent());
        assertEquals(new BigDecimal("500.00"), status.getRemainingAmount());
        assertFalse(status.isExceeded());
    }
}
