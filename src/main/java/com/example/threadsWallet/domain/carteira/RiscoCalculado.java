package com.example.threadswallet.domain.carteira;

import com.example.threadswallet.domain.exception.DomainException;
import lombok.Getter;

@Getter
public class RiscoCalculado {

    private final Long id;
    private final Long carteiraId;
    private Double valor;
    private final MetodoCalculo tipo;

    private RiscoCalculado(Long id, Long carteiraId, Double valor, MetodoCalculo tipo) {
        if (carteiraId == null)
            throw new DomainException("O ID da carteira é obrigatório.");
        if (valor == null || valor < 0)
            throw new DomainException("O valor do risco calculado não pode ser nulo ou negativo.");
        if (tipo == null)
            throw new DomainException("O tipo do método de cálculo de risco é obrigatório.");

        this.id = id;
        this.carteiraId = carteiraId;
        this.valor = valor;
        this.tipo = tipo;
    }

    public static RiscoCalculado create(Long carteiraId, Double valor, MetodoCalculo tipo) {
        return new RiscoCalculado(null, carteiraId, valor, tipo);
    }

    public static RiscoCalculado restore(Long id, Long carteiraId, Double valor, MetodoCalculo tipo) {
        return new RiscoCalculado(id, carteiraId, valor, tipo);
    }

    public void atualizarValor(Double novoValor) {
        if (novoValor == null || novoValor < 0)
            throw new DomainException("O valor do risco calculado não pode ser nulo ou negativo.");
        this.valor = novoValor;
    }
}
