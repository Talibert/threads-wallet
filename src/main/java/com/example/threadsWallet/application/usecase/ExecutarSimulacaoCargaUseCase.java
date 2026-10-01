package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
public class ExecutarSimulacaoCargaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExecutarSimulacaoCargaUseCase.class);

    private final ProcessarCarteiraUseCase processarCarteiraUseCase;
    private final CarteiraRepository carteiraRepository;
    private final ExecutorService virtualThreadExecutor;
    private final int iteracoesMonteCarloPadrao;

    public ExecutarSimulacaoCargaUseCase(
            ProcessarCarteiraUseCase processarCarteiraUseCase,
            CarteiraRepository carteiraRepository,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor,
            @Value("${simulador.monte-carlo.iteracoes:100000}") int iteracoesMonteCarloPadrao
    ) {
        this.processarCarteiraUseCase = processarCarteiraUseCase;
        this.carteiraRepository = carteiraRepository;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.iteracoesMonteCarloPadrao = iteracoesMonteCarloPadrao;
    }

    public SimulacaoResult execute(Integer limite) {
        return execute(limite, null);
    }

    /**
     * Executa o cálculo de risco concorrente apenas para carteiras já cadastradas na base.
     * O número de iterações do Monte Carlo é parametrizado (não guardando estado interno).
     */
    public SimulacaoResult execute(Integer limite, Integer iteracoes) {
        List<Long> carteiraIds = carteiraRepository.findAllIds();

        if (carteiraIds.isEmpty())
            throw new DomainException("Nenhuma carteira encontrada no banco. Gere a massa de dados primeiro via POST /api/simulador/massa-dados.");

        if (limite != null && limite > 0 && limite < carteiraIds.size())
            carteiraIds = carteiraIds.subList(0, limite);

        int total = carteiraIds.size();
        int cores = Runtime.getRuntime().availableProcessors();
        int totalIteracoes = (iteracoes != null && iteracoes > 0) ? iteracoes : this.iteracoesMonteCarloPadrao;

        log.info(">>> INICIANDO CRONÔMETRO: Submetendo {} carteiras ({} iterações de Monte Carlo/carteira) ao executor de Virtual Threads...",
                total, totalIteracoes);
        long inicio = System.currentTimeMillis();

        // Cria uma thread virtual em cada iteração. Cada virtual thread chama o execute
        List<Future<?>> futures = new ArrayList<>(total);
        for (Long carteiraId : carteiraIds)
            futures.add(virtualThreadExecutor.submit(() -> processarCarteiraUseCase.execute(carteiraId, totalIteracoes)));

        // Aguarda a conclusão de todas as Virtual Threads
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Simulação de concorrência foi interrompida", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Erro ao processar carteira em Virtual Thread", e.getCause());
            }
        }

        long fim = System.currentTimeMillis();
        long tempoTotalMs = fim - inicio;
        double tempoMedioPorCarteira = (double) tempoTotalMs / total;

        String resumo = String.format(
                """
                \n================================================================================
                 🚀 SIMULAÇÃO DE CARGA CONCLUÍDA COM SUCESSO!
                ================================================================================
                 📊 Total de Carteiras Processadas: %d
                 ⏱️  Tempo Total Decorrido: %d ms (%.2f s)
                 ⚡ Tempo Médio por Carteira: %.2f ms
                 🧠 Núcleos de CPU (Pool Fixo): %d
                 🎲 Iterações de Monte Carlo por Carteira: %d
                 🧵 Gestão de I/O: %d Virtual Threads disparadas concorrentemente
                ================================================================================
                """,
                total, tempoTotalMs, (tempoTotalMs / 1000.0), tempoMedioPorCarteira, cores, totalIteracoes, total
        );

        System.out.println(resumo);
        log.info("Simulação concluída em {} ms", tempoTotalMs);

        return new SimulacaoResult(
                total,
                tempoTotalMs,
                tempoMedioPorCarteira,
                cores,
                totalIteracoes,
                "Simulação massiva de concorrência concluída com sucesso."
        );
    }
}
