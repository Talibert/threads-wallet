package com.example.threadswallet.application.dto;

import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.RiscoCalculado;

import java.util.List;

public record CarteiraDTO(
        Long id,
        String nomeCliente,
        int quantidadeAtivos,
        double valorTotal,
        List<RiscoCalculadoDTO> riscos
) {
    public static CarteiraDTO fromDomain(Carteira carteira, List<RiscoCalculado> riscos) {
        List<RiscoCalculadoDTO> riscosDTO = (riscos != null)
                ? riscos.stream().map(RiscoCalculadoDTO::fromDomain).toList()
                : List.of();

        return new CarteiraDTO(
                carteira.getId(),
                carteira.getNomeCliente(),
                carteira.getAtivos().size(),
                carteira.getValorTotal(),
                riscosDTO
        );
    }

    public static CarteiraDTO fromDomain(Carteira carteira) {
        return fromDomain(carteira, List.of());
    }
}
