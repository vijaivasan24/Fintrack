package com.fintrack.service;

import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.CategoryResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.User;
import com.fintrack.exception.CategoryInUseException;
import com.fintrack.exception.CategoryNotFoundException;
import com.fintrack.exception.DuplicateCategoryException;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        String name = request.getName().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(user.getId(), name, request.getType())) {
            throw new DuplicateCategoryException("Category already exists with name '" + name + "' and type '" + request.getType() + "'");
        }

        Category category = new Category(name, request.getType(), user);
        Category savedCategory = categoryRepository.save(category);

        return mapToResponse(savedCategory);
    }

    public List<CategoryResponse> getCategories(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Category> categories = categoryRepository.findByUserId(user.getId());

        return categories.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id " + id));

        return mapToResponse(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id " + id));

        String name = request.getName().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(user.getId(), name, request.getType(), id)) {
            throw new DuplicateCategoryException("Category already exists with name '" + name + "' and type '" + request.getType() + "'");
        }

        category.setName(name);
        category.setType(request.getType());
        Category updatedCategory = categoryRepository.save(category);

        return mapToResponse(updatedCategory);
    }

    public void deleteCategory(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id " + id));

        if (hasTransactions(id)) {
            throw new CategoryInUseException("Cannot delete category because it has associated transactions");
        }

        categoryRepository.delete(category);
    }

    protected boolean hasTransactions(Long categoryId) {
        return transactionRepository.existsByCategoryId(categoryId);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getType());
    }
}
