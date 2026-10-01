package com.example.threadswallet.domain.carteira;

import com.example.threadswallet.domain.exception.DomainException;
import lombok.Getter;

@Getter
public class Ativo {

    private final Long id;
    private final Long carteiraId;
    private final String ticker;
    private final Double valorAtual;
    private final Double taxaVolatilidade;

    private Ativo(Long id, Long carteiraId, String ticker, Double valorAtual, Double taxaVolatilidade) {
        if (ticker == null || ticker.isBlank()) {
            throw new DomainException("O ticker do ativo não pode ser nulo ou vazio.");
        }
        if (valorAtual == null || valorAtual <= 0) {
            throw new DomainException("O valor atual do ativo deve ser maior que zero.");
        }
        if (taxaVolatilidade == null || taxaVolatilidade < 0) {
            throw new DomainException("A taxa de volatilidade não pode ser negativa.");
        }

        this.id = id;
        this.carteiraId = carteiraId;
        this.ticker = ticker.trim().toUpperCase();
        this.valorAtual = valorAtual;
        this.taxaVolatilidade = taxaVolatilidade;
    }

    public static Ativo create(Long carteiraId, String ticker, Double valorAtual, Double taxaVolatilidade) {
        return new Ativo(null, carteiraId, ticker, valorAtual, taxaVolatilidade);
    }

    public static Ativo restore(Long id, Long carteiraId, String ticker, Double valorAtual, Double taxaVolatilidade) {
        return new Ativo(id, carteiraId, ticker, valorAtual, taxaVolatilidade);
    }
}
