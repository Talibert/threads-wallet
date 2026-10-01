package com.example.threadswallet.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ConcurrencyConfig {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyConfig.class);

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
     * Limitado estritamente à quantidade de núcleos do processador da máquina hospedeira.
     * Evita context switching excessivo e saturação descontrolada dos núcleos da CPU.
     */
    @Bean(name = "cpuThreadPool", destroyMethod = "close")
    public ExecutorService cpuThreadPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        log.info("🧠 [CONCORRÊNCIA] Inicializando Pool Fixo de CPU com {} threads nativas (1 por núcleo)...", cores);
        return Executors.newFixedThreadPool(
                cores,
                Thread.ofPlatform().name("cpu-worker-", 1).factory()
        );
    }
}
