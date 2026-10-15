package com.fintrack.perf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void shouldRefuseNonPerfDatasourceUrl() {
        assertThrows(IllegalStateException.class, () ->
                dataSeeder.validateDatasourceUrl("jdbc:mysql://localhost:3306/fintrack"));

        assertThrows(IllegalStateException.class, () ->
                dataSeeder.validateDatasourceUrl("jdbc:mysql://localhost:3306/prod_db"));

        assertThrows(IllegalStateException.class, () ->
                dataSeeder.validateDatasourceUrl(null));
    }

    @Test
    void shouldAcceptPerfDatasourceUrl() {
        assertDoesNotThrow(() ->
                dataSeeder.validateDatasourceUrl("jdbc:mysql://localhost:3306/fintrack_perf"));

        assertDoesNotThrow(() ->
                dataSeeder.validateDatasourceUrl("jdbc:mysql://localhost:3306/fintrack_perf?createDatabaseIfNotExist=true"));
    }

    @Test
    void shouldRefuseToRunWithNonPerfConnection() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn("jdbc:mysql://localhost:3306/fintrack");

        assertThrows(IllegalStateException.class, () -> dataSeeder.run());
    }
}
