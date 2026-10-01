package com.example.threadswallet.infra.controller.dto;

import com.example.threadswallet.application.dto.CarteiraDTO;

public record CarteiraResponse(
        Long id,
        String nomeCliente,
        Double riscoCalculado,
        int quantidadeAtivos,
        double valorTotal
) {
    public static CarteiraResponse from(CarteiraDTO dto) {
        return new CarteiraResponse(
                dto.id(),
                dto.nomeCliente(),
                dto.riscoCalculado(),
                dto.quantidadeAtivos(),
                dto.valorTotal()
        );
    }
}
