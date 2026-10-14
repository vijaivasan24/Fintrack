package com.fintrack.repository;

import com.fintrack.entity.CategoryType;
import com.fintrack.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.category.id = :categoryId " +
           "AND t.type = :type " +
           "AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.type = :type " +
           "AND t.date BETWEEN :from AND :to")
    BigDecimal sumAmountByUserIdAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") CategoryType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.date BETWEEN :from AND :to")
    long countByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT t.category.id, t.category.name, SUM(t.amount) " +
           "FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.type = :type " +
           "AND t.date BETWEEN :from AND :to " +
           "GROUP BY t.category.id, t.category.name " +
           "ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumAmountByCategoryAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") CategoryType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT MONTH(t.date), t.type, SUM(t.amount) " +
           "FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY MONTH(t.date), t.type")
    List<Object[]> sumAmountByMonthAndTypeForYear(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
