package com.example.threadswallet.infra.calculation;

import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CalculadoraRisco;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementação stateless de cálculo de risco intensivo em CPU utilizando o método de Monte Carlo.
 * Não guarda nenhum estado interno. O número de iterações é fornecido estritamente via parâmetro.
 */
@Component
public class MonteCarloCalculadoraRiscoImpl implements CalculadoraRisco {

    @Override
    public Double calcularRisco(List<Ativo> ativos, int iteracoes) {
        if (ativos == null || ativos.isEmpty() || iteracoes <= 0) {
            return 0.0;
        }

        double valorTotalCarteira = 0.0;
        int n = ativos.size();
        double[] pesos = new double[n];
        double[] volatilidades = new double[n];

        for (int i = 0; i < n; i++) {
            Ativo ativo = ativos.get(i);
            valorTotalCarteira += ativo.getValorAtual();
        }

        if (valorTotalCarteira <= 0) {
            return 0.0;
        }

        for (int i = 0; i < n; i++) {
            Ativo ativo = ativos.get(i);
            pesos[i] = ativo.getValorAtual() / valorTotalCarteira;
            volatilidades[i] = ativo.getTaxaVolatilidade();
        }

        double somaPerdasSimuladas = 0.0;
        double somaQuadrados = 0.0;

        // Laço de iterações esgotando CPU (fornecido via parâmetro)
        for (int step = 0; step < iteracoes; step++) {
            double choqueDiarioCarteira = 0.0;

            for (int i = 0; i < n; i++) {
                // Choque normal com média 0 e desvio padrão igual à volatilidade do ativo
                double choqueNormal = ThreadLocalRandom.current().nextGaussian();
                double choqueAtivo = choqueNormal * volatilidades[i];
                choqueDiarioCarteira += pesos[i] * choqueAtivo;
            }

            somaPerdasSimuladas += choqueDiarioCarteira;
            somaQuadrados += (choqueDiarioCarteira * choqueDiarioCarteira);
        }

        // Variância e desvio padrão amostral dos retornos simulados
        double media = somaPerdasSimuladas / iteracoes;
        double variancia = (somaQuadrados / iteracoes) - (media * media);
        double desvioPadrao = Math.sqrt(Math.max(0.0, variancia));

        // Risco calculado: VaR paramétrico simplificado para 95% de confiança (1.645 * desvio padrão)
        double riscoVaR95 = 1.645 * desvioPadrao;

        return Math.round(riscoVaR95 * 10000.0) / 10000.0;
    }

    @Override
    public MetodoCalculo getMetodo() {
        return MetodoCalculo.MONTE_CARLO;
    }
}
