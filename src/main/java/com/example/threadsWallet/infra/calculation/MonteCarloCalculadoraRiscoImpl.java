package com.example.threadswallet.infra.calculation;

import com.example.threadswallet.domain.carteira.AmostraRisco;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CalculadoraRisco;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementação stateless de cálculo de risco intensivo em CPU utilizando o método de Monte Carlo.
 * Não guarda nenhum estado interno. Suporta tanto cálculo sequencial quanto particionado via Map-Reduce.
 */
@Component
public class MonteCarloCalculadoraRiscoImpl implements CalculadoraRisco {

    @Override
    public Double calcularRisco(List<Ativo> ativos, int iteracoes) {
        AmostraRisco amostra = calcularAmostra(ativos, iteracoes);
        return consolidarAmostras(List.of(amostra));
    }

    @Override
    public boolean isParalelizavel() {
        return true;
    }

    @Override
    public AmostraRisco calcularAmostra(List<Ativo> ativos, int iteracoes) {
        if (ativos == null || ativos.isEmpty() || iteracoes <= 0)
            return new AmostraRisco(0.0, 0.0, 0);

        double valorTotalCarteira = 0.0;
        int n = ativos.size();
        double[] pesos = new double[n];
        double[] volatilidades = new double[n];

        for (int i = 0; i < n; i++) {
            Ativo ativo = ativos.get(i);
            valorTotalCarteira += ativo.getValorAtual();
        }

        if (valorTotalCarteira <= 0)
            return new AmostraRisco(0.0, 0.0, 0);

        for (int i = 0; i < n; i++) {
            Ativo ativo = ativos.get(i);
            pesos[i] = ativo.getValorAtual() / valorTotalCarteira;
            volatilidades[i] = ativo.getTaxaVolatilidade();
        }

        double somaPerdasSimuladas = 0.0;
        double somaQuadrados = 0.0;

        for (int step = 0; step < iteracoes; step++) {
            double choqueDiarioCarteira = 0.0;

            for (int i = 0; i < n; i++) {
                double choqueNormal = ThreadLocalRandom.current().nextGaussian();
                double choqueAtivo = choqueNormal * volatilidades[i];
                choqueDiarioCarteira += pesos[i] * choqueAtivo;
            }

            somaPerdasSimuladas += choqueDiarioCarteira;
            somaQuadrados += (choqueDiarioCarteira * choqueDiarioCarteira);
        }

        return new AmostraRisco(somaPerdasSimuladas, somaQuadrados, iteracoes);
    }

    @Override
    public Double consolidarAmostras(List<AmostraRisco> amostras) {
        if (amostras == null || amostras.isEmpty())
            return 0.0;

        double somaTotalPerdas = 0.0;
        double somaTotalQuadrados = 0.0;
        int totalIteracoes = 0;

        for (AmostraRisco amostra : amostras) {
            somaTotalPerdas += amostra.somaPerdas();
            somaTotalQuadrados += amostra.somaQuadrados();
            totalIteracoes += amostra.iteracoes();
        }

        if (totalIteracoes <= 0)
            return 0.0;

        double media = somaTotalPerdas / totalIteracoes;
        double variancia = (somaTotalQuadrados / totalIteracoes) - (media * media);
        double desvioPadrao = Math.sqrt(Math.max(0.0, variancia));
        double riscoVaR95 = 1.645 * desvioPadrao;

        return Math.round(riscoVaR95 * 10000.0) / 10000.0;
    }

    @Override
    public MetodoCalculo getMetodo() {
        return MetodoCalculo.MONTE_CARLO;
    }
}
