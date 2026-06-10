package com.taskflow.taskManager.dto.request;

import com.taskflow.taskManager.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleRequest {

    @NotNull(message = "Role is required")
    private Role.RoleName roleName;
}
