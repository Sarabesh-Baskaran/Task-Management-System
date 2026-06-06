package com.taskflow.taskManager.repository;


import com.taskflow.taskManager.entity.Project;
import com.taskflow.taskManager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // All projects created by a specific user
    Page<Project> findByCreatedBy(User user, Pageable pageable);

    // All projects where user is a member
    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.id = :userId")
    Page<Project> findProjectsByMemberId(@Param("userId") Long userId, Pageable pageable);

    // Search projects by name
    Page<Project> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Filter by status
    Page<Project> findByStatus(Project.ProjectStatus status, Pageable pageable);


}
