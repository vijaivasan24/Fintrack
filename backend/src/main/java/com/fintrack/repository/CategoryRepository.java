package com.fintrack.repository;

import com.fintrack.entity.Category;
import com.fintrack.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCaseAndType(Long userId, String name, CategoryType type);

    boolean existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(Long userId, String name, CategoryType type, Long id);
}
