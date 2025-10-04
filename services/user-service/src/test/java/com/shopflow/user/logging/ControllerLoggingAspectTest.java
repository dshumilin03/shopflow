package com.shopflow.user.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.shopflow.user.config.TestSecurityConfig;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControllerLoggingAspectTest {

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

    // ──────────────────────────────── TESTS ────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Logs successful controller call with correlationId and args")
    void logsSuccessfulControllerCall() throws Exception {
        mockMvc.perform(get("/api/test")
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
                .contains("GET /api/test")
                .contains("status=200")
                .contains("correlationId=abc-123")
                .contains("handler=DummyController.ok")
                .contains("args=");
    }

    @Test
    @Order(2)
    @DisplayName("Logs controller error correctly with correlationId and error message")
    void logsErrorFromController() throws Exception {
        mockMvc.perform(get("/api/test/error")
                        .header("X-Correlation-Id", "cid-err"))
                .andExpect(status().isInternalServerError());

        List<ILoggingEvent> logs = appender.list;
        assertThat(logs).isNotEmpty();

        ILoggingEvent logEvent = logs.get(0);
        String message = logEvent.getFormattedMessage();

        assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(message)
                .contains("[Controller][ERROR]")
                .contains("GET /api/test/error")
                .contains("correlationId=cid-err")
                .contains("error=Boom!")
                .contains("args=");
    }

    @Test
    @Order(3)
    @DisplayName("Generates correlationId when header missing")
    void generatesCorrelationIdWhenMissing() throws Exception {
        mockMvc.perform(get("/api/test/noheader"))
                .andExpect(status().isOk());

        List<ILoggingEvent> logs = appender.list;
        String message = logs.get(0).getFormattedMessage();

        assertThat(message).contains("correlationId=");
        String cid = message.substring(message.indexOf("correlationId=") + 14).split(" ")[0];
        assertThat(cid).isNotBlank();
        UUID.fromString(cid); // validate UUID format
    }

    @Test
    @Order(4)
    @DisplayName("Logs request duration field")
    void logsRequestDuration() throws Exception {
        mockMvc.perform(get("/api/test/delay")
                        .header("X-Correlation-Id", "cid-delay"))
                .andExpect(status().isOk());

        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message)
                .contains("duration=")
                .contains("cid-delay");
    }

    @Test
    @Order(5)
    @DisplayName("Logs sanitized args")
    void logsSanitizedArgs() throws Exception {
        mockMvc.perform(get("/api/test")
                        .param("param", "sensitiveValue"))
                .andExpect(status().isOk());

        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("args=");
    }
}
