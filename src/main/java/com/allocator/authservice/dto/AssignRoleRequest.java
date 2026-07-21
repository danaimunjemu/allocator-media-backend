package com.allocator.authservice.dto;

import com.allocator.authservice.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRoleRequest {
    @NotBlank
    @Email
    private String email;

    @NotNull
    private RoleName roleName;

    @NotBlank
    private String brandCode;
}
