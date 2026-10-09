package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.CarteiraIndividualResult;
import com.example.threadswallet.domain.carteira.*;
import com.example.threadswallet.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProcessarCarteiraIndividualUseCase {

    private final CarteiraRepository carteiraRepository;
    private final RiscoCalculadoRepository riscoCalculadoRepository;
    private final Map<MetodoCalculo, CalculadoraRisco> calculadoras;
    private final ExecutorService cpuThreadPool;
    private final int threadsReservadas;

    public ProcessarCarteiraIndividualUseCase(
            CarteiraRepository carteiraRepository,
            RiscoCalculadoRepository riscoCalculadoRepository,
            List<CalculadoraRisco> calculadoras,
            @Qualifier("cpuThreadPool") ExecutorService cpuThreadPool,
            @Value("${simulador.cpu-pool.threads-reservadas:2}") int threadsReservadas
    ) {
        this.carteiraRepository = carteiraRepository;
        this.riscoCalculadoRepository = riscoCalculadoRepository;
        this.calculadoras = calculadoras.stream()
                .collect(Collectors.toMap(CalculadoraRisco::getMetodo, Function.identity()));
        this.cpuThreadPool = cpuThreadPool;
        this.threadsReservadas = threadsReservadas;
    }

    /**
     * Processa o cálculo de risco de uma carteira individual sob demanda.
     * Recebe o Value Object ParametrosCalculo contendo o método e iterações.
     */
    public CarteiraIndividualResult execute(Long carteiraId, ParametrosCalculo parametros) {
        if (carteiraId == null)
            throw new DomainException("O ID da carteira é obrigatório.");

        if (parametros == null)
            throw new DomainException("Os parâmetros de cálculo de risco são obrigatórios.");

        CalculadoraRisco calculadora = calculadoras.get(parametros.metodo());
        if (calculadora == null)
            throw new DomainException("Nenhuma calculadora de risco encontrada para o método: " + parametros.metodo());

        List<Ativo> ativos = carteiraRepository.findAtivosByCarteiraId(carteiraId);

        if (ativos.isEmpty())
            throw new DomainException("Carteira com ID " + carteiraId + " não possui ativos ou não foi encontrada.");

        long inicio = System.currentTimeMillis();

        // 1. Identifica a capacidade do pool fixo de CPU respeitando as threads reservadas para SO/JVM/Carrier
        int totalCores = Runtime.getRuntime().availableProcessors();
        int poolCores = Math.max(1, totalCores - threadsReservadas);

        // 2. Delega a execução paralela para a calculadora selecionada (Strategy Pattern).
        // Cada estratégia decide sua própria política de concorrência (ex: Monte Carlo faz Map-Reduce particionando iterações;
        // VaR Paramétrico despacha uma única tarefa de CPU sem concorrência desnecessária).
        ResultadoCalculo resultado = calculadora.calcularRiscoParalelo(
                ativos,
                parametros.iteracoes(),
                cpuThreadPool,
                poolCores
        );

        Double riscoCalculado = resultado.valor();
        int nucleosCpuUtilizados = resultado.nucleosUtilizados();

        Optional<RiscoCalculado> existente = riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, parametros.metodo());
        if (existente.isPresent()) {
            RiscoCalculado risco = existente.get();
            risco.atualizarValor(riscoCalculado);
            riscoCalculadoRepository.salvar(risco);
        } else
            riscoCalculadoRepository.salvar(RiscoCalculado.create(carteiraId, riscoCalculado, parametros.metodo()));

        long fim = System.currentTimeMillis();
        long tempoTotalMs = fim - inicio;

        return new CarteiraIndividualResult(
                carteiraId,
                riscoCalculado,
                parametros.metodo(),
                tempoTotalMs,
                nucleosCpuUtilizados,
                parametros.iteracoes()
        );
    }
}
