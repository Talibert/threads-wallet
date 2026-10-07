package com.example.threadswallet.infra.controller.dto;

import com.example.threadswallet.application.dto.RiscoCalculadoDTO;
import com.example.threadswallet.domain.carteira.MetodoCalculo;

public record RiscoCalculadoResponse(
        Long id,
        MetodoCalculo tipo,
        Double valor
) {
    public static RiscoCalculadoResponse from(RiscoCalculadoDTO dto) {
        return new RiscoCalculadoResponse(
                dto.id(),
                dto.tipo(),
                dto.valor()
        );
    }
}
