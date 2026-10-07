package com.example.threadswallet.application.dto;

import com.example.threadswallet.domain.carteira.MetodoCalculo;
import com.example.threadswallet.domain.carteira.RiscoCalculado;

public record RiscoCalculadoDTO(
        Long id,
        MetodoCalculo tipo,
        Double valor
) {
    public static RiscoCalculadoDTO fromDomain(RiscoCalculado domain) {
        return new RiscoCalculadoDTO(
                domain.getId(),
                domain.getTipo(),
                domain.getValor()
        );
    }
}
