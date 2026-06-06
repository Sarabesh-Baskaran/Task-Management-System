package com.taskflow.taskManager.repository;


import com.taskflow.taskManager.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Tasks under a project
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    // Tasks assigned to a user
    Page<Task> findByAssignedToId(Long userId, Pageable pageable);

    // Tasks created by a user
    Page<Task> findByCreatedById(Long userId, Pageable pageable);

    // Filter by status
    Page<Task> findByProjectIdAndStatus(Long projectId, Task.TaskStatus status, Pageable pageable);

    // Filter by priority
    Page<Task> findByProjectIdAndPriority(Long projectId, Task.TaskPriority priority, Pageable pageable);

    // Count tasks by status in a project
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    Long countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") Task.TaskStatus status);
}
