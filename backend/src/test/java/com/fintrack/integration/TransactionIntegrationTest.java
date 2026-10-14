package com.fintrack.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.TransactionRequest;
import com.fintrack.entity.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    void setUp() {
        testHelper.cleanDatabase();
    }

    @Test
    void shouldCreateTransactionSuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("45.50"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Weekly groceries",
                categoryId
        );

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(45.50))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.date").value("2026-03-15"))
                .andExpect(jsonPath("$.description").value("Weekly groceries"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Groceries"));
    }

    @Test
    void shouldFailValidationOnZeroNegativeAmountOrMissingDate() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        TransactionRequest zeroAmountRequest = new TransactionRequest(
                BigDecimal.ZERO,
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Zero amount",
                categoryId
        );
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zeroAmountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        TransactionRequest negativeAmountRequest = new TransactionRequest(
                new BigDecimal("-10.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Negative amount",
                categoryId
        );
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(negativeAmountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        TransactionRequest missingDateRequest = new TransactionRequest(
                new BigDecimal("15.00"),
                CategoryType.EXPENSE,
                null,
                "Missing date",
                categoryId
        );
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingDateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.date").exists());
    }

    @Test
    void shouldReturnBadRequestWhenCategoryTypeMismatchesTransactionType() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long incomeCategoryId = createCategory(token, "Salary", CategoryType.INCOME);

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("2000.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Mismatched type",
                incomeCategoryId
        );

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction type must match category type"));
    }

    @Test
    void shouldReturnBadRequestWhenUsingAnotherUsersCategory() throws Exception {
        String user1Token = testHelper.registerAndLogin("User One", "user1@example.com", "password123");
        Long user1CategoryId = createCategory(user1Token, "User1 Category", CategoryType.EXPENSE);

        String user2Token = testHelper.registerAndLogin("User Two", "user2@example.com", "password123");

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("25.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15),
                "Using other's category",
                user1CategoryId
        );

        mockMvc.perform(post("/api/transactions")
                .header("Authorization", user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category does not belong to user"));
    }

    @Test
    void shouldReturnNotFoundWhenAccessingAnotherUsersTransaction() throws Exception {
        String user1Token = testHelper.registerAndLogin("User One", "user1@example.com", "password123");
        Long categoryId1 = createCategory(user1Token, "Personal", CategoryType.EXPENSE);
        Long transactionId = createTransaction(user1Token, new BigDecimal("30.00"), CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 15), "Private", categoryId1);

        String user2Token = testHelper.registerAndLogin("User Two", "user2@example.com", "password123");
        Long categoryId2 = createCategory(user2Token, "Personal Two", CategoryType.EXPENSE);

        mockMvc.perform(get("/api/transactions/" + transactionId)
                .header("Authorization", user2Token))
                .andExpect(status().isNotFound());

        TransactionRequest updateRequest = new TransactionRequest(
                new BigDecimal("50.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 16),
                "Attempted update",
                categoryId2
        );
        mockMvc.perform(put("/api/transactions/" + transactionId)
                .header("Authorization", user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/transactions/" + transactionId)
                .header("Authorization", user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPaginateAndSortByDateDescending() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        createTransaction(token, new BigDecimal("10.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 10), "T1", categoryId);
        createTransaction(token, new BigDecimal("25.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 25), "T2", categoryId);
        createTransaction(token, new BigDecimal("18.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 18), "T3", categoryId);

        mockMvc.perform(get("/api/transactions?page=0&size=2")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].date").value("2026-03-25"))
                .andExpect(jsonPath("$.content[0].description").value("T2"))
                .andExpect(jsonPath("$.content[1].date").value("2026-03-18"))
                .andExpect(jsonPath("$.content[1].description").value("T3"));

        mockMvc.perform(get("/api/transactions?page=1&size=2")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].date").value("2026-03-10"))
                .andExpect(jsonPath("$.content[0].description").value("T1"));
    }

    @Test
    void shouldFilterTransactionsByDateRangeTypeAndCategoryId() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long expenseCatId = createCategory(token, "Food", CategoryType.EXPENSE);
        Long incomeCatId = createCategory(token, "Salary", CategoryType.INCOME);

        createTransaction(token, new BigDecimal("50.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 5), "T1", expenseCatId);
        createTransaction(token, new BigDecimal("70.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 15), "T2", expenseCatId);
        createTransaction(token, new BigDecimal("2000.00"), CategoryType.INCOME, LocalDate.of(2026, 3, 15), "T3", incomeCatId);
        createTransaction(token, new BigDecimal("90.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 25), "T4", expenseCatId);

        mockMvc.perform(get("/api/transactions?from=2026-03-10&to=2026-03-20")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/transactions?type=INCOME")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("T3"));

        mockMvc.perform(get("/api/transactions?categoryId=" + expenseCatId)
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/transactions?type=EXPENSE&categoryId=" + expenseCatId + "&from=2026-03-10&to=2026-03-20")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("T2"));
    }

    @Test
    void shouldUpdateTransactionSuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId1 = createCategory(token, "Groceries", CategoryType.EXPENSE);
        Long categoryId2 = createCategory(token, "Dining", CategoryType.EXPENSE);

        Long transactionId = createTransaction(token, new BigDecimal("40.00"), CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 12), "Dinner", categoryId1);

        TransactionRequest updateRequest = new TransactionRequest(
                new BigDecimal("60.00"),
                CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 13),
                "Dinner & Dessert",
                categoryId2
        );

        mockMvc.perform(put("/api/transactions/" + transactionId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.amount").value(60.00))
                .andExpect(jsonPath("$.date").value("2026-03-13"))
                .andExpect(jsonPath("$.description").value("Dinner & Dessert"))
                .andExpect(jsonPath("$.categoryId").value(categoryId2))
                .andExpect(jsonPath("$.categoryName").value("Dining"));
    }

    @Test
    void shouldDeleteTransactionSuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);
        Long transactionId = createTransaction(token, new BigDecimal("20.00"), CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 14), "Snacks", categoryId);

        mockMvc.perform(delete("/api/transactions/" + transactionId)
                .header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/" + transactionId)
                .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenDeletingCategoryWithTransactions() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Subscriptions", CategoryType.EXPENSE);

        createTransaction(token, new BigDecimal("12.99"), CategoryType.EXPENSE,
                LocalDate.of(2026, 3, 1), "Streaming", categoryId);

        mockMvc.perform(delete("/api/categories/" + categoryId)
                .header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete category because it has associated transactions"));

        mockMvc.perform(get("/api/categories/" + categoryId)
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Subscriptions"));
    }

    private Long createCategory(String token, String name, CategoryType type) throws Exception {
        CategoryRequest request = new CategoryRequest(name, type);
        MvcResult result = mockMvc.perform(post("/api/categories")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    private Long createTransaction(String token, BigDecimal amount, CategoryType type,
                                  LocalDate date, String description, Long categoryId) throws Exception {
        TransactionRequest request = new TransactionRequest(amount, type, date, description, categoryId);
        MvcResult result = mockMvc.perform(post("/api/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }
}
