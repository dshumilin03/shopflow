package com.shopflow.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    String email;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(name = "full_name")
    String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    Role role;

    @CreationTimestamp
    @Column(name="created_at")
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    OffsetDateTime updatedAt;

}
