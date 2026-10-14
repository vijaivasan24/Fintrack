package com.fintrack.service;

import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.CategoryType;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.User;
import com.fintrack.exception.InvalidTransactionException;
import com.fintrack.exception.TransactionNotFoundException;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User sampleUser;
    private Category expenseCategory;
    private Category incomeCategory;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Jane Doe", "jane@example.com", "hashedPassword");
        sampleUser.setId(1L);

        expenseCategory = new Category("Groceries", CategoryType.EXPENSE, sampleUser);
        expenseCategory.setId(10L);

        incomeCategory = new Category("Salary", CategoryType.INCOME, sampleUser);
        incomeCategory.setId(20L);

        sampleTransaction = new Transaction(
                new BigDecimal("50.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Weekly groceries",
                sampleUser,
                expenseCategory
        );
        sampleTransaction.setId(100L);
        sampleTransaction.setCreatedAt(LocalDateTime.of(2026, 3, 15, 10, 0));
    }

    @Test
    void shouldCreateTransactionSuccessfully() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Weekly groceries",
                10L
        );

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.createTransaction(request, "jane@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(new BigDecimal("50.00"), response.getAmount());
        assertEquals(CategoryType.EXPENSE, response.getType());
        assertEquals(LocalDate.of(2026, 3, 15), response.getDate());
        assertEquals("Weekly groceries", response.getDescription());
        assertEquals(10L, response.getCategoryId());
        assertEquals("Groceries", response.getCategoryName());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenCategoryDoesNotBelongToUser() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Weekly groceries",
                99L
        );

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class, () ->
                transactionService.createTransaction(request, "jane@example.com"));

        assertEquals("Category does not belong to user", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenCategoryTypeMismatchesTransactionType() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Weekly groceries",
                20L
        );

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(incomeCategory));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class, () ->
                transactionService.createTransaction(request, "jane@example.com"));

        assertEquals("Transaction type must match category type", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldGetTransactionsWithFiltersAndPaginationSortedByDateDesc() {
        Page<Transaction> page = new PageImpl<>(List.of(sampleTransaction));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);

        Page<TransactionResponse> result = transactionService.getTransactions(
                "jane@example.com",
                0,
                10,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                CategoryType.EXPENSE,
                10L
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Weekly groceries", result.getContent().get(0).getDescription());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(10, capturedPageable.getPageSize());
        Sort.Order dateOrder = capturedPageable.getSort().getOrderFor("date");
        assertNotNull(dateOrder);
        assertEquals(Sort.Direction.DESC, dateOrder.getDirection());
    }

    @Test
    void shouldGetTransactionByIdSuccessfully() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleTransaction));

        TransactionResponse response = transactionService.getTransactionById(100L, "jane@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(new BigDecimal("50.00"), response.getAmount());
    }

    @Test
    void shouldThrowWhenTransactionNotFoundById() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.getTransactionById(999L, "jane@example.com"));
    }

    @Test
    void shouldThrowWhenTransactionBelongsToAnotherUser() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.getTransactionById(200L, "jane@example.com"));
    }

    @Test
    void shouldUpdateTransactionSuccessfully() {
        TransactionRequest updateRequest = new TransactionRequest(
                new BigDecimal("75.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 16),
                "Monthly groceries",
                10L
        );

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleTransaction));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.updateTransaction(100L, updateRequest, "jane@example.com");

        assertNotNull(response);
        assertEquals(new BigDecimal("75.00"), sampleTransaction.getAmount());
        assertEquals("Monthly groceries", sampleTransaction.getDescription());
        assertEquals(LocalDate.of(2026, 3, 16), sampleTransaction.getDate());
        verify(transactionRepository).save(sampleTransaction);
    }

    @Test
    void shouldThrowWhenUpdatingTransactionOfAnotherUser() {
        TransactionRequest updateRequest = new TransactionRequest(
                new BigDecimal("75.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 16),
                "Monthly groceries",
                10L
        );

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.updateTransaction(200L, updateRequest, "jane@example.com"));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenUpdatingWithMismatchedCategoryType() {
        TransactionRequest updateRequest = new TransactionRequest(
                new BigDecimal("75.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 16),
                "Monthly groceries",
                20L
        );

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleTransaction));
        when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(incomeCategory));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class, () ->
                transactionService.updateTransaction(100L, updateRequest, "jane@example.com"));

        assertEquals("Transaction type must match category type", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldDeleteTransactionSuccessfully() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(sampleTransaction));

        transactionService.deleteTransaction(100L, "jane@example.com");

        verify(transactionRepository).delete(sampleTransaction);
    }

    @Test
    void shouldThrowWhenDeletingTransactionOfAnotherUser() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.deleteTransaction(200L, "jane@example.com"));

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
}
