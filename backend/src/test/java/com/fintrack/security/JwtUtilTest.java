package com.fintrack.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "unitTestSecretKeyForTestingJwtTokenGenerationAndValidation1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(email, jwtUtil.extractEmail(token));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }
}
