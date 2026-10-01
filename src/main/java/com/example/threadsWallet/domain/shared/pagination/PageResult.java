package com.example.api_docker.domain.shared.pagination;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PageResult {
        items = items != null ? Collections.unmodifiableList(items) : List.of();
    }

    public boolean isFirst() {
        return page == 0;
    }

    public boolean isLast() {
        return totalPages == 0 || page >= totalPages - 1;
    }

    public boolean hasNext() {
        return page < totalPages - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mappedItems = this.items.stream().map(mapper).toList();
        return new PageResult<>(mappedItems, this.page, this.size, this.totalElements, this.totalPages);
    }

    public static <T> PageResult<T> empty(PaginationRequest pagination) {
        return new PageResult<>(List.of(), pagination.page(), pagination.size(), 0L, 0);
    }
}
