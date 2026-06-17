package com.taskflow.taskManager.service;


import com.taskflow.taskManager.dto.request.LoginRequest;
import com.taskflow.taskManager.dto.request.RegisterRequest;
import com.taskflow.taskManager.dto.response.AuthResponse;
import com.taskflow.taskManager.entity.Role;
import com.taskflow.taskManager.entity.User;
import com.taskflow.taskManager.exception.DuplicateResourceException;
import com.taskflow.taskManager.exception.ResourceNotFoundException;
import com.taskflow.taskManager.repository.RoleRepository;
import com.taskflow.taskManager.repository.UserRepository;
import com.taskflow.taskManager.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")

public class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private Role mockRole;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {

        //Mock role
        mockRole = new Role();
        mockRole.setId(1L);
        mockRole.setName(Role.RoleName.ROLE_DEVELOPER);

        //Mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("Ajay Kumar");
        mockUser.setEmail("ajay@taskflow.com");
        mockUser.setPassword("encoded_password");
        mockUser.setActive(true);
        mockUser.setRoles(Set.of(mockRole));

        //Register request
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Ajay Kumar");
        registerRequest.setEmail("ajay@taskflow.com");
        registerRequest.setPassword("ajay123");

        //Login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("ajay@taskflow.com");
        loginRequest.setPassword("ajay123");

    }
    //REGISTER TESTS
    @Test
    @DisplayName("Register - Success")
    void register_ShouldReturnAuthResponse_WhenValidRequest(){

        //Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_DEVELOPER))
                .thenReturn(Optional.of(mockRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(mockUserDetails);
        when(jwtUtil.generateAccessToken(any())).thenReturn("mock_access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("mock_refresh_token");

        //Act
        AuthResponse response = authService.register(registerRequest);

        //Assert
        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals("mock_refresh_token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals("Ajay Kumar", response.getUser().getFullName());

        // Verify interactions
        verify(userRepository).existsByEmail("ajay@taskflow.com");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("ajay123");
    }
    @Test
    @DisplayName("Register - Fail - Email Already Exists")
    void register_ShouldThrowException_WhenEmailAlreadyExists() {

        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () ->
                authService.register(registerRequest)
        );

        // Verify save never called
        verify(userRepository, never()).save(any(User.class));
    }
    @Test
    @DisplayName("Register - Fail - Default Role Not Found")
    void register_ShouldThrowException_WhenDefaultRoleNotFound() {

        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_DEVELOPER))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                authService.register(registerRequest)
        );
    }
    // LOGIN TESTS
    @Test
    @DisplayName("Login - Success")
    void login_ShouldReturnAuthResponse_WhenValidCredentials() {

        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "ajay@taskflow.com", "ajay123"
                ));
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mockUser));

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(mockUserDetails);
        when(jwtUtil.generateAccessToken(any())).thenReturn("mock_access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("mock_refresh_token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertNotNull(response.getUser());
        assertEquals("ajay@taskflow.com", response.getUser().getEmail());

        verify(authenticationManager).authenticate(any());
    }
    @Test
    @DisplayName("Login - Fail - Bad Credentials")
    void login_ShouldThrowException_WhenBadCredentials() {

        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                authService.login(loginRequest)
        );
    }
    // REFRESH TOKEN TESTS
    @Test
    @DisplayName("Refresh Token - Success")
    void refreshToken_ShouldReturnNewAccessToken_WhenValidRefreshToken() {

        // Arrange
        String refreshToken = "valid_refresh_token";

        when(jwtUtil.extractEmail(refreshToken))
                .thenReturn("ajay@taskflow.com");
        when(jwtUtil.extractTokenType(refreshToken))
                .thenReturn("refresh");

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(mockUserDetails);
        when(jwtUtil.isTokenValid(anyString(), any()))
                .thenReturn(true);
        when(jwtUtil.generateAccessToken(any()))
                .thenReturn("new_access_token");
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mockUser));

        // Act
        AuthResponse response = authService.refreshToken(refreshToken);

        // Assert
        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
    }
    @Test
    @DisplayName("Refresh Token - Fail - Wrong Token Type")
    void refreshToken_ShouldThrowException_WhenNotRefreshToken() {

        // Arrange
        String accessToken = "this_is_access_token_not_refresh";

        when(jwtUtil.extractEmail(accessToken))
                .thenReturn("ajay@taskflow.com");
        when(jwtUtil.extractTokenType(accessToken))
                .thenReturn("access"); // wrong type!

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                authService.refreshToken(accessToken)
        );
    }
}
