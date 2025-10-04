package com.shopflow.user.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException e) {
        return buildErrorResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst().orElse("Invalid request data");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "METHOD_ARGUMENT_NOT_VALID", errorMsg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", e.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String code, String message) {
        String correlationId = MDC.get("correlationId");

        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                code,
                message,
                correlationId
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    public record ErrorResponse(
            OffsetDateTime timestamp,
            int status,
            String errorCode,
            String message,
            String correlationId
    ) { }

}
