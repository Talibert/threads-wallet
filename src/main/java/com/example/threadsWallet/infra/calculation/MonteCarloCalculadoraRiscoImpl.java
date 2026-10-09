package com.example.threadswallet.infra.calculation;

import com.example.threadswallet.domain.carteira.AmostraRisco;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CalculadoraRisco;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import com.example.threadswallet.domain.carteira.ResultadoCalculo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
    public ResultadoCalculo calcularRiscoParalelo(List<Ativo> ativos, int iteracoes, ExecutorService cpuThreadPool, int threadsDisponiveis) {
        if (ativos == null || ativos.isEmpty() || iteracoes <= 0)
            return new ResultadoCalculo(0.0, 1);

        // Caso Monte Carlo: Estratégia estocástica perfeitamente paralelizável via Map-Reduce.
        // Dividimos as iterações estocásticas em fatias (chunks) para saturar os núcleos disponíveis do pool de CPU.

        // Passo 1: Define o número de chunks (fatias).
        // Não faz sentido criar mais chunks do que threads de CPU disponíveis, nem mais chunks do que iterações totais.
        int numChunks = Math.clamp(iteracoes, 1, Math.max(1, threadsDisponiveis));

        // Passo 2: Particionamento das iterações entre os chunks.
        // Divide as iterações igualmente e distribui o resto da divisão inteira no primeiro chunk,
        // garantindo que a soma dos chunks seja exatamente igual ao total de iterações configurado.
        int iteracoesPorChunk = iteracoes / numChunks;
        int resto = iteracoes % numChunks;

        // Passo 3 (MAP): Submete cada fatia de iterações concorrentemente ao pool fixo de threads de CPU.
        List<Future<AmostraRisco>> futures = new ArrayList<>(numChunks);
        for (int i = 0; i < numChunks; i++) {
            int iteracoesDesteChunk = iteracoesPorChunk + (i == 0 ? resto : 0);
            futures.add(cpuThreadPool.submit(() -> calcularAmostra(ativos, iteracoesDesteChunk)));
        }

        try {
            // Passo 4: Coleta os resultados parciais conforme concluem.
            // A thread chamadora (Virtual Thread) suspende em future.get() sem travar threads de SO.
            List<AmostraRisco> amostras = new ArrayList<>(numChunks);
            for (Future<AmostraRisco> future : futures)
                amostras.add(future.get());

            // Passo 5 (REDUCE): Consolida as amostras parciais em um resultado estatístico global unificado.
            Double risco = consolidarAmostras(amostras);
            return new ResultadoCalculo(risco, numChunks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cálculo de Monte Carlo paralelo foi interrompido", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro no cálculo paralelo de Monte Carlo", e.getCause());
        }
    }

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
