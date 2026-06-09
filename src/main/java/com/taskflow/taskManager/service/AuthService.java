package com.taskflow.taskManager.service;

import com.taskflow.taskManager.dto.request.LoginRequest;
import com.taskflow.taskManager.dto.request.RegisterRequest;
import com.taskflow.taskManager.dto.response.AuthResponse;
import com.taskflow.taskManager.dto.response.UserResponse;
import com.taskflow.taskManager.entity.Role;
import com.taskflow.taskManager.entity.User;
import com.taskflow.taskManager.exception.ResourceNotFoundException;
import com.taskflow.taskManager.repository.RoleRepository;
import com.taskflow.taskManager.repository.UserRepository;
import com.taskflow.taskManager.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    //REGISTER
    @Transactional
    public AuthResponse register(RegisterRequest request){

        //1.Check email already exist
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        //2.Default role = DEVELOPER
        Role developerRole = roleRepository.findByName(Role.RoleName.ROLE_DEVELOPER)
                .orElseThrow(()-> new ResourceNotFoundException("Default role not found. Run data seeder!"));

        //3.Build and save user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(developerRole));
        user.setActive(true);

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        //4.Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return buildAuthResponse(accessToken, refreshToken, savedUser);
    }

    //LOGIN
    public AuthResponse login(LoginRequest request){

        //1.Authenticate — throws exception if wrong credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        //2.Load user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        //3.Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    //REFRESH TOKEN
    public AuthResponse refreshToken(String refreshToken){

        //1.Extract email from refresh token
        String email = jwtUtil.extractEmail(refreshToken);

        //2.Validate Token type
        String tokenType = jwtUtil.extractTokenType(refreshToken);
        if(!"refresh".equals(tokenType)){
            throw new IllegalArgumentException("Invalid token type. Please provide a refresh token!");
        }

        //3.Load user and validate
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if(!jwtUtil.isTokenValid(refreshToken, userDetails)){
            throw new IllegalArgumentException("Refresh token expired. Please login again!");
        }

        //4.Generate new access token only
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(newAccessToken, refreshToken, user);
    }

    //HELPER
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user){
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .active(user.isActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

}
