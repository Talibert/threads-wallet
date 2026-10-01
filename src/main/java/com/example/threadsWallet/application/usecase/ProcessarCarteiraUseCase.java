package com.example.threadswallet.application.usecase;

import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CalculadoraRisco;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
public class ProcessarCarteiraUseCase {

    private final CarteiraRepository carteiraRepository;
    private final CalculadoraRisco calculadoraRisco;
    private final ExecutorService cpuThreadPool;

    public ProcessarCarteiraUseCase(
            CarteiraRepository carteiraRepository,
            CalculadoraRisco calculadoraRisco,
            @Qualifier("cpuThreadPool") ExecutorService cpuThreadPool
    ) {
        this.carteiraRepository = carteiraRepository;
        this.calculadoraRisco = calculadoraRisco;
        this.cpuThreadPool = cpuThreadPool;
    }

    /**
     * Executado dentro de uma Virtual Thread exclusiva.
     */
    public Double execute(Long carteiraId, int iteracoes) {
        List<Ativo> ativos = carteiraRepository.findAtivosByCarteiraId(carteiraId);

        if (ativos.isEmpty())
            throw new DomainException("Carteira com ID " + carteiraId + " não possui ativos ou não foi encontrada.");

        Double riscoCalculado;
        try {
            // A virtual thread chama uma thread de cpu do pool e essa thread roda o calcularRisco
            Future<Double> futureCalculo = cpuThreadPool.submit(() -> calculadoraRisco.calcularRisco(ativos, iteracoes));
            riscoCalculado = futureCalculo.get(); // Bloqueio barato na Virtual Thread
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processamento da carteira " + carteiraId + " foi interrompido", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao calcular risco da carteira " + carteiraId, e.getCause());
        }

        carteiraRepository.atualizarRisco(carteiraId, riscoCalculado);

        return riscoCalculado;
    }
}
