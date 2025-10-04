package com.shopflow.user.service.password;

public interface PasswordService {
    String encrypt(String password);
    boolean verify(String password, String encryptedPassword);
}
