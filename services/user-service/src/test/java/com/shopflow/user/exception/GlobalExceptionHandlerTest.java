package com.shopflow.user.exception;

import com.shopflow.user.config.TestSecurityConfig;
import com.shopflow.user.controller.UserController;
import com.shopflow.user.dto.UserResponse;
import com.shopflow.user.service.UserService;
import com.shopflow.user.service.exception.GlobalExceptionHandler;
import com.shopflow.user.service.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
public class GlobalExceptionHandlerTest {

    private final String userUUID = "00000000-0000-0000-0000-000000000001";
    private final String userEmail = "sample@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    public void shouldReturnUserNotFoundExceptionWithUUID() throws Exception {

        when(userService.getUserById(any())).thenThrow(new UserNotFoundException(userUUID));

        mockMvc.perform(get("/api/users/" + userUUID)
                        .header("X-Correlation-Id", "abc-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(userUUID)))
                .andExpect(jsonPath("$.correlationId").value("abc-123"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    public void shouldReturnUserNotFoundExceptionWithEmail() throws Exception {

        when(userService.getUserByEmail(any())).thenThrow(new UserNotFoundException(userEmail));

        mockMvc.perform(get("/api/users/by-email?email=" + userEmail)
                        .header("X-Correlation-Id", "abc-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(userEmail)))
                .andExpect(jsonPath("$.correlationId").value("abc-123"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    public void shouldReturnMethodArgumentNotValidWithInformation() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"sample@gmail.com\", \"password\": \"\", \"role\": \"USER\" }")
                        .header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("METHOD_ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password: must not be blank")))
                .andExpect(jsonPath("$.correlationId").value("abc-123"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    public void shouldReturnInternatlErrorWithInformation() throws Exception {

        mockMvc.perform(post("/api/users/by-email?email=" + userEmail)
                        .header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Request method 'POST' is not supported")))
                .andExpect(jsonPath("$.correlationId").value("abc-123"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500));
    }
}

