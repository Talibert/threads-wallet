package com.example.threadswallet.domain.carteira;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Contrato de domínio para estratégias de cálculo de risco de portfólios (Strategy Pattern).
 * Todas as implementações são stateless e definem como computar risco de forma atômica ou paralela.
 */
public interface CalculadoraRisco {

    /**
     * Executa o cálculo de risco de forma sequencial ou atômica para uma carteira.
     * Utilizado principalmente em processamento em lote de alto throughput.
     */
    Double calcularRisco(List<Ativo> ativos, int iteracoes);

    /**
     * Executa o cálculo de risco otimizado para latência sob demanda de uma única carteira.
     * Cada calculadora é livre para orquestrar paralelismo no pool de CPU conforme sua própria natureza matemática
     * (ex: Monte Carlo divide iterações em chunks; VaR Paramétrico despacha tarefa única de CPU).
     *
     * @param ativos Lista de ativos da carteira
     * @param iteracoes Número de iterações configuradas (se aplicável ao método)
     * @param cpuThreadPool Pool fixo de threads de CPU
     * @param threadsDisponiveis Quantidade de threads disponíveis no pool de CPU
     * @return Resultado contendo o valor do risco e o número de núcleos de CPU efetivamente utilizados
     */
    ResultadoCalculo calcularRiscoParalelo(List<Ativo> ativos, int iteracoes, ExecutorService cpuThreadPool, int threadsDisponiveis);

    MetodoCalculo getMetodo();
}
