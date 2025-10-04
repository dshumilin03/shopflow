package com.shopflow.user.service.impl;

import com.shopflow.user.dto.UserRequest;
import com.shopflow.user.dto.UserResponse;
import com.shopflow.user.mapper.UserMapper;
import com.shopflow.user.model.User;
import com.shopflow.user.repository.UserRepository;
import com.shopflow.user.service.UserService;
import com.shopflow.user.service.exception.UserAlreadyExistsException;
import com.shopflow.user.service.exception.UserNotFoundException;
import com.shopflow.user.service.password.PasswordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordService passwordService;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest userRequest) {

        if (userRepository.findByEmail(userRequest.email()).isPresent()) {
            throw new UserAlreadyExistsException(userRequest.email());
        }
        User user = userMapper.toEntity(userRequest);

        String hashedPassword = passwordService.encrypt(userRequest.password());
        user.setPasswordHash(hashedPassword);

        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {

        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {

        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }

        userRepository.deleteById(id);
    }
}
