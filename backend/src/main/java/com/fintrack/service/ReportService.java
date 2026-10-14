package com.fintrack.service;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.SummaryReportResponse;
import com.fintrack.entity.CategoryType;
import com.fintrack.entity.User;
import com.fintrack.exception.InvalidReportException;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ReportService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public SummaryReportResponse getSummary(LocalDate from, LocalDate to, String userEmail) {
        validateDateRange(from, to);
        User user = getUserByEmail(userEmail);

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), CategoryType.INCOME, from, to);
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }

        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                user.getId(), CategoryType.EXPENSE, from, to);
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpense);
        long count = transactionRepository.countByUserIdAndDateBetween(user.getId(), from, to);

        return new SummaryReportResponse(totalIncome, totalExpense, netBalance, count);
    }

    public List<CategoryReportResponse> getByCategory(LocalDate from, LocalDate to, CategoryType type, String userEmail) {
        validateDateRange(from, to);
        if (type == null) {
            throw new InvalidReportException("Category type is required");
        }

        User user = getUserByEmail(userEmail);
        List<Object[]> results = transactionRepository.sumAmountByCategoryAndTypeAndDateBetween(
                user.getId(), type, from, to);

        List<CategoryReportResponse> responses = new ArrayList<>();
        for (Object[] row : results) {
            Long categoryId = (Long) row[0];
            String categoryName = (String) row[1];
            BigDecimal totalAmount = (BigDecimal) row[2];
            responses.add(new CategoryReportResponse(categoryId, categoryName, totalAmount));
        }

        return responses;
    }

    public List<MonthlyReportResponse> getMonthly(Integer year, String userEmail) {
        if (year == null) {
            throw new InvalidReportException("Year is required");
        }

        User user = getUserByEmail(userEmail);
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        BigDecimal[] incomeByMonth = new BigDecimal[13];
        BigDecimal[] expenseByMonth = new BigDecimal[13];
        Arrays.fill(incomeByMonth, BigDecimal.ZERO);
        Arrays.fill(expenseByMonth, BigDecimal.ZERO);

        List<Object[]> results = transactionRepository.sumAmountByMonthAndTypeForYear(
                user.getId(), startDate, endDate);

        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue();
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            if (type == CategoryType.INCOME) {
                incomeByMonth[month] = amount != null ? amount : BigDecimal.ZERO;
            } else if (type == CategoryType.EXPENSE) {
                expenseByMonth[month] = amount != null ? amount : BigDecimal.ZERO;
            }
        }

        List<MonthlyReportResponse> responses = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal income = incomeByMonth[m];
            BigDecimal expense = expenseByMonth[m];
            BigDecimal net = income.subtract(expense);
            responses.add(new MonthlyReportResponse(m, income, expense, net));
        }

        return responses;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidReportException("From and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new InvalidReportException("From date cannot be after to date");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
