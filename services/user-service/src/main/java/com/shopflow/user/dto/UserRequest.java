package com.shopflow.user.dto;

import com.shopflow.user.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank  @Email String email,
        String fullName,
        @NotBlank String password,
        Role role
) {}
