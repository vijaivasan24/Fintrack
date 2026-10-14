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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
class ReportIntegrationTest {

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
    void shouldRejectReportRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/reports/summary?from=2026-01-01&to=2026-03-31"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/by-category?from=2026-01-01&to=2026-03-31&type=EXPENSE"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/monthly?year=2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnSummaryReportAccurately() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long incomeCat = createCategory(token, "Salary", CategoryType.INCOME);
        Long bonusCat = createCategory(token, "Bonus", CategoryType.INCOME);
        Long expenseCat = createCategory(token, "Groceries", CategoryType.EXPENSE);

        createTransaction(token, new BigDecimal("3000.00"), CategoryType.INCOME, LocalDate.of(2026, 1, 15), incomeCat);
        createTransaction(token, new BigDecimal("500.00"), CategoryType.INCOME, LocalDate.of(2026, 2, 20), bonusCat);
        createTransaction(token, new BigDecimal("250.00"), CategoryType.EXPENSE, LocalDate.of(2026, 1, 20), expenseCat);
        createTransaction(token, new BigDecimal("150.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 10), expenseCat);
        // Out of date range
        createTransaction(token, new BigDecimal("2000.00"), CategoryType.INCOME, LocalDate.of(2026, 5, 1), incomeCat);

        mockMvc.perform(get("/api/reports/summary?from=2026-01-01&to=2026-03-31")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(3500.00))
                .andExpect(jsonPath("$.totalExpense").value(400.00))
                .andExpect(jsonPath("$.netBalance").value(3100.00))
                .andExpect(jsonPath("$.transactionCount").value(4));
    }

    @Test
    void shouldReturnZeroSummaryWhenNoTransactionsInRange() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(get("/api/reports/summary?from=2026-01-01&to=2026-03-31")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.netBalance").value(0))
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    @Test
    void shouldRejectSummaryWhenFromIsAfterToWith400() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(get("/api/reports/summary?from=2026-03-31&to=2026-01-01")
                .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("From date cannot be after to date"));
    }

    @Test
    void shouldReturnByCategoryReportAccurately() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long groceriesCat = createCategory(token, "Groceries", CategoryType.EXPENSE);
        Long diningCat = createCategory(token, "Dining", CategoryType.EXPENSE);

        createTransaction(token, new BigDecimal("120.00"), CategoryType.EXPENSE, LocalDate.of(2026, 1, 10), groceriesCat);
        createTransaction(token, new BigDecimal("80.00"), CategoryType.EXPENSE, LocalDate.of(2026, 2, 10), groceriesCat);
        createTransaction(token, new BigDecimal("65.00"), CategoryType.EXPENSE, LocalDate.of(2026, 2, 14), diningCat);

        mockMvc.perform(get("/api/reports/by-category?from=2026-01-01&to=2026-03-31&type=EXPENSE")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.categoryName == 'Groceries')].totalAmount").value(200.00))
                .andExpect(jsonPath("$[?(@.categoryName == 'Dining')].totalAmount").value(65.00));
    }

    @Test
    void shouldRejectByCategoryWhenFromIsAfterToWith400() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(get("/api/reports/by-category?from=2026-04-01&to=2026-03-01&type=EXPENSE")
                .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("From date cannot be after to date"));
    }

    @Test
    void shouldReturnMonthlyReportWithAll12Months() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long salaryCat = createCategory(token, "Salary", CategoryType.INCOME);
        Long expenseCat = createCategory(token, "Groceries", CategoryType.EXPENSE);

        createTransaction(token, new BigDecimal("2500.00"), CategoryType.INCOME, LocalDate.of(2026, 1, 5), salaryCat);
        createTransaction(token, new BigDecimal("300.00"), CategoryType.EXPENSE, LocalDate.of(2026, 1, 12), expenseCat);
        createTransaction(token, new BigDecimal("450.00"), CategoryType.EXPENSE, LocalDate.of(2026, 3, 15), expenseCat);

        mockMvc.perform(get("/api/reports/monthly?year=2026")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[0].totalIncome").value(2500.00))
                .andExpect(jsonPath("$[0].totalExpense").value(300.00))
                .andExpect(jsonPath("$[0].netBalance").value(2200.00))
                .andExpect(jsonPath("$[1].month").value(2))
                .andExpect(jsonPath("$[1].totalIncome").value(0))
                .andExpect(jsonPath("$[1].totalExpense").value(0))
                .andExpect(jsonPath("$[1].netBalance").value(0))
                .andExpect(jsonPath("$[2].month").value(3))
                .andExpect(jsonPath("$[2].totalIncome").value(0))
                .andExpect(jsonPath("$[2].totalExpense").value(450.00))
                .andExpect(jsonPath("$[2].netBalance").value(-450.00))
                .andExpect(jsonPath("$[11].month").value(12))
                .andExpect(jsonPath("$[11].totalIncome").value(0))
                .andExpect(jsonPath("$[11].totalExpense").value(0));
    }

    @Test
    void shouldIsolateReportsBetweenDifferentUsers() throws Exception {
        String user1Token = testHelper.registerAndLogin("User One", "user1@example.com", "password123");
        Long user1Cat = createCategory(user1Token, "Income", CategoryType.INCOME);
        createTransaction(user1Token, new BigDecimal("1000.00"), CategoryType.INCOME, LocalDate.of(2026, 2, 1), user1Cat);

        String user2Token = testHelper.registerAndLogin("User Two", "user2@example.com", "password123");

        mockMvc.perform(get("/api/reports/summary?from=2026-01-01&to=2026-12-31")
                .header("Authorization", user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.transactionCount").value(0));

        mockMvc.perform(get("/api/reports/by-category?from=2026-01-01&to=2026-12-31&type=INCOME")
                .header("Authorization", user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/reports/monthly?year=2026")
                .header("Authorization", user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[1].totalIncome").value(0));
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
