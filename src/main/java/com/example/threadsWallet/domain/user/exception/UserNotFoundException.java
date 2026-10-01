package com.example.api_docker.domain.user.exception;

import com.example.api_docker.domain.user.UserId;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UserId userId) {
    }

    public UserNotFoundException() {
    }
}
