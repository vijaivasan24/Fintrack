package com.fintrack.perf;

import com.fintrack.entity.CategoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("seed")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(JdbcTemplate jdbcTemplate, DataSource dataSource, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
    }

    public void validateDatasourceUrl(String url) {
        if (url == null || !url.contains("fintrack_perf")) {
            throw new IllegalStateException("Refusing to seed non-perf datasource: " + url);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        String url;
        try (Connection conn = dataSource.getConnection()) {
            url = conn.getMetaData().getURL();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read datasource URL", e);
        }

        validateDatasourceUrl(url);

        Integer existingUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (existingUsers != null && existingUsers > 0) {
            log.info("Users table already contains rows. Skipping data seeding.");
            return;
        }

        log.info("Starting performance test data seeding on fintrack_perf...");
        seedUsers();
        seedCategories();
        seedTransactions();
        seedBudgets();

        logRowCounts();
    }

    private void seedUsers() {
        String hashedPassword = passwordEncoder.encode("PerfPass123!");
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String sql = "INSERT INTO users (name, email, password, created_at) VALUES (?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>(500);
        for (int i = 1; i <= 500; i++) {
            batch.add(new Object[]{"Perf User " + i, "perf" + i + "@example.com", hashedPassword, now});
        }
        jdbcTemplate.batchUpdate(sql, batch);
        log.info("Inserted 500 users.");
    }

    private void seedCategories() {
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id FROM users ORDER BY id ASC");
        String sql = "INSERT INTO categories (name, type, user_id) VALUES (?, ?, ?)";

        List<Object[]> batch = new ArrayList<>(users.size() * 6);
        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("id")).longValue();
            batch.add(new Object[]{"Salary", CategoryType.INCOME.name(), userId});
            batch.add(new Object[]{"Investments", CategoryType.INCOME.name(), userId});
            batch.add(new Object[]{"Housing", CategoryType.EXPENSE.name(), userId});
            batch.add(new Object[]{"Groceries", CategoryType.EXPENSE.name(), userId});
            batch.add(new Object[]{"Utilities", CategoryType.EXPENSE.name(), userId});
            batch.add(new Object[]{"Entertainment", CategoryType.EXPENSE.name(), userId});
        }
        jdbcTemplate.batchUpdate(sql, batch);
        log.info("Inserted {} categories.", batch.size());
    }

    private void seedTransactions() {
        List<Map<String, Object>> categories = jdbcTemplate.queryForList(
                "SELECT id, name, type, user_id FROM categories ORDER BY user_id ASC, id ASC"
        );

        Map<Long, List<Long>> incomeCategoryMap = new HashMap<>();
        Map<Long, List<Long>> expenseCategoryMap = new HashMap<>();

        for (Map<String, Object> cat : categories) {
            Long userId = ((Number) cat.get("user_id")).longValue();
            Long catId = ((Number) cat.get("id")).longValue();
            String type = (String) cat.get("type");

            if (CategoryType.INCOME.name().equals(type)) {
                incomeCategoryMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(catId);
            } else {
                expenseCategoryMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(catId);
            }
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id FROM users ORDER BY id ASC");
        LocalDate today = LocalDate.now();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String sql = "INSERT INTO transactions (amount, type, date, description, created_at, user_id, category_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>(users.size() * 100);

        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("id")).longValue();
            List<Long> incomeCats = incomeCategoryMap.get(userId);
            List<Long> expenseCats = expenseCategoryMap.get(userId);

            for (int t = 0; t < 100; t++) {
                LocalDate txDate = today.minusDays((long) (t * 365) / 100 + 1);
                Date sqlDate = Date.valueOf(txDate);

                if (t % 5 == 0) {
                    Long catId = incomeCats.get((t / 5) % incomeCats.size());
                    BigDecimal amount = BigDecimal.valueOf(2500 + (t * 20)).setScale(2, RoundingMode.HALF_UP);
                    String desc = (t % 2 == 0) ? "Monthly Salary" : "Investment Dividend";
                    batch.add(new Object[]{amount, CategoryType.INCOME.name(), sqlDate, desc, now, userId, catId});
                } else {
                    Long catId = expenseCats.get(t % expenseCats.size());
                    BigDecimal amount = BigDecimal.valueOf(25 + ((t * 11) % 180)).setScale(2, RoundingMode.HALF_UP);
                    String desc;
                    int variant = t % 4;
                    if (variant == 0) {
                        desc = "Housing and rent payment";
                    } else if (variant == 1) {
                        desc = "Supermarket groceries";
                    } else if (variant == 2) {
                        desc = "Electricity and water utility";
                    } else {
                        desc = "Dinner and cinema entertainment";
                    }
                    batch.add(new Object[]{amount, CategoryType.EXPENSE.name(), sqlDate, desc, now, userId, catId});
                }
            }
        }

        jdbcTemplate.batchUpdate(sql, batch);
        log.info("Inserted {} transactions.", batch.size());
    }

    private void seedBudgets() {
        List<Map<String, Object>> categories = jdbcTemplate.queryForList(
                "SELECT id, user_id FROM categories WHERE type = 'EXPENSE' ORDER BY user_id ASC, id ASC"
        );

        Map<Long, List<Long>> userExpenseMap = new HashMap<>();
        for (Map<String, Object> cat : categories) {
            Long userId = ((Number) cat.get("user_id")).longValue();
            Long catId = ((Number) cat.get("id")).longValue();
            userExpenseMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(catId);
        }

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        String sql = "INSERT INTO budgets (limit_amount, month, year, user_id, category_id) VALUES (?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>(userExpenseMap.size() * 3);

        for (Map.Entry<Long, List<Long>> entry : userExpenseMap.entrySet()) {
            Long userId = entry.getKey();
            List<Long> expenseCats = entry.getValue();

            BigDecimal[] limits = {
                    BigDecimal.valueOf(1500.00),
                    BigDecimal.valueOf(600.00),
                    BigDecimal.valueOf(300.00)
            };

            for (int i = 0; i < 3 && i < expenseCats.size(); i++) {
                batch.add(new Object[]{limits[i], month, year, userId, expenseCats.get(i)});
            }
        }

        jdbcTemplate.batchUpdate(sql, batch);
        log.info("Inserted {} budgets.", batch.size());
    }

    private void logRowCounts() {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer categoryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
        Integer transactionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        Integer budgetCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM budgets", Integer.class);

        log.info("Data seeding finished: users={}, categories={}, transactions={}, budgets={}",
                userCount, categoryCount, transactionCount, budgetCount);
    }
}
