package com.shopflow.user.service;

import com.shopflow.user.dto.UserRequest;
import com.shopflow.user.dto.UserResponse;
import com.shopflow.user.mapper.UserMapper;
import com.shopflow.user.model.Role;
import com.shopflow.user.model.User;
import com.shopflow.user.repository.UserRepository;
import com.shopflow.user.service.exception.UserAlreadyExistsException;
import com.shopflow.user.service.exception.UserNotFoundException;
import com.shopflow.user.service.impl.UserServiceImpl;
import com.shopflow.user.service.password.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private User user;
    private UserRequest request;
    private UserResponse response;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        user.setFullName("John Doe");
        user.setRole(Role.USER);
        user.setCreatedAt(OffsetDateTime.now().minusDays(1));
        user.setUpdatedAt(OffsetDateTime.now());

        request = new UserRequest("test@example.com", "John Doe", "plain", Role.USER);

        response = new UserResponse(
                USER_ID,
                "test@example.com",
                "John Doe",
                Role.USER,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Test
    @DisplayName("Creates user with valid arguments")
    void createUserWithValidArgumentsSuccess() {
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordService.encrypt("plain")).thenReturn("hashed123");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertThat(result).isEqualTo(response);
        verify(passwordService).encrypt("plain");
        verify(userRepository).save(user);
        verify(userMapper).toEntity(request);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("createUser Throws UserAlreadyExistsException if email already exists")
    void createUserShouldThrowExceptionIfDuplicateEmail() {
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(request.email());

        verify(userRepository, never()).save(any());
        verify(passwordService, never()).encrypt(any());
    }

    @Test
    @DisplayName("getUserById returns UserResponse if found")
    void getUserByIdShouldReturnUserResponseIfFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserById(USER_ID);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(USER_ID);
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("getUserById throws UserNotFoundException if not found")
    void getUserByIdShouldReturnExceptionIfNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());
    }

    @Test
    @DisplayName("getUserByEmail returns UserResponse if found")
    void getUserByEmailShouldReturnUserResponseIfFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserByEmail("test@example.com");

        assertThat(result).isEqualTo(response);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("getUserByEmail throws UserNotFoundException if not found")
    void getUserByEmailShouldThrowExceptionIfNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("test@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("test@example.com");
    }

    @Test
    @DisplayName("getAllUsers returns users Page")
    void getAllUsersShouldreturnUsersPage() {
        var pageable = PageRequest.of(0, 10);
        var users = List.of(user);
        var page = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("deleteUser deletes user if found")
    void deleteUserShouldDeleteUserIfFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userService.deleteUser(USER_ID);

        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("deleteUser throws UserNotFoundException user if not found")
    void deleteUserShouldThrowExceptionIfNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());

        verify(userRepository, never()).deleteById(any());
    }
}