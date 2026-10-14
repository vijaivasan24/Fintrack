package com.fintrack.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.dto.CategoryRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
class CategoryIntegrationTest {

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
    void shouldRejectCategoryRequestsWithoutToken() throws Exception {
        CategoryRequest request = new CategoryRequest("Groceries", CategoryType.EXPENSE);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateCategorySuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        CategoryRequest request = new CategoryRequest("Groceries", CategoryType.EXPENSE);

        mockMvc.perform(post("/api/categories")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void shouldReturnConflictWhenCreatingDuplicateCategory() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        CategoryRequest request = new CategoryRequest("Groceries", CategoryType.EXPENSE);

        mockMvc.perform(post("/api/categories")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListCategoriesForUser() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        createCategory(token, "Groceries", CategoryType.EXPENSE);
        createCategory(token, "Salary", CategoryType.INCOME);

        mockMvc.perform(get("/api/categories")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGetCategoryById() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        mockMvc.perform(get("/api/categories/" + categoryId)
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Groceries"));
    }

    @Test
    void shouldUpdateCategorySuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        CategoryRequest updateRequest = new CategoryRequest("Food & Dining", CategoryType.EXPENSE);

        mockMvc.perform(put("/api/categories/" + categoryId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Food & Dining"));
    }

    @Test
    void shouldDeleteCategorySuccessfully() throws Exception {
        String token = testHelper.registerAndLogin("Jane Doe", "jane@example.com", "password123");
        Long categoryId = createCategory(token, "Groceries", CategoryType.EXPENSE);

        mockMvc.perform(delete("/api/categories/" + categoryId)
                .header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/" + categoryId)
                .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPreventAnotherUserFromAccessingCategory() throws Exception {
        String tokenUser1 = testHelper.registerAndLogin("User One", "user1@example.com", "password123");
        Long categoryId = createCategory(tokenUser1, "Secret Category", CategoryType.EXPENSE);

        String tokenUser2 = testHelper.registerAndLogin("User Two", "user2@example.com", "password123");

        mockMvc.perform(get("/api/categories/" + categoryId)
                .header("Authorization", tokenUser2))
                .andExpect(status().isNotFound());

        CategoryRequest updateRequest = new CategoryRequest("Changed Name", CategoryType.EXPENSE);
        mockMvc.perform(put("/api/categories/" + categoryId)
                .header("Authorization", tokenUser2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/categories/" + categoryId)
                .header("Authorization", tokenUser2))
                .andExpect(status().isNotFound());
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
}
