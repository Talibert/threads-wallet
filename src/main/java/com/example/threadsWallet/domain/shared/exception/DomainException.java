package com.example.api_docker.domain.shared.exception;

public class DomainException extends RuntimeException{
    public DomainException(String message){
        super(message);
    }
}
