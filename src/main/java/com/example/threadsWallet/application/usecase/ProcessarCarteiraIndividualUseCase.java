package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.CarteiraIndividualResult;
import com.example.threadswallet.domain.carteira.*;
import com.example.threadswallet.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProcessarCarteiraIndividualUseCase {

    private final CarteiraRepository carteiraRepository;
    private final RiscoCalculadoRepository riscoCalculadoRepository;
    private final Map<MetodoCalculo, CalculadoraRisco> calculadoras;
    private final ExecutorService cpuThreadPool;
    private final int iteracoesPadrao;
    private final int threadsReservadas;

    public ProcessarCarteiraIndividualUseCase(
            CarteiraRepository carteiraRepository,
            RiscoCalculadoRepository riscoCalculadoRepository,
            List<CalculadoraRisco> calculadoras,
            @Qualifier("cpuThreadPool") ExecutorService cpuThreadPool,
            @Value("${simulador.monte-carlo.iteracoes:100000}") int iteracoesPadrao,
            @Value("${simulador.cpu-pool.threads-reservadas:2}") int threadsReservadas
    ) {
        this.carteiraRepository = carteiraRepository;
        this.riscoCalculadoRepository = riscoCalculadoRepository;
        this.calculadoras = calculadoras.stream()
                .collect(Collectors.toMap(CalculadoraRisco::getMetodo, Function.identity()));
        this.cpuThreadPool = cpuThreadPool;
        this.iteracoesPadrao = iteracoesPadrao;
        this.threadsReservadas = threadsReservadas;
    }

    /**
     * Processa o cálculo de risco de uma carteira individual sob demanda.
     * Se o método for paralelizável (ex: MONTE_CARLO), particiona o cálculo em todas as threads do pool de CPU.
     * Se o método for analítico (ex: VAR_PARAMETRICO), executa em uma única thread sem overhead.
     */
    public CarteiraIndividualResult execute(Long carteiraId, Integer iteracoes, MetodoCalculo metodo) {
        if (carteiraId == null)
            throw new DomainException("O ID da carteira é obrigatório.");

        if (metodo == null)
            throw new DomainException("O método de cálculo de risco é obrigatório.");

        CalculadoraRisco calculadora = calculadoras.get(metodo);

        if (calculadora == null)
            throw new DomainException("Nenhuma calculadora de risco encontrada para o método: " + metodo);

        List<Ativo> ativos = carteiraRepository.findAtivosByCarteiraId(carteiraId);

        if (ativos.isEmpty())
            throw new DomainException("Carteira com ID " + carteiraId + " não possui ativos ou não foi encontrada.");

        int totalIteracoes = (iteracoes != null && iteracoes > 0) ? iteracoes : this.iteracoesPadrao;
        long inicio = System.currentTimeMillis();

        Double riscoCalculado;
        int nucleosCpuUtilizados;

        try {
            if (!calculadora.isParalelizavel()) {
                nucleosCpuUtilizados = 1;
                Future<Double> future = cpuThreadPool.submit(() -> calculadora.calcularRisco(ativos, totalIteracoes));
                riscoCalculado = future.get();
            } else {
                int totalCores = Runtime.getRuntime().availableProcessors();
                int poolCores = Math.max(1, totalCores - threadsReservadas);
                int numChunks = Math.clamp(totalIteracoes, 1, poolCores);
                nucleosCpuUtilizados = numChunks;

                int iteracoesPorChunk = totalIteracoes / numChunks;
                int resto = totalIteracoes % numChunks;

                List<Future<AmostraRisco>> futures = new ArrayList<>(numChunks);
                for (int i = 0; i < numChunks; i++) {
                    int iteracoesDesteChunk = iteracoesPorChunk + (i == 0 ? resto : 0);
                    futures.add(cpuThreadPool.submit(() -> calculadora.calcularAmostra(ativos, iteracoesDesteChunk)));
                }

                List<AmostraRisco> amostras = new ArrayList<>(numChunks);
                for (Future<AmostraRisco> future : futures)
                    amostras.add(future.get());

                riscoCalculado = calculadora.consolidarAmostras(amostras);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processamento da carteira individual " + carteiraId + " foi interrompido", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao calcular risco da carteira individual " + carteiraId, e.getCause());
        }

        Optional<RiscoCalculado> existente = riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, metodo);
        if (existente.isPresent()) {
            RiscoCalculado risco = existente.get();
            risco.atualizarValor(riscoCalculado);
            riscoCalculadoRepository.salvar(risco);
        } else
            riscoCalculadoRepository.salvar(RiscoCalculado.create(carteiraId, riscoCalculado, metodo));

        long fim = System.currentTimeMillis();
        long tempoTotalMs = fim - inicio;

        return new CarteiraIndividualResult(
                carteiraId,
                riscoCalculado,
                metodo,
                tempoTotalMs,
                nucleosCpuUtilizados,
                totalIteracoes
        );
    }
}
