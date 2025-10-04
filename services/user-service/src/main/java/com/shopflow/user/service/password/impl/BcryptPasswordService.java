package com.shopflow.user.service.password.impl;

import com.shopflow.user.service.password.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BcryptPasswordService implements PasswordService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encrypt(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean verify(String password, String encryptedPassword) {
        return passwordEncoder.matches(password, encryptedPassword);
    }
}
