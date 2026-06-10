package com.taskflow.taskManager.service;

import com.taskflow.taskManager.dto.request.RoleRequest;
import com.taskflow.taskManager.dto.response.UserResponse;
import com.taskflow.taskManager.entity.Role;
import com.taskflow.taskManager.entity.User;
import com.taskflow.taskManager.exception.ResourceNotFoundException;
import com.taskflow.taskManager.repository.RoleRepository;
import com.taskflow.taskManager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // GET ALL USERS
    // (ADMIN only)

    public Page<UserResponse> getAllUsers(int page, int size, String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());

        return userRepository.findAll(pageable).map(this::mapToUserResponse);

    }

    // GET USER BY ID

    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return mapToUserResponse(user);
    }

    // GET CURRENT USER (ME)

    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        return mapToUserResponse(user);
    }

    // ASSIGN ROLE TO USER
    // (ADMIN only)

    @Transactional
    public UserResponse assignRole(Long userId, RoleRequest request) {

        User user = findUserById(userId);

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRoleName()));

        user.getRoles().add(role);
        User saved = userRepository.save(user);

        log.info("Role {} assigned to user {}", request.getRoleName(), userId);

        return mapToUserResponse(saved);
    }

    // REMOVE ROLE FROM USER
    // (ADMIN only)

    @Transactional
    public UserResponse removeRole(Long userId, RoleRequest request) {

        User user = findUserById(userId);
        //Prevent removing last role
        if (user.getRoles().size() == 1) {
            throw new IllegalArgumentException(
                    "Cannot remove last role. User must have at least one role!"
            );
        }

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + request.getRoleName()
                        )
                );

        user.getRoles().remove(role);
        User saved = userRepository.save(user);

        log.info("Role {} removed from user {}", request.getRoleName(), userId);

        return mapToUserResponse(saved);

    }
    // DEACTIVATE USER
    // (ADMIN only)

    @Transactional
    public UserResponse deactivateUser(Long userId){

        User user = findUserById(userId);

        // Prevent self deactivation
        String currentEmail = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        if (user.getEmail().equals(currentEmail)) {
            throw new IllegalArgumentException(
                    "You cannot deactivate your own account!"
            );
        }

        user.setActive(false);
        User saved = userRepository.save(user);

        log.info("User deactivated: {}", userId);

        return mapToUserResponse(saved);
    }

    // ACTIVATE USER
    // (ADMIN only)

    @Transactional
    public UserResponse activateUser(Long userId) {
        User user = findUserById(userId);
        user.setActive(true);
        User saved = userRepository.save(user);

        log.info("User activated: {}", userId);

        return mapToUserResponse(saved);
    }

    // HELPERS

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + id)
                );
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .active(user.isActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }

}

