package com.taskhub.auth.application.port.out;

public interface PasswordHasherPort {

    String hash(String rawPassword);
}
