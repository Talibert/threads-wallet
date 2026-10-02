package com.example.threadswallet.infra.calculation;

import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CalculadoraRisco;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação stateless de cálculo de risco analítico utilizando VaR Paramétrico (Variância-Covariância).
 * Não executa laços estocásticos pesados; realiza o cálculo direto em O(N).
 */
@Component
public class VarParametricoCalculadoraRiscoImpl implements CalculadoraRisco {

    // Nível de confiança de 95% para distribuição normal (z = 1.645)
    private static final double Z_SCORE_95 = 1.645;

    @Override
    public Double calcularRisco(List<Ativo> ativos, int iteracoes) {
        if (ativos == null || ativos.isEmpty()) {
            return 0.0;
        }

        double valorTotalCarteira = 0.0;
        for (Ativo ativo : ativos) {
            valorTotalCarteira += ativo.getValorAtual();
        }

        if (valorTotalCarteira <= 0) {
            return 0.0;
        }

        // Variância da carteira: soma ponderada das variâncias individuais dos ativos
        double somaVarianciasPonderadas = 0.0;
        for (Ativo ativo : ativos) {
            double peso = ativo.getValorAtual() / valorTotalCarteira;
            double volatilidade = ativo.getTaxaVolatilidade();
            double riscoComponente = peso * volatilidade;
            somaVarianciasPonderadas += (riscoComponente * riscoComponente);
        }

        // Desvio padrão analítico da carteira
        double desvioPadraoCarteira = Math.sqrt(somaVarianciasPonderadas);

        // VaR Paramétrico a 95% de confiança
        double varParametrico = Z_SCORE_95 * desvioPadraoCarteira;

        return Math.round(varParametrico * 10000.0) / 10000.0;
    }

    @Override
    public MetodoCalculo getMetodo() {
        return MetodoCalculo.VAR_PARAMETRICO;
    }
}
