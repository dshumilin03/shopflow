package com.shopflow.user.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.shopflow.user.dto.UserRequest;
import com.shopflow.user.dto.UserResponse;
import com.shopflow.user.model.Role;
import com.shopflow.user.mapper.UserMapper;
import com.shopflow.user.model.User;
import com.shopflow.user.repository.UserRepository;
import com.shopflow.user.service.password.PasswordService;
import com.shopflow.user.service.UserService;
import com.shopflow.user.service.impl.UserServiceImpl;
import com.shopflow.user.service.logging.LoggingSanitizer;
import com.shopflow.user.service.logging.ServiceLoggingAspect;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import({ServiceLoggingAspect.class, LoggingSanitizer.class, UserServiceImpl.class})
class ServiceLoggingAspectTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordService passwordService;

    @MockBean
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.shopflow.user.service.logging.ServiceLoggingAspect");
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        appender.stop();
        MDC.clear();
    }

    @Test
    @DisplayName("Logs successful service call with correlationId and args")
    void logsSuccessfulServiceCall() {
        Page<UserResponse> page = new PageImpl<>(List.of(
                new UserResponse(UUID.randomUUID(), "john@shopflow.com", "John Doe", Role.USER,
                        OffsetDateTime.now(), OffsetDateTime.now())
        ));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(Page.empty());
        when(userMapper.toResponse(any())).thenReturn(page.getContent().get(0));

        MDC.put("correlationId", "cid-ok");

        userService.getAllUsers(PageRequest.of(0, 10));

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent log = appender.list.get(0);
        String msg = log.getFormattedMessage();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(msg)
                .contains("[Service]")
                .contains("UserServiceImpl.getAllUsers(..)")
                .contains("correlationId=cid-ok")
                .contains("status=SUCCESS")
                .contains("duration=")
                .contains("args=")
                .contains("result=");
    }

    @Test
    @DisplayName("Logs service error correctly with correlationId and error message")
    void logsServiceError() {
        when(userRepository.findById(any(UUID.class))).thenThrow(new RuntimeException("Simulated service error"));

        MDC.put("correlationId", "cid-err");

        assertThatThrownBy(() -> userService.getUserById(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent log = appender.list.get(0);
        String msg = log.getFormattedMessage();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(msg)
                .contains("[Service][ERROR]")
                .contains("UserServiceImpl.getUserById(..)")
                .contains("correlationId=cid-err")
                .contains("status=FAILED")
                .contains("error=Simulated service error");
    }

    @Test
    @DisplayName("Logs service method duration field")
    void logsServiceDuration() {
        when(userRepository.findAll(any(PageRequest.class))).thenAnswer(inv -> {
            Thread.sleep(120);
            return Page.empty();
        });

        MDC.put("correlationId", "cid-dur");

        userService.getAllUsers(PageRequest.of(1, 5));

        assertThat(appender.list).isNotEmpty();
        String msg = appender.list.get(0).getFormattedMessage();

        assertThat(msg)
                .contains("duration=")
                .contains("correlationId=cid-dur");
    }

    @Test
    @DisplayName("Sanitizes sensitive args correctly")
    void sanitizesSensitiveArgs() {
        when(passwordService.encrypt(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toEntity(any(UserRequest.class))).thenAnswer(inv -> {
            UserRequest req = inv.getArgument(0);
            User u = new User();
            u.setEmail(req.email());
            u.setFullName(req.fullName());
            u.setRole(req.role());
            return u;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(UUID.randomUUID(), "mail@shopflow.com", "John Doe", Role.USER,
                        OffsetDateTime.now(), OffsetDateTime.now())
        );

        MDC.put("correlationId", "cid-sensitive");

        userService.createUser(new UserRequest("mail@shopflow.com", "John Doe", "superSecret", Role.USER));

        assertThat(appender.list).isNotEmpty();
        String msg = appender.list.get(0).getFormattedMessage();

        assertThat(msg)
                .contains("args=")
                .doesNotContain("superSecret")
                .contains("***");
    }

    @Test
    @DisplayName("Uses fallback correlationId when missing")
    void usesFallbackCorrelationIdWhenMissing() {
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(Page.empty());

        userService.getAllUsers(PageRequest.of(0, 5));

        assertThat(appender.list).isNotEmpty();
        String msg = appender.list.get(0).getFormattedMessage();

        assertThat(msg)
                .contains("[Service]")
                .contains("correlationId=")
                .contains("no-cid");
    }
}
