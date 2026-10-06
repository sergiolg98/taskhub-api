package com.taskhub.application.port.out;

public interface PasswordHasherPort {

    String hash(String rawPassword);
}
