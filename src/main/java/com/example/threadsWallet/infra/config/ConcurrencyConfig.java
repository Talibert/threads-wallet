package com.example.threadswallet.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ConcurrencyConfig {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyConfig.class);

    private final int threadsReservadas;

    public ConcurrencyConfig(
            @Value("${simulador.cpu-pool.threads-reservadas:2}") int threadsReservadas
    ) {
        this.threadsReservadas = threadsReservadas;
    }

    /**
     * Pool de Virtual Threads para gestão de I/O (banco de dados, rede).
     * Cria uma nova Virtual Thread sob demanda para cada tarefa submetida.
     * Levíssima, escalável para dezenas de milhares de tarefas simultâneas sem consumir threads do SO.
     */
    @Bean(name = "virtualThreadExecutor", destroyMethod = "close")
    public ExecutorService virtualThreadExecutor() {
        log.info("🧵 [CONCORRÊNCIA] Inicializando Executor de Virtual Threads por Tarefa (Java 21)...");
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("vt-carteira-io-", 1).factory()
        );
    }

    /**
     * Pool de Threads Tradicionais (Platform Threads) de tamanho fixo para cargas de CPU.
     * Dimensionado reservando núcleos para o SO, JVM (Garbage Collector e JIT) e Carrier Threads de I/O.
     * Evita context switching excessivo e saturação descontrolada dos núcleos da CPU.
     */
    @Bean(name = "cpuThreadPool", destroyMethod = "close")
    public ExecutorService cpuThreadPool() {
        int totalCores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(1, totalCores - threadsReservadas);
        log.info("🧠 [CONCORRÊNCIA] Inicializando Pool Fixo de CPU com {} threads nativas (total da máquina: {}, {} reservadas para SO/JVM/I/O)...",
                poolSize, totalCores, threadsReservadas);
        return Executors.newFixedThreadPool(
                poolSize,
                Thread.ofPlatform().name("cpu-worker-", 1).factory()
        );
    }
}
