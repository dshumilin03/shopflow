package com.shopflow.user.service;

import com.shopflow.user.dto.UserRequest;
import com.shopflow.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse createUser(UserRequest userRequest);
    UserResponse getUserById(UUID id);
    UserResponse getUserByEmail(String email);
    Page<UserResponse> getAllUsers(Pageable pageable);
    void deleteUser(UUID id);
}
