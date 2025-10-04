package com.shopflow.user.repository;

import com.shopflow.user.TestcontainersConfiguration;
import com.shopflow.user.model.User;
import com.shopflow.user.model.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserRepositoryTest {

    private final UserRepository userRepository;

    @Test
    void testSaveAndFindByEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setFullName("Test User");
        user.setRole(Role.USER);

        userRepository.save(user);
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getPasswordHash()).isEqualTo("hashed_password");
    }

    @Test
    void findByEmail_shouldReturnUser() {
        User user = new User();
        user.setEmail("unique@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("unique@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void duplicateEmail_shouldThrowException() {
        User user1 = new User();
        user1.setEmail("dup@example.com");
        user1.setPasswordHash("hash1");
        user1.setRole(Role.USER);
        userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("dup@example.com");
        user2.setPasswordHash("hash2");
        user2.setRole(Role.USER);

        assertThrows(Exception.class, () -> userRepository.saveAndFlush(user2));
    }
}
