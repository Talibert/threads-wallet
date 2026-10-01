package com.example.api_docker.domain.user;

import com.example.api_docker.domain.shared.exception.DomainException;

public record Email(String value) {

    public Email {
        if (value == null || !value.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$"))
            throw new DomainException("Email inválido: " + value);

        value = value.toLowerCase();
    }
}
