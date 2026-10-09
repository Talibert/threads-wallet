package com.example.threadswallet.infra.calculation;

import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CalculadoraRisco;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import com.example.threadswallet.domain.carteira.ResultadoCalculo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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
        if (ativos == null || ativos.isEmpty())
            return 0.0;

        double valorTotalCarteira = 0.0;
        for (Ativo ativo : ativos)
            valorTotalCarteira += ativo.getValorAtual();

        if (valorTotalCarteira <= 0)
            return 0.0;

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
    public ResultadoCalculo calcularRiscoParalelo(List<Ativo> ativos, int iteracoes, ExecutorService cpuThreadPool, int threadsDisponiveis) {
        // VaR Paramétrico analítico em O(N): o cálculo é instantâneo (< 0.001 ms).
        // Despachamos uma única tarefa para 1 thread nativa do pool de CPU, evitando overhead de concorrência.
        try {
            Future<Double> future = cpuThreadPool.submit(() -> calcularRisco(ativos, iteracoes));
            Double risco = future.get();
            return new ResultadoCalculo(risco, 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cálculo de VaR Paramétrico foi interrompido", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao calcular VaR Paramétrico", e.getCause());
        }
    }

    @Override
    public MetodoCalculo getMetodo() {
        return MetodoCalculo.VAR_PARAMETRICO;
    }
}
