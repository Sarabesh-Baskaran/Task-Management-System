package com.taskflow.taskManager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.taskManager.dto.request.LoginRequest;
import com.taskflow.taskManager.dto.request.RegisterRequest;
import com.taskflow.taskManager.dto.response.AuthResponse;
import com.taskflow.taskManager.dto.response.UserResponse;
import com.taskflow.taskManager.security.CustomUserDetailsService;
import com.taskflow.taskManager.security.JwtFilter;
import com.taskflow.taskManager.security.JwtUtil;
import com.taskflow.taskManager.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.taskflow.taskManager.security.JwtUtil;
import com.taskflow.taskManager.security.JwtFilter;
import com.taskflow.taskManager.security.CustomUserDetailsService;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Required by Spring Security in WebMvcTest
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtFilter jwtFilter;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .fullName("Ajay Kumar")
                .email("ajay@taskflow.com")
                .active(true)
                .roles(Set.of("ROLE_DEVELOPER"))
                .build();

        mockAuthResponse = AuthResponse.builder()
                .accessToken("mock_access_token")
                .refreshToken("mock_refresh_token")
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }
    // REGISTER TESTS
    @Test
    @DisplayName("POST /api/auth/register - Success")
    void register_ShouldReturn201_WhenValidRequest() throws Exception {

        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Ajay Kumar");
        request.setEmail("ajay@taskflow.com");
        request.setPassword("ajay123");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(mockAuthResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"))
                .andExpect(jsonPath("$.data.user.email").value("ajay@taskflow.com"));
    }
    @Test
    @DisplayName("POST /api/auth/register - Fail - Validation Error")
    void register_ShouldReturn400_WhenInvalidRequest() throws Exception {

        // Arrange — empty request body
        RegisterRequest request = new RegisterRequest();
        request.setFullName("");        // blank
        request.setEmail("not-email"); // invalid email
        request.setPassword("12");     // too short

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
    // LOGIN TESTS
    @Test
    @DisplayName("POST /api/auth/login - Success")
    void login_ShouldReturn200_WhenValidCredentials() throws Exception {

        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("ajay@taskflow.com");
        request.setPassword("ajay123");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(mockAuthResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }
    @Test
    @DisplayName("POST /api/auth/login - Fail - Empty Body")
    void login_ShouldReturn400_WhenEmptyBody() throws Exception {

        // Arrange
        LoginRequest request = new LoginRequest();
        // email and password both empty

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    // REFRESH TOKEN TESTS
    @Test
    @WithMockUser
    @DisplayName("POST /api/auth/refresh - Success")
    void refresh_ShouldReturn200_WhenValidRefreshToken() throws Exception {

        // Arrange
        when(authService.refreshToken(any()))
                .thenReturn(mockAuthResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .header("Refresh-Token", "valid_refresh_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }
}
