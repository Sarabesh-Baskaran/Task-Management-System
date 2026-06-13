package com.taskflow.taskManager.controller;

import com.taskflow.taskManager.dto.request.TaskRequest;
import com.taskflow.taskManager.dto.response.ApiResponse;
import com.taskflow.taskManager.dto.response.TaskResponse;
import com.taskflow.taskManager.entity.Task;
import com.taskflow.taskManager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    // POST /api/tasks
    // Project member only
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request) {

        TaskResponse task = taskService.createTask(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", task));
    }
    // GET /api/tasks/project/{projectId}?page=0&size=10&sortBy=dueDate&status=&priority=
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) Task.TaskStatus status,
            @RequestParam(required = false) Task.TaskPriority priority) {

        Page<TaskResponse> tasks = taskService.getTasksByProject(
                projectId, page, size, sortBy, status, priority
        );

        return ResponseEntity.ok(
                ApiResponse.success("Tasks fetched successfully", tasks)
        );
    }
    // GET /api/tasks/my?page=0&size=10
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getMyTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Task.TaskStatus status) {

        Page<TaskResponse> tasks = taskService.getMyTasks(page, size, status);

        return ResponseEntity.ok(
                ApiResponse.success("My tasks fetched successfully", tasks)
        );
    }
    // GET /api/tasks/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
            @PathVariable Long id) {

        TaskResponse task = taskService.getTaskById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Task fetched successfully", task)
        );
    }
    // PUT /api/tasks/{id}
    // Project member only
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {

        TaskResponse task = taskService.updateTask(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Task updated successfully", task)
        );
    }
    // PATCH /api/tasks/{id}/status
    // Quick status update (e.g. drag-drop board)
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Task.TaskStatus status = Task.TaskStatus.valueOf(body.get("status"));
        TaskResponse task = taskService.updateTaskStatus(id, status);

        return ResponseEntity.ok(
                ApiResponse.success("Task status updated successfully", task)
        );
    }
    // DELETE /api/tasks/{id}
    // ADMIN, MANAGER, or creator
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long id) {

        taskService.deleteTask(id);

        return ResponseEntity.ok(
                ApiResponse.success("Task deleted successfully")
        );
    }
}
