package com.example.api_docker.application.user.query;

import com.example.api_docker.domain.shared.pagination.PaginationRequest;

public record ListUsersQuery(PaginationRequest pagination) {
    public ListUsersQuery {
        if (pagination == null) {
            pagination = PaginationRequest.of(0, 10);
        }
    }
}
