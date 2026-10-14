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
import com.fintrack.repository.TransactionSpecification;
import com.fintrack.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public TransactionResponse createTransaction(TransactionRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Category category = getCategoryForUser(request.getCategoryId(), user.getId());

        if (category.getType() != request.getType()) {
            throw new InvalidTransactionException("Transaction type must match category type");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getType(),
                request.getDate(),
                description,
                user,
                category
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    public Page<TransactionResponse> getTransactions(String userEmail, int page, int size,
                                                     LocalDate from, LocalDate to,
                                                     CategoryType type, Long categoryId) {
        User user = getUserByEmail(userEmail);

        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));
        Specification<Transaction> spec = TransactionSpecification.filter(user.getId(), from, to, type, categoryId);

        return transactionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public TransactionResponse getTransactionById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id " + id));

        return mapToResponse(transaction);
    }

    public TransactionResponse updateTransaction(Long id, TransactionRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id " + id));

        Category category = getCategoryForUser(request.getCategoryId(), user.getId());

        if (category.getType() != request.getType()) {
            throw new InvalidTransactionException("Transaction type must match category type");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDate(request.getDate());
        transaction.setDescription(description);
        transaction.setCategory(category);

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return mapToResponse(updatedTransaction);
    }

    public void deleteTransaction(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id " + id));

        transactionRepository.delete(transaction);
    }

    private Category getCategoryForUser(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new InvalidTransactionException("Category does not belong to user"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDate(),
                transaction.getDescription(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getCreatedAt()
        );
    }
}
