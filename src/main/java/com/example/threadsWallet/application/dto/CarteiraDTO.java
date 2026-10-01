package com.example.threadswallet.application.dto;

import com.example.threadswallet.domain.carteira.Carteira;

public record CarteiraDTO(
        Long id,
        String nomeCliente,
        Double riscoCalculado,
        int quantidadeAtivos,
        double valorTotal
) {
    public static CarteiraDTO fromDomain(Carteira carteira) {
        return new CarteiraDTO(
                carteira.getId(),
                carteira.getNomeCliente(),
                carteira.getRiscoCalculado(),
                carteira.getAtivos().size(),
                carteira.getValorTotal()
        );
    }
}
