package com.taskflow.taskManager.controller;

import com.taskflow.taskManager.dto.request.ProjectRequest;
import com.taskflow.taskManager.dto.response.ApiResponse;
import com.taskflow.taskManager.dto.response.ProjectResponse;
import com.taskflow.taskManager.entity.Project;
import com.taskflow.taskManager.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    // POST /api/projects
    // ADMIN + MANAGER only
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectRequest request) {

        ProjectResponse project = projectService.createProject(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", project));
    }
    // GET /api/projects?page=0&size=10&sortBy=name&search=&status=
    // Any authenticated user
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Project.ProjectStatus status) {

        Page<ProjectResponse> projects =
                projectService.getAllProjects(page, size, sortBy, search, status);

        return ResponseEntity.ok(
                ApiResponse.success("Projects fetched successfully", projects)
        );
    }
    // GET /api/projects/my
    // Current user's projects
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getMyProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProjectResponse> projects =
                projectService.getMyProjects(page, size);

        return ResponseEntity.ok(
                ApiResponse.success("My projects fetched", projects)
        );
    }
    // GET /api/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Long id) {

        ProjectResponse project = projectService.getProjectById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Project fetched successfully", project)
        );
    }
    // PUT /api/projects/{id}
    // ADMIN + MANAGER only
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {

        ProjectResponse project = projectService.updateProject(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Project updated successfully", project)
        );
    }
    // DELETE /api/projects/{id}
    // ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long id) {

        projectService.deleteProject(id);

        return ResponseEntity.ok(
                ApiResponse.success("Project deleted successfully")
        );
    }
    // POST /api/projects/{id}/members/{userId}
    // ADMIN + MANAGER only
    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> addMember(
            @PathVariable Long id,
            @PathVariable Long userId) {

        ProjectResponse project = projectService.addMember(id, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Member added successfully", project)
        );
    }
    // DELETE /api/projects/{id}/members/{userId}
    // ADMIN + MANAGER only
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {

        ProjectResponse project = projectService.removeMember(id, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Member removed successfully", project)
        );
    }
}
