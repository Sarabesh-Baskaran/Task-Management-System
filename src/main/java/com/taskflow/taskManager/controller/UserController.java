package com.taskflow.taskManager.controller;

import com.taskflow.taskManager.dto.request.RoleRequest;
import com.taskflow.taskManager.dto.response.ApiResponse;
import com.taskflow.taskManager.dto.response.UserResponse;
import com.taskflow.taskManager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users?page=0&size=10&sortBy=fullName
    // ADMIN only
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy){

        Page<UserResponse> users = userService.getAllUsers(page, size, sortBy);

        return ResponseEntity.ok(
                ApiResponse.success("Users fetched successfully", users)
        );
    }
    // GET /api/users/me
    // Any logged in user
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        UserResponse user = userService.getCurrentUser();

        return ResponseEntity.ok(
                ApiResponse.success("Current user fetched", user)
        );
    }
    // GET /api/users/{id}
    // ADMIN only
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id) {

        UserResponse user = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success("User fetched successfully", user)
        );
    }

    // POST /api/users/{id}/roles/assign
    // ADMIN only
    @PostMapping("/{id}/roles/assign")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {

        UserResponse user = userService.assignRole(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Role assigned successfully", user)
        );
    }

    // DELETE /api/users/{id}/roles/remove
    // ADMIN only
    @DeleteMapping("/{id}/roles/remove")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {

        UserResponse user = userService.removeRole(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Role removed successfully", user)
        );
    }

    // PATCH /api/users/{id}/deactivate
    // ADMIN only
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable Long id) {

        UserResponse user = userService.deactivateUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully", user)
        );
    }

    // PATCH /api/users/{id}/activate
    // ADMIN only
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @PathVariable Long id) {

        UserResponse user = userService.activateUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User activated successfully", user)
        );
    }

}
