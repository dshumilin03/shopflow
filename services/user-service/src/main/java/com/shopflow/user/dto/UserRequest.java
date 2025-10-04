package com.shopflow.user.dto;

import com.shopflow.user.model.Role;
import com.shopflow.user.service.logging.annotation.Sensitive;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank  @Email String email,
        String fullName,
        @NotBlank @Sensitive String password,
        Role role
) {}
