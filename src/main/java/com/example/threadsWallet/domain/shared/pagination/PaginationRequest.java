package com.example.api_docker.domain.shared.pagination;

public record PaginationRequest(
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT_DIRECTION = "ASC";

    public PaginationRequest {
        if (page < 0)
            page = DEFAULT_PAGE;
        if (size <= 0)
            size = DEFAULT_SIZE;
        if (size > MAX_SIZE)
            size = MAX_SIZE;
        if (sortDirection == null || (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC"))) {
            sortDirection = DEFAULT_SORT_DIRECTION;
        } else {
            sortDirection = sortDirection.toUpperCase();
        }
    }

    public static PaginationRequest of(int page, int size) {
        return new PaginationRequest(page, size, null, DEFAULT_SORT_DIRECTION);
    }

    public static PaginationRequest of(int page, int size, String sortBy, String sortDirection) {
        return new PaginationRequest(page, size, sortBy, sortDirection);
    }
}
