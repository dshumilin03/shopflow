package com.shopflow.user.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.shopflow.user.controller.UserController;
import com.shopflow.user.service.UserService;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.shopflow.user.config.TestSecurityConfig;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class ControllerLoggingAspectTest {

    @Autowired
    UserController userController;

    @MockBean
    UserService userService;

    @Autowired
    private MockMvc mockMvc;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.shopflow.user.service.logging.ControllerLoggingAspect");
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        appender.stop();
    }

    @Test
    @DisplayName("Logs successful controller call with correlationId and args")
    void logsSuccessfulControllerCall() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("X-Correlation-Id", "abc-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        List<ILoggingEvent> logs = appender.list;
        assertThat(logs).isNotEmpty();

        ILoggingEvent logEvent = logs.get(0);
        String message = logEvent.getFormattedMessage();

        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(message)
                .contains("[Controller]")
                .contains("GET /api/users")
                .contains("status=200")
                .contains("correlationId=abc-123")
                .contains("handler=UserController.getAll(..)")
                .contains("args=");
    }

    @Test
    @DisplayName("Logs controller error correctly with correlationId and error message")
    void logsErrorFromController() throws Exception {

        when(userService.getUserById(any())).thenThrow(new RuntimeException("Simulated internal error"));

        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/users/" + id)
                        .header("X-Correlation-Id", "cid-err"))
                .andExpect(status().isInternalServerError());

        List<ILoggingEvent> logs = appender.list;
        assertThat(logs).isNotEmpty();

        ILoggingEvent logEvent = logs.get(0);
        String message = logEvent.getFormattedMessage();

        assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(message)
                .contains("[Controller][ERROR]")
                .contains("GET /api/users/" + id)
                .contains("correlationId=cid-err")
                .contains("error=Simulated internal error")
                .contains("args=");
    }

    @Test
    @DisplayName("Generates correlationId when header missing")
    void generatesCorrelationIdWhenMissing() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());

        List<ILoggingEvent> logs = appender.list;
        String message = logs.get(0).getFormattedMessage();

        assertThat(message).contains("correlationId=");
        String cid = message.substring(message.indexOf("correlationId=") + 14).split(" ")[0];
        assertThat(cid).isNotBlank();
        UUID.fromString(cid);
    }

    @Test
    @DisplayName("Logs request duration field")
    void logsRequestDuration() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("X-Correlation-Id", "cid-delay"))
                .andExpect(status().isOk());

        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message)
                .contains("duration=")
                .contains("cid-delay");
    }

    @Test
    @DisplayName("Logs sanitized args")
    void logsSanitizedArgs() throws Exception {
        String requestBody = """
        {
            "email": "test@example.com",
            "fullName": "John Doe",
            "password": "secret123",
            "role": "USER"
        }
        """;

        mockMvc.perform(post("/api/users")
                        .header("X-Correlation-Id", "cid-sanitize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        List<ILoggingEvent> logs = appender.list;
        assertThat(logs).isNotEmpty();

        String message = logs.get(0).getFormattedMessage();

        assertThat(message)
                .contains("args=")
                .contains("correlationId=cid-sanitize")
                .contains("UserController.createUser(..)");
    }

}
