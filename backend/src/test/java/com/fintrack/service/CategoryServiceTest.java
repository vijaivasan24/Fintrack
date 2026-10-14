package com.fintrack.service;

import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.CategoryResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.CategoryType;
import com.fintrack.entity.User;
import com.fintrack.exception.CategoryInUseException;
import com.fintrack.exception.CategoryNotFoundException;
import com.fintrack.exception.DuplicateCategoryException;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User sampleUser;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Jane Doe", "jane@example.com", "hashedPassword");
        sampleUser.setId(1L);

        sampleCategory = new Category("Groceries", CategoryType.EXPENSE, sampleUser);
        sampleCategory.setId(10L);
    }

    @Test
    void shouldCreateCategorySuccessfully() {
        CategoryRequest request = new CategoryRequest("Groceries", CategoryType.EXPENSE);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(1L, "Groceries", CategoryType.EXPENSE)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        CategoryResponse response = categoryService.createCategory(request, "jane@example.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Groceries", response.getName());
        assertEquals(CategoryType.EXPENSE, response.getType());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldThrowWhenCreatingDuplicateCategory() {
        CategoryRequest request = new CategoryRequest("Groceries", CategoryType.EXPENSE);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(1L, "Groceries", CategoryType.EXPENSE)).thenReturn(true);

        assertThrows(DuplicateCategoryException.class, () ->
                categoryService.createCategory(request, "jane@example.com"));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldGetAllCategoriesForUser() {
        Category secondCategory = new Category("Salary", CategoryType.INCOME, sampleUser);
        secondCategory.setId(11L);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(sampleCategory, secondCategory));

        List<CategoryResponse> categories = categoryService.getCategories("jane@example.com");

        assertEquals(2, categories.size());
        assertEquals("Groceries", categories.get(0).getName());
        assertEquals("Salary", categories.get(1).getName());
    }

    @Test
    void shouldGetCategoryByIdSuccessfully() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleCategory));

        CategoryResponse response = categoryService.getCategoryById(10L, "jane@example.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Groceries", response.getName());
    }

    @Test
    void shouldThrowWhenCategoryNotFoundById() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () ->
                categoryService.getCategoryById(99L, "jane@example.com"));
    }

    @Test
    void shouldUpdateCategorySuccessfully() {
        CategoryRequest updateRequest = new CategoryRequest("Supermarket", CategoryType.EXPENSE);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(1L, "Supermarket", CategoryType.EXPENSE, 10L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        CategoryResponse response = categoryService.updateCategory(10L, updateRequest, "jane@example.com");

        assertNotNull(response);
        assertEquals("Supermarket", sampleCategory.getName());
        verify(categoryRepository).save(sampleCategory);
    }

    @Test
    void shouldThrowWhenUpdatingToDuplicateCategory() {
        CategoryRequest updateRequest = new CategoryRequest("Utilities", CategoryType.EXPENSE);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(1L, "Utilities", CategoryType.EXPENSE, 10L)).thenReturn(true);

        assertThrows(DuplicateCategoryException.class, () ->
                categoryService.updateCategory(10L, updateRequest, "jane@example.com"));
    }

    @Test
    void shouldDeleteCategorySuccessfully() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleCategory));
        when(transactionRepository.existsByCategoryId(10L)).thenReturn(false);

        categoryService.deleteCategory(10L, "jane@example.com");

        verify(categoryRepository).delete(sampleCategory);
    }

    @Test
    void shouldThrowConflictWhenDeletingCategoryWithTransactions() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleCategory));
        when(transactionRepository.existsByCategoryId(10L)).thenReturn(true);

        assertThrows(CategoryInUseException.class, () ->
                categoryService.deleteCategory(10L, "jane@example.com"));

        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
