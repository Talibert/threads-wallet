package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
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
    private final int threadsReservadas;

    public ExecutarSimulacaoCargaUseCase(
            ProcessarCarteiraUseCase processarCarteiraUseCase,
            CarteiraRepository carteiraRepository,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor,
            @Value("${simulador.monte-carlo.iteracoes:100000}") int iteracoesMonteCarloPadrao,
            @Value("${simulador.cpu-pool.threads-reservadas:2}") int threadsReservadas
    ) {
        this.processarCarteiraUseCase = processarCarteiraUseCase;
        this.carteiraRepository = carteiraRepository;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.iteracoesMonteCarloPadrao = iteracoesMonteCarloPadrao;
        this.threadsReservadas = threadsReservadas;
    }

    /**
     * Executa o cálculo de risco concorrente apenas para carteiras já cadastradas na base.
     * O método de cálculo é obrigatório (Strategy Pattern) e o número de iterações é parametrizado.
     */
    public SimulacaoResult execute(Integer limite, Integer iteracoes, MetodoCalculo metodo) {
        if (metodo == null)
            throw new DomainException("O método de cálculo de risco é obrigatório.");

        List<Long> carteiraIds = carteiraRepository.findAllIds();

        if (carteiraIds.isEmpty())
            throw new DomainException("Nenhuma carteira encontrada no banco. Gere a massa de dados primeiro via POST /api/simulador/massa-dados.");

        if (limite != null && limite > 0 && limite < carteiraIds.size())
            carteiraIds = carteiraIds.subList(0, limite);

        int total = carteiraIds.size();
        int totalCores = Runtime.getRuntime().availableProcessors();
        int poolCores = Math.max(1, totalCores - threadsReservadas);
        int totalIteracoes = (iteracoes != null && iteracoes > 0) ? iteracoes : this.iteracoesMonteCarloPadrao;

        log.info(">>> INICIANDO CRONÔMETRO: Submetendo {} carteiras (Método: {}, {} iterações) ao executor de Virtual Threads...",
                total, metodo, totalIteracoes);
        long inicio = System.currentTimeMillis();

        // Cria uma thread virtual em cada iteração. Cada virtual thread chama o execute
        List<Future<?>> futures = new ArrayList<>(total);
        for (Long carteiraId : carteiraIds)
            futures.add(virtualThreadExecutor.submit(() -> processarCarteiraUseCase.execute(carteiraId, totalIteracoes, metodo)));

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
                 🏷️  Método de Cálculo: %s
                 ⏱️  Tempo Total Decorrido: %d ms (%.2f s)
                 ⚡ Tempo Médio por Carteira: %.2f ms
                 🧠 Núcleos de CPU (Pool Fixo): %d (de %d núcleos da máquina, %d reservados)
                 🎲 Iterações Parametrizadas: %d
                 🧵 Gestão de I/O: %d Virtual Threads disparadas concorrentemente
                ================================================================================
                """,
                total, metodo, tempoTotalMs, (tempoTotalMs / 1000.0), tempoMedioPorCarteira, poolCores, totalCores, threadsReservadas, totalIteracoes, total
        );

        System.out.println(resumo);
        log.info("Simulação concluída em {} ms", tempoTotalMs);

        return new SimulacaoResult(
                total,
                tempoTotalMs,
                tempoMedioPorCarteira,
                poolCores,
                totalIteracoes,
                metodo,
                "Simulação massiva de concorrência concluída com sucesso."
        );
    }
}
