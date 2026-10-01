package com.example.api_docker.infra.controller.response;

import com.example.api_docker.domain.shared.pagination.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Envelope de resposta paginada genérica")
public record PageResponse<T>(
        @Schema(description = "Lista de elementos da página atual")
        List<T> content,

        @Schema(description = "Número da página atual (0-indexada)", example = "0")
        int page,

        @Schema(description = "Quantidade de elementos por página", example = "10")
        int size,

        @Schema(description = "Total de registros encontrados em todas as páginas", example = "42")
        long totalElements,

        @Schema(description = "Total de páginas disponíveis", example = "5")
        int totalPages,

        @Schema(description = "Indica se é a primeira página", example = "true")
        boolean isFirst,

        @Schema(description = "Indica se é a última página", example = "false")
        boolean isLast
) {
    public static <S, T> PageResponse<T> from(PageResult<S> pageResult, Function<S, T> mapper) {
        List<T> mappedContent = pageResult.items().stream().map(mapper).toList();
        return new PageResponse<>(
                mappedContent,
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    public static <T> PageResponse<T> from(PageResult<T> pageResult) {
        return new PageResponse<>(
                pageResult.items(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }
}
