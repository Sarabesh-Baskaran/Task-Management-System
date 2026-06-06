package com.taskflow.taskManager.dto.response;

import com.taskflow.taskManager.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Project.ProjectStatus status;
    private UserResponse createdBy;
    private List<UserResponse> members;
    private long totalTasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
