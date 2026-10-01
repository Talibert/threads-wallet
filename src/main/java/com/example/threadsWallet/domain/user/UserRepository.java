package com.example.api_docker.domain.user;

import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;

import java.util.Optional;

public interface UserRepository {
    boolean existsByEmail(Email email);
    void save(User admin);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    PageResult<User> findAll(PaginationRequest pagination);
}
