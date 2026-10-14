package com.fintrack.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.dto.BudgetRequest;
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
class BudgetIntegrationTest {

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
    void shouldRejectBudgetRequestsWithoutToken() throws Exception {
        BudgetRequest request = new BudgetRequest(1L, new BigDecimal("100.00"), 2026, 3);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/budgets/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/budgets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/budgets/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/budgets/status?year=2026&month=3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateBudgetSuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        BudgetRequest request = new BudgetRequest(categoryId, new BigDecimal("450.00"), 2026, 3);

        mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.limitAmount").value(450.00))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(3));
    }

    @Test
    void shouldFailValidationOnZeroNegativeLimitOrInvalidMonth() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        BudgetRequest zeroLimitRequest = new BudgetRequest(categoryId, BigDecimal.ZERO, 2026, 3);
        mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zeroLimitRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limitAmount").exists());

        BudgetRequest negativeLimitRequest = new BudgetRequest(categoryId, new BigDecimal("-50.00"), 2026, 3);
        mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(negativeLimitRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limitAmount").exists());

        BudgetRequest invalidMonthRequest = new BudgetRequest(categoryId, new BigDecimal("100.00"), 2026, 13);
        mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMonthRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.month").exists());
    }

    @Test
    void shouldReturnConflictWhenCreatingDuplicateBudget() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        BudgetRequest request = new BudgetRequest(categoryId, new BigDecimal("300.00"), 2026, 3);

        mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Budget already exists for this category, year and month"));
    }

    @Test
    void shouldReturnBadRequestWhenCategoryIsNotExpense() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long incomeCatId = createCategory(token, "Salary", CategoryType.INCOME);

        BudgetRequest request = new BudgetRequest(incomeCatId, new BigDecimal("1000.00"), 2026, 3);

        mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Budget category must be an EXPENSE category"));
    }

    @Test
    void shouldReturnBadRequestWhenUsingAnotherUsersCategory() throws Exception {
        String user1Token = testHelper.registerAndLogin("User One", "user1@example.com", "password123");
        Long user1CategoryId = createCategory(user1Token, "User1 Cat", CategoryType.EXPENSE);

        String user2Token = testHelper.registerAndLogin("User Two", "user2@example.com", "password123");

        BudgetRequest request = new BudgetRequest(user1CategoryId, new BigDecimal("200.00"), 2026, 3);

        mockMvc.perform(post("/api/budgets")
                .header("Authorization", user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category does not belong to user"));
    }

    @Test
    void shouldReturnNotFoundWhenAccessingAnotherUsersBudget() throws Exception {
        String user1Token = testHelper.registerAndLogin("User One", "user1@example.com", "password123");
        Long cat1Id = createCategory(user1Token, "Dining", CategoryType.EXPENSE);
        Long budgetId = createBudget(user1Token, cat1Id, new BigDecimal("150.00"), 2026, 3);

        String user2Token = testHelper.registerAndLogin("User Two", "user2@example.com", "password123");
        Long cat2Id = createCategory(user2Token, "Personal", CategoryType.EXPENSE);

        mockMvc.perform(get("/api/budgets/" + budgetId)
                .header("Authorization", user2Token))
                .andExpect(status().isNotFound());

        BudgetRequest updateRequest = new BudgetRequest(cat2Id, new BigDecimal("200.00"), 2026, 3);
        mockMvc.perform(put("/api/budgets/" + budgetId)
                .header("Authorization", user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/budgets/" + budgetId)
                .header("Authorization", user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListAndFilterBudgets() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long cat1 = createCategory(token, "Groceries", CategoryType.EXPENSE);
        Long cat2 = createCategory(token, "Utilities", CategoryType.EXPENSE);

        createBudget(token, cat1, new BigDecimal("300.00"), 2026, 3);
        createBudget(token, cat2, new BigDecimal("150.00"), 2026, 3);
        createBudget(token, cat1, new BigDecimal("320.00"), 2026, 4);

        mockMvc.perform(get("/api/budgets")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/budgets?year=2026&month=3")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/budgets?year=2026&month=4")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].limitAmount").value(320.00));
    }

    @Test
    void shouldUpdateBudgetSuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long catId = createCategory(token, "Groceries", CategoryType.EXPENSE);
        Long budgetId = createBudget(token, catId, new BigDecimal("300.00"), 2026, 3);

        BudgetRequest updateRequest = new BudgetRequest(catId, new BigDecimal("400.00"), 2026, 3);

        mockMvc.perform(put("/api/budgets/" + budgetId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId))
                .andExpect(jsonPath("$.limitAmount").value(400.00));
    }

    @Test
    void shouldDeleteBudgetSuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long catId = createCategory(token, "Groceries", CategoryType.EXPENSE);
        Long budgetId = createBudget(token, catId, new BigDecimal("300.00"), 2026, 3);

        mockMvc.perform(delete("/api/budgets/" + budgetId)
                .header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budgets/" + budgetId)
                .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCalculateBudgetStatusAccurately() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long groceriesCatId = createCategory(token, "Groceries", CategoryType.EXPENSE);
        Long utilitiesCatId = createCategory(token, "Utilities", CategoryType.EXPENSE);

        createBudget(token, groceriesCatId, new BigDecimal("200.00"), 2026, 3);
        createBudget(token, utilitiesCatId, new BigDecimal("100.00"), 2026, 3);

        createTransaction(token, new BigDecimal("80.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 5), groceriesCatId);
        createTransaction(token, new BigDecimal("70.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 15), groceriesCatId);
        createTransaction(token, new BigDecimal("120.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 10), utilitiesCatId);
        createTransaction(token, new BigDecimal("50.00"), CategoryType.EXPENSE, LocalDate.of(2026, 4, 1), groceriesCatId);

        mockMvc.perform(get("/api/budgets/status?year=2026&month=3")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.categoryName == 'Groceries')].limitAmount").value(200.0))
                .andExpect(jsonPath("$[?(@.categoryName == 'Groceries')].amountSpent").value(150.0))
                .andExpect(jsonPath("$[?(@.categoryName == 'Groceries')].remainingAmount").value(50.0))
                .andExpect(jsonPath("$[?(@.categoryName == 'Groceries')].exceeded").value(false))
                .andExpect(jsonPath("$[?(@.categoryName == 'Utilities')].limitAmount").value(100.0))
                .andExpect(jsonPath("$[?(@.categoryName == 'Utilities')].amountSpent").value(120.0))
                .andExpect(jsonPath("$[?(@.categoryName == 'Utilities')].remainingAmount").value(-20.0))
                .andExpect(jsonPath("$[?(@.categoryName == 'Utilities')].exceeded").value(true));
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

    private Long createBudget(String token, Long categoryId, BigDecimal limitAmount, int year, int month) throws Exception {
        BudgetRequest request = new BudgetRequest(categoryId, limitAmount, year, month);
        MvcResult result = mockMvc.perform(post("/api/budgets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    private void createTransaction(String token, BigDecimal amount, CategoryType type,
                                   LocalDate date, Long categoryId) throws Exception {
        TransactionRequest request = new TransactionRequest(amount, type, date, "test", categoryId);
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
