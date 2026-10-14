package com.fintrack.service;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.SummaryReportResponse;
import com.fintrack.entity.CategoryType;
import com.fintrack.entity.User;
import com.fintrack.exception.InvalidReportException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Jane Doe", "jane@example.com", "hashedPassword");
        sampleUser.setId(1L);
    }

    @Test
    void shouldGetSummarySuccessfully() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(1L, CategoryType.INCOME, from, to))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(1L, CategoryType.EXPENSE, from, to))
                .thenReturn(new BigDecimal("1500.00"));
        when(transactionRepository.countByUserIdAndDateBetween(1L, from, to))
                .thenReturn(15L);

        SummaryReportResponse response = reportService.getSummary(from, to, "jane@example.com");

        assertNotNull(response);
        assertEquals(new BigDecimal("5000.00"), response.getTotalIncome());
        assertEquals(new BigDecimal("1500.00"), response.getTotalExpense());
        assertEquals(new BigDecimal("3500.00"), response.getNetBalance());
        assertEquals(15L, response.getTransactionCount());
    }

    @Test
    void shouldGetSummaryWithZeroTotalsWhenNoTransactions() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(1L, CategoryType.INCOME, from, to))
                .thenReturn(null);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(1L, CategoryType.EXPENSE, from, to))
                .thenReturn(null);
        when(transactionRepository.countByUserIdAndDateBetween(1L, from, to))
                .thenReturn(0L);

        SummaryReportResponse response = reportService.getSummary(from, to, "jane@example.com");

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getTotalIncome());
        assertEquals(BigDecimal.ZERO, response.getTotalExpense());
        assertEquals(BigDecimal.ZERO, response.getNetBalance());
        assertEquals(0L, response.getTransactionCount());
    }

    @Test
    void shouldThrowWhenSummaryFromDateIsAfterToDate() {
        LocalDate from = LocalDate.of(2026, 3, 31);
        LocalDate to = LocalDate.of(2026, 1, 1);

        InvalidReportException exception = assertThrows(InvalidReportException.class, () ->
                reportService.getSummary(from, to, "jane@example.com"));

        assertEquals("From date cannot be after to date", exception.getMessage());
    }

    @Test
    void shouldThrowWhenSummaryDateIsNull() {
        assertThrows(InvalidReportException.class, () ->
                reportService.getSummary(null, LocalDate.of(2026, 1, 1), "jane@example.com"));

        assertThrows(InvalidReportException.class, () ->
                reportService.getSummary(LocalDate.of(2026, 1, 1), null, "jane@example.com"));
    }

    @Test
    void shouldGetByCategorySuccessfully() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{10L, "Groceries", new BigDecimal("450.00")});
        rows.add(new Object[]{11L, "Utilities", new BigDecimal("150.00")});

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.sumAmountByCategoryAndTypeAndDateBetween(1L, CategoryType.EXPENSE, from, to))
                .thenReturn(rows);

        List<CategoryReportResponse> responses = reportService.getByCategory(from, to, CategoryType.EXPENSE, "jane@example.com");

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Groceries", responses.get(0).getCategoryName());
        assertEquals(new BigDecimal("450.00"), responses.get(0).getTotalAmount());
        assertEquals("Utilities", responses.get(1).getCategoryName());
        assertEquals(new BigDecimal("150.00"), responses.get(1).getTotalAmount());
    }

    @Test
    void shouldThrowWhenByCategoryFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 3, 31);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThrows(InvalidReportException.class, () ->
                reportService.getByCategory(from, to, CategoryType.EXPENSE, "jane@example.com"));
    }

    @Test
    void shouldThrowWhenByCategoryTypeIsNull() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        assertThrows(InvalidReportException.class, () ->
                reportService.getByCategory(from, to, null, "jane@example.com"));
    }

    @Test
    void shouldGetMonthlyReportWithAll12MonthsAndZerosWhereNoData() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1, CategoryType.INCOME, new BigDecimal("3000.00")});
        rows.add(new Object[]{1, CategoryType.EXPENSE, new BigDecimal("800.00")});
        rows.add(new Object[]{3, CategoryType.EXPENSE, new BigDecimal("500.00")});

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.sumAmountByMonthAndTypeForYear(1L, startDate, endDate))
                .thenReturn(rows);

        List<MonthlyReportResponse> responses = reportService.getMonthly(2026, "jane@example.com");

        assertNotNull(responses);
        assertEquals(12, responses.size());

        // Month 1 has income and expense
        assertEquals(1, responses.get(0).getMonth());
        assertEquals(new BigDecimal("3000.00"), responses.get(0).getTotalIncome());
        assertEquals(new BigDecimal("800.00"), responses.get(0).getTotalExpense());
        assertEquals(new BigDecimal("2200.00"), responses.get(0).getNetBalance());

        // Month 2 has no data -> should be 0
        assertEquals(2, responses.get(1).getMonth());
        assertEquals(BigDecimal.ZERO, responses.get(1).getTotalIncome());
        assertEquals(BigDecimal.ZERO, responses.get(1).getTotalExpense());
        assertEquals(BigDecimal.ZERO, responses.get(1).getNetBalance());

        // Month 3 has only expense
        assertEquals(3, responses.get(2).getMonth());
        assertEquals(BigDecimal.ZERO, responses.get(2).getTotalIncome());
        assertEquals(new BigDecimal("500.00"), responses.get(2).getTotalExpense());
        assertEquals(new BigDecimal("-500.00"), responses.get(2).getNetBalance());

        // Month 12 has no data -> should be 0
        assertEquals(12, responses.get(11).getMonth());
        assertEquals(BigDecimal.ZERO, responses.get(11).getTotalIncome());
        assertEquals(BigDecimal.ZERO, responses.get(11).getTotalExpense());
        assertEquals(BigDecimal.ZERO, responses.get(11).getNetBalance());
    }

    @Test
    void shouldThrowWhenMonthlyYearIsNull() {
        assertThrows(InvalidReportException.class, () ->
                reportService.getMonthly(null, "jane@example.com"));
    }
}
