package com.taskflow.taskManager.service;


import com.taskflow.taskManager.dto.request.RoleRequest;
import com.taskflow.taskManager.dto.response.UserResponse;
import com.taskflow.taskManager.entity.Role;
import com.taskflow.taskManager.entity.User;
import com.taskflow.taskManager.exception.ResourceNotFoundException;
import com.taskflow.taskManager.repository.RoleRepository;
import com.taskflow.taskManager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
public class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private Role adminRole;
    private Role developerRole;

    @BeforeEach
    void setUp(){
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(Role.RoleName.ROLE_ADMIN);

        developerRole = new Role();
        developerRole.setId(3L);
        developerRole.setName(Role.RoleName.ROLE_DEVELOPER);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("Ajay Kumar");
        mockUser.setEmail("ajay@taskflow.com");
        mockUser.setActive(true);
        mockUser.setRoles(new HashSet<>(Set.of(developerRole)));

        // Setup security context mock
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ajay@taskflow.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
    // GET ALL USERS TESTS
    @Test
    @DisplayName("Get All Users - Should Return Paginated Users")
    void getAllUsers_ShouldReturnPageOfUsers() {

        // Arrange
        Page<User> mockPage = new PageImpl<>(List.of(mockUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        // Act
        Page<UserResponse> result = userService.getAllUsers(0, 10, "fullName");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Ajay Kumar", result.getContent().get(0).getFullName());
        verify(userRepository).findAll(any(Pageable.class));
    }
    // GET USER BY ID TESTS
    @Test
    @DisplayName("Get User By ID - Success")
    void getUserById_ShouldReturnUser_WhenExists() {

        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act
        UserResponse response = userService.getUserById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Ajay Kumar", response.getFullName());
        assertEquals("ajay@taskflow.com", response.getEmail());
    }
    @Test
    @DisplayName("Get User By ID - Fail - Not Found")
    void getUserById_ShouldThrowException_WhenNotFound() {

        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                userService.getUserById(999L)
        );
    }
    // GET CURRENT USER TESTS
    @Test
    @DisplayName("Get Current User - Success")
    void getCurrentUser_ShouldReturnLoggedInUser() {

        // Arrange
        when(userRepository.findByEmail("ajay@taskflow.com"))
                .thenReturn(Optional.of(mockUser));

        // Act
        UserResponse response = userService.getCurrentUser();

        // Assert
        assertNotNull(response);
        assertEquals("ajay@taskflow.com", response.getEmail());
    }
    // ASSIGN ROLE TESTS
    @Test
    @DisplayName("Assign Role - Success")
    void assignRole_ShouldAddRoleToUser_WhenValid() {

        // Arrange
        RoleRequest request = new RoleRequest();
        request.setRoleName(Role.RoleName.ROLE_ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findByName(Role.RoleName.ROLE_ADMIN))
                .thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        UserResponse response = userService.assignRole(1L, request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }
    @Test
    @DisplayName("Assign Role - Fail - Role Not Found")
    void assignRole_ShouldThrowException_WhenRoleNotFound() {

        // Arrange
        RoleRequest request = new RoleRequest();
        request.setRoleName(Role.RoleName.ROLE_ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findByName(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                userService.assignRole(1L, request)
        );
    }
    // DEACTIVATE USER TESTS
    @Test
    @DisplayName("Deactivate User - Success")
    void deactivateUser_ShouldSetActiveFalse_WhenNotSelf() {

        // Arrange — different user (not self)
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@taskflow.com");
        otherUser.setActive(true);
        otherUser.setRoles(new HashSet<>(Set.of(developerRole)));

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(userRepository.save(any(User.class))).thenReturn(otherUser);

        // Act
        userService.deactivateUser(2L);

        // Assert
        assertFalse(otherUser.isActive());
        verify(userRepository).save(otherUser);
    }
    @Test
    @DisplayName("Deactivate User - Fail - Cannot Deactivate Self")
    void deactivateUser_ShouldThrowException_WhenDeactivatingSelf() {

        // Arrange — same user (self)
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                userService.deactivateUser(1L)
        );

        verify(userRepository, never()).save(any());
    }
    // REMOVE ROLE TESTS
    @Test
    @DisplayName("Remove Role - Fail - Cannot Remove Last Role")
    void removeRole_ShouldThrowException_WhenLastRole() {

        // Arrange — user has only one role
        RoleRequest request = new RoleRequest();
        request.setRoleName(Role.RoleName.ROLE_DEVELOPER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        // mockUser already has only 1 role (developerRole)

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                userService.removeRole(1L, request)
        );

        verify(userRepository, never()).save(any());
    }

}
