package com.shopflow.user.dto;

import com.shopflow.user.model.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
