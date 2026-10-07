package com.example.threadswallet.infra.controller.dto;

import com.example.threadswallet.application.dto.CarteiraDTO;

import java.util.List;

public record CarteiraResponse(
        Long id,
        String nomeCliente,
        int quantidadeAtivos,
        double valorTotal,
        List<RiscoCalculadoResponse> riscos
) {
    public static CarteiraResponse from(CarteiraDTO dto) {
        List<RiscoCalculadoResponse> riscosResponse = (dto.riscos() != null)
                ? dto.riscos().stream().map(RiscoCalculadoResponse::from).toList()
                : List.of();

        return new CarteiraResponse(
                dto.id(),
                dto.nomeCliente(),
                dto.quantidadeAtivos(),
                dto.valorTotal(),
                riscosResponse
        );
    }
}
