package com.fintrack.service;

import com.fintrack.dto.AuthResponse;
import com.fintrack.dto.LoginRequest;
import com.fintrack.dto.RegisterRequest;
import com.fintrack.dto.UserResponse;
import com.fintrack.entity.User;
import com.fintrack.exception.DuplicateEmailException;
import com.fintrack.repository.UserRepository;
import com.fintrack.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Jane Doe", "jane@example.com", "hashedPassword");
        sampleUser.setId(1L);
    }

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Jane Doe", response.getName());
        assertEquals("jane@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenRegisteringDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("jane@example.com", "password123");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("jane@example.com")).thenReturn("mockToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        assertEquals("Jane Doe", response.getName());
    }

    @Test
    void shouldThrowWhenLoginWithWrongPassword() {
        LoginRequest request = new LoginRequest("jane@example.com", "wrongpassword");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void shouldThrowWhenLoginWithNonExistingEmail() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void shouldGetCurrentUser() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getCurrentUser("jane@example.com");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Jane Doe", response.getName());
        assertEquals("jane@example.com", response.getEmail());
    }
}
