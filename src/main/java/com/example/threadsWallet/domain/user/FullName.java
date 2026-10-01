package com.example.api_docker.domain.user;

import com.example.api_docker.domain.shared.exception.DomainException;

public record FullName(String firstName, String lastName) {

    public FullName {
        if (firstName == null || firstName.isBlank())
            throw new DomainException("Nome não pode ser vazio");

        if (lastName == null || lastName.isBlank())
            throw new DomainException("Sobrenome não pode ser vazio");

        firstName = firstName.trim();
        lastName = lastName.trim();
    }

    public String full() {
        return firstName + " " + lastName;
    }
}
