package com.shopflow.user.service.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID uuid) {
        super("User not found: " + uuid);
    }

    public UserNotFoundException(String email)
    {
        super("User not found: " + email);
    }
}
