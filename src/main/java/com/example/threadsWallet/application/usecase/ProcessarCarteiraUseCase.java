package com.example.threadswallet.application.usecase;

import com.example.threadswallet.domain.carteira.*;
import com.example.threadswallet.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProcessarCarteiraUseCase {

    private final CarteiraRepository carteiraRepository;
    private final RiscoCalculadoRepository riscoCalculadoRepository;
    private final Map<MetodoCalculo, CalculadoraRisco> calculadoras;
    private final ExecutorService cpuThreadPool;

    public ProcessarCarteiraUseCase(
            CarteiraRepository carteiraRepository,
            RiscoCalculadoRepository riscoCalculadoRepository,
            List<CalculadoraRisco> calculadoras,
            @Qualifier("cpuThreadPool") ExecutorService cpuThreadPool
    ) {
        this.carteiraRepository = carteiraRepository;
        this.riscoCalculadoRepository = riscoCalculadoRepository;
        this.calculadoras = calculadoras.stream()
                .collect(Collectors.toMap(CalculadoraRisco::getMetodo, Function.identity()));
        this.cpuThreadPool = cpuThreadPool;
    }

    /**
     * Executado dentro de uma Virtual Thread exclusiva para cada carteira.
     * Recebe o Value Object ParametrosCalculo contendo o método e iterações.
     */
    public Double execute(Long carteiraId, ParametrosCalculo parametros) {
        if (parametros == null)
            throw new DomainException("Os parâmetros de cálculo de risco são obrigatórios.");

        CalculadoraRisco calculadora = calculadoras.get(parametros.metodo());
        if (calculadora == null)
            throw new DomainException("Nenhuma calculadora de risco encontrada para o método: " + parametros.metodo());

        List<Ativo> ativos = carteiraRepository.findAtivosByCarteiraId(carteiraId);

        if (ativos.isEmpty())
            throw new DomainException("Carteira com ID " + carteiraId + " não possui ativos ou não foi encontrada.");

        Double riscoCalculado;
        try {
            // A virtual thread chama uma thread de cpu do pool e essa thread roda o calcularRisco
            Future<Double> futureCalculo = cpuThreadPool.submit(() -> calculadora.calcularRisco(ativos, parametros.iteracoes()));
            // A virtual thread é bloqueada de forma barata e fica esperando a cpu thread terminar
            riscoCalculado = futureCalculo.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processamento da carteira " + carteiraId + " foi interrompido", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao calcular risco da carteira " + carteiraId, e.getCause());
        }

        Optional<RiscoCalculado> existente = riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, parametros.metodo());
        if (existente.isPresent()) {
            RiscoCalculado risco = existente.get();
            risco.atualizarValor(riscoCalculado);
            riscoCalculadoRepository.salvar(risco);
        } else
            riscoCalculadoRepository.salvar(RiscoCalculado.create(carteiraId, riscoCalculado, parametros.metodo()));

        return riscoCalculado;
    }
}
