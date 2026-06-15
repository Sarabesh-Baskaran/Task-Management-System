package com.taskflow.taskManager.service;

import com.taskflow.taskManager.dto.request.ProjectRequest;
import com.taskflow.taskManager.dto.response.ProjectResponse;
import com.taskflow.taskManager.dto.response.UserResponse;
import com.taskflow.taskManager.entity.Project;
import com.taskflow.taskManager.entity.User;
import com.taskflow.taskManager.exception.ResourceNotFoundException;
import com.taskflow.taskManager.exception.UnauthorizedException;
import com.taskflow.taskManager.repository.ProjectRepository;
import com.taskflow.taskManager.repository.TaskRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    // CREATE PROJECT
    // ADMIN + MANAGER only

    @Transactional
    public ProjectResponse createProject(ProjectRequest request){

        User currentUser = getCurrentUserEntity();

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(
                request.getStatus() != null
                ? request.getStatus()
                        : Project.ProjectStatus.ACTIVE
        );
        project.setCreatedBy(currentUser);

        // Creator is automatically a member
        project.getMembers().add(currentUser);

        Project saved = projectRepository.save(project);
        log.info("Project created: {} by {}", saved.getId(), currentUser.getEmail());

        return mapToProjectResponse(saved);
    }
    // GET ALL PROJECTS
    // Paginated + filterable

    public Page<ProjectResponse> getAllProjects(int page, int size, String sortBy,
                                                String search, Project.ProjectStatus status){
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(sortBy).descending()
        );
        //Filter by search term
        if (search != null && !search.isEmpty()) {
            return projectRepository
                    .findByNameContainingIgnoreCase(search, pageable)
                    .map(this::mapToProjectResponse);
        }
        // Filter by status
        if (status != null) {
            return projectRepository
                    .findByStatus(status, pageable)
                    .map(this::mapToProjectResponse);
        }
        return projectRepository.findAll(pageable)
                .map(this::mapToProjectResponse);

    }
    // GET MY PROJECTS
    // Projects where I am member
    public Page<ProjectResponse> getMyProjects(int page, int size){
        User currentUser = getCurrentUserEntity();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return projectRepository.findProjectsByMemberId(currentUser.getId(), pageable)
                .map(this::mapToProjectResponse);
    }
    // GET PROJECT BY ID
    public ProjectResponse getProjectById(Long id) {
        Project project = findProjectById(id);
        return mapToProjectResponse(project);
    }
    // UPDATE PROJECT
    // ADMIN + MANAGER only
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request){
        Project project = findProjectById(id);
        User currentUser = getCurrentUserEntity();

        // Only creator or ADMIN can update
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));

        boolean isCreator = project.getCreatedBy()
                .getId().equals(currentUser.getId());

        if (!isAdmin && !isCreator) {
            throw new UnauthorizedException(
                    "Only project creator or ADMIN can update this project!"
            );
        }

        if (request.getName() != null)
            project.setName(request.getName());

        if (request.getDescription() != null)
            project.setDescription(request.getDescription());

        if (request.getStatus() != null)
            project.setStatus(request.getStatus());

        Project saved = projectRepository.save(project);
        log.info("Project updated: {}", saved.getId());

        return mapToProjectResponse(saved);
    }
    // DELETE PROJECT
    // ADMIN only
    @Transactional
    public void deleteProject(Long id){
        Project project = findProjectById(id);
        projectRepository.delete(project);

        log.info("Project deleted: {}", id);
    }
    // ADD MEMBER
    // ADMIN + MANAGER only
    @Transactional
    public ProjectResponse addMember(Long projectId, Long userId){
        Project project = findProjectById(projectId);
        User user = findUserById(userId);

        // Check already member
        boolean alreadyMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(userId));

        if (alreadyMember) {
            throw new IllegalArgumentException(
                    "User is already a member of this project!"
            );
        }
        project.getMembers().add(user);
        Project saved = projectRepository.save(project);

        log.info("User {} added to project {}", userId, projectId);

        return mapToProjectResponse(saved);
    }
    // REMOVE MEMBER
    // ADMIN + MANAGER only
    @Transactional
    public ProjectResponse removeMember(Long projectId, Long userId){

        Project project = findProjectById(projectId);
        User user = findUserById(userId);

        // Cannot remove project creator
        if (project.getCreatedBy().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Cannot remove project creator from members!"
            );
        }
        project.getMembers().remove(user);
        Project saved = projectRepository.save(project);

        log.info("User {} removed from project {}", userId, projectId);

        return mapToProjectResponse(saved);
    }
    // HELPERS
    private Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Project not found with id: " + id)
                );
    }
    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + id)
                );
    }
    private User getCurrentUserEntity() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Current user not found")
                );
    }
    public ProjectResponse mapToProjectResponse(Project project) {

        // Member list map
        var members = project.getMembers().stream()
                .map(m -> UserResponse.builder()
                        .id(m.getId())
                        .fullName(m.getFullName())
                        .email(m.getEmail())
                        .build())
                .collect(Collectors.toList());

        // CreatedBy map
        UserResponse createdBy = UserResponse.builder()
                .id(project.getCreatedBy().getId())
                .fullName(project.getCreatedBy().getFullName())
                .email(project.getCreatedBy().getEmail())
                .build();

        // Task count
        long totalTasks = taskRepository
                .countByProjectIdAndStatus(project.getId(), null) != null
                ? project.getTasks().size()
                : 0;

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .createdBy(createdBy)
                .members(members)
                .totalTasks(project.getTasks().size())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
