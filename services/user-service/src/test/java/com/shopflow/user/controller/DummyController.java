package com.shopflow.user.controller;

import com.shopflow.user.dto.UserRequest;
import com.shopflow.user.service.exception.UserAlreadyExistsException;
import com.shopflow.user.service.exception.UserNotFoundException;
import jakarta.validation.Valid;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/test")
public class DummyController {

    private final String userUUID = "00000000-0000-0000-0000-000000000001";
    private final String userEmail = "sample@gmail.com";

    @GetMapping
    public String ok() { return "ok"; }

    @GetMapping("/error")
    public String error() { throw new RuntimeException("Boom!"); }

    @GetMapping("/user-not-found-uuid")
    public String userNotFoundUUID() { throw new UserNotFoundException(userUUID); }

    @GetMapping("/user-already-exists-email")
    public String userAlreadyExists() { throw new UserAlreadyExistsException(userEmail); }

    @PostMapping("/validation")
    public void createUserValidation(@Valid @RequestBody UserRequest request) {}

    @GetMapping("/internal-error")
    public void createUserValidation() {}

    @GetMapping("/user-not-found-email")
    public String userNotFoundEmail() { throw new UserNotFoundException(userEmail); }

    @GetMapping("/delay")
    public String delay() throws InterruptedException {
        Thread.sleep(50);
        return "delayed";
    }

    @GetMapping("/noheader")
    public String noHeader() { return "headerless"; }
}
