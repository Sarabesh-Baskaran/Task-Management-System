package com.taskflow.taskManager.service;


import com.taskflow.taskManager.dto.request.TaskRequest;
import com.taskflow.taskManager.dto.response.TaskResponse;
import com.taskflow.taskManager.dto.response.UserResponse;
import com.taskflow.taskManager.entity.Project;
import com.taskflow.taskManager.entity.Task;
import com.taskflow.taskManager.entity.User;
import com.taskflow.taskManager.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
     private final TaskRepository taskRepository;
     private final ProjectRepository projectRepository;
     private final UserRepository userRepository;

    // CREATE TASK
    // Project member only
    @Transactional
    public TaskResponse createTask(TaskRequest request){

        Project project = findProjectById(request.getProjectId());
        User currentUser = getCurrentUserEntity();

        // Only project members can create tasks
        validateProjectMember(project, currentUser);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(
                request.getStatus() != null ? request.getStatus() : Task.TaskStatus.TODO
        );
        task.setPriority(
                request.getPriority() != null ? request.getPriority() : Task.TaskPriority.MEDIUM
        );
        task.setDueDate(request.getDueDate());
        task.setProject(project);
        task.setCreatedBy(currentUser);

        // Assign to user (if provided)
        if (request.getAssignedToId() != null) {
            User assignee = findUserById(request.getAssignedToId());
            validateProjectMember(project, assignee);
            task.setAssignedTo(assignee);
        }

        Task saved = taskRepository.save(task);
        log.info("Task created: {} in project {}", saved.getId(), project.getId());

        return mapToTaskResponse(saved);
    }
    // GET TASKS BY PROJECT
    // Paginated + filterable
    public Page<TaskResponse> getTasksByProject(
            Long projectId, int page, int size, String sortBy,
            Task.TaskStatus status, Task.TaskPriority priority) {

        // Validate project exists
        findProjectById(projectId);

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(sortBy).descending()
        );

        if (status != null) {
            return taskRepository
                    .findByProjectIdAndStatus(projectId, status, pageable)
                    .map(this::mapToTaskResponse);
        }
        if (priority != null) {
            return taskRepository
                    .findByProjectIdAndPriority(projectId, priority, pageable)
                    .map(this::mapToTaskResponse);
        }

        return taskRepository.findByProjectId(projectId, pageable)
                .map(this::mapToTaskResponse);
    }
    // GET MY TASKS
    // Tasks assigned to current user
    public Page<TaskResponse> getMyTasks(int page, int size, Task.TaskStatus status) {

        User currentUser = getCurrentUserEntity();

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("dueDate").ascending()
        );

        Page<Task> tasks = taskRepository
                .findByAssignedToId(currentUser.getId(), pageable);

        // Filter by status if provided
        if (status != null) {
            return tasks.map(this::mapToTaskResponse)
                    .map(t -> t)  // status filtering done at query level ideally
                    ;
        }

        return tasks.map(this::mapToTaskResponse);
    }
    // GET TASK BY ID
    public TaskResponse getTaskById(Long id) {
        Task task = findTaskById(id);
        return mapToTaskResponse(task);
    }
    // UPDATE TASK
    // Project member only
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {

        Task task = findTaskById(id);
        User currentUser = getCurrentUserEntity();

        validateProjectMember(task.getProject(), currentUser);

        if (request.getTitle() != null)
            task.setTitle(request.getTitle());

        if (request.getDescription() != null)
            task.setDescription(request.getDescription());

        if (request.getStatus() != null)
            task.setStatus(request.getStatus());

        if (request.getPriority() != null)
            task.setPriority(request.getPriority());

        if (request.getDueDate() != null)
            task.setDueDate(request.getDueDate());

        // Reassign task
        if (request.getAssignedToId() != null) {
            User assignee = findUserById(request.getAssignedToId());
            validateProjectMember(task.getProject(), assignee);
            task.setAssignedTo(assignee);
        }

        Task saved = taskRepository.save(task);
        log.info("Task updated: {}", saved.getId());

        return mapToTaskResponse(saved);
    }
    // UPDATE TASK STATUS ONLY
    // Quick status change (drag-drop board style)
    @Transactional
    public TaskResponse updateTaskStatus(Long id, Task.TaskStatus status) {

        Task task = findTaskById(id);
        User currentUser = getCurrentUserEntity();

        validateProjectMember(task.getProject(), currentUser);

        task.setStatus(status);
        Task saved = taskRepository.save(task);

        log.info("Task {} status changed to {}", id, status);

        return mapToTaskResponse(saved);
    }
    // DELETE TASK
    // ADMIN, MANAGER, or task creator
    @Transactional
    public void deleteTask(Long id) {

        Task task = findTaskById(id);
        User currentUser = getCurrentUserEntity();

        boolean isAdminOrManager = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")
                        || r.getName().name().equals("ROLE_MANAGER"));

        boolean isCreator = task.getCreatedBy().getId()
                .equals(currentUser.getId());

        if (!isAdminOrManager && !isCreator) {
            throw new IllegalArgumentException(
                    "Only task creator, ADMIN, or MANAGER can delete this task!"
            );
        }

        taskRepository.delete(task);
        log.info("Task deleted: {}", id);
    }
    // HELPERS
    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found with id: " + id)
                );
    }

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
    // Check if user is a member of the project (or ADMIN)
    private void validateProjectMember(Project project, User user) {

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));

        if (isAdmin) return;

        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));

        if (!isMember) {
            throw new IllegalArgumentException(
                    "User must be a project member to perform this action!"
            );
        }
    }
    public TaskResponse mapToTaskResponse(Task task) {

        UserResponse createdBy = UserResponse.builder()
                .id(task.getCreatedBy().getId())
                .fullName(task.getCreatedBy().getFullName())
                .email(task.getCreatedBy().getEmail())
                .build();

        UserResponse assignedTo = null;
        if (task.getAssignedTo() != null) {
            assignedTo = UserResponse.builder()
                    .id(task.getAssignedTo().getId())
                    .fullName(task.getAssignedTo().getFullName())
                    .email(task.getAssignedTo().getEmail())
                    .build();
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .createdBy(createdBy)
                .assignedTo(assignedTo)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }


}
