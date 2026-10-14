package com.fintrack.repository;

import com.fintrack.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(Long userId, Long categoryId, Integer year, Integer month);

    boolean existsByUserIdAndCategoryIdAndYearAndMonth(Long userId, Long categoryId, Integer year, Integer month);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
           "AND (:year IS NULL OR b.year = :year) " +
           "AND (:month IS NULL OR b.month = :month)")
    List<Budget> findByUserIdAndFilters(@Param("userId") Long userId,
                                       @Param("year") Integer year,
                                       @Param("month") Integer month);

    List<Budget> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

    boolean existsByCategoryId(Long categoryId);
}
