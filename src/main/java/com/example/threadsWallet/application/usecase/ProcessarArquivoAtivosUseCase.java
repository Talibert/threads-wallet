package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.ProcessamentoAtivosResult;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
public class ProcessarArquivoAtivosUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarArquivoAtivosUseCase.class);

    private final ExecutorService virtualThreadExecutor;

    public ProcessarArquivoAtivosUseCase(
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor
    ) {
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Processa um fluxo de entrada de arquivo (.csv ou .txt) utilizando uma Virtual Thread
     * para streaming de I/O e instancia os agregados Ativo sem persistência.
     */
    public ProcessamentoAtivosResult execute(InputStream inputStream) {
        if (inputStream == null)
            throw new DomainException("O fluxo de dados do arquivo não pode ser nulo.");

        Future<ProcessamentoAtivosResult> future = virtualThreadExecutor.submit(() -> lerEInstanciarAtivos(inputStream));

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("O processamento do arquivo de ativos foi interrompido", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof DomainException domainException) {
                throw domainException;
            }
            throw new RuntimeException("Erro ao processar o arquivo de ativos", e.getCause());
        }
    }

    private ProcessamentoAtivosResult lerEInstanciarAtivos(InputStream inputStream) {
        long inicio = System.currentTimeMillis();
        List<Ativo> ativos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            int numeroLinha = 0;

            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                String linhaLimpa = linha.trim();

                if (linhaLimpa.isEmpty())
                    continue;

                // Detecta e ignora cabeçalho se houver (ex: carteira_id;ticker;valor_atual;taxa_volatilidade)
                if (numeroLinha == 1 && isCabecalho(linhaLimpa)) {
                    log.debug("Cabeçalho detectado e ignorado na linha 1: {}", linhaLimpa);
                    continue;
                }

                Ativo ativo = converterLinhaParaAtivo(linhaLimpa, numeroLinha);
                ativos.add(ativo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha de I/O ao ler o arquivo de ativos", e);
        }

        if (ativos.isEmpty())
            throw new DomainException("O arquivo não contém nenhum ativo válido para processamento.");

        long tempoTotalMs = System.currentTimeMillis() - inicio;
        log.info("Processamento de arquivo concluído: {} ativos instanciados em {} ms.", ativos.size(), tempoTotalMs);

        return new ProcessamentoAtivosResult(
                ativos.size(),
                tempoTotalMs,
                ativos,
                String.format("%d ativos processados e instanciados com sucesso.", ativos.size())
        );
    }

    private boolean isCabecalho(String linha) {
        String lower = linha.toLowerCase();
        return lower.contains("ticker") || lower.contains("carteira") || lower.contains("volatilidade");
    }

    private Ativo converterLinhaParaAtivo(String linha, int numeroLinha) {
        String[] partes = linha.split(";");
        if (partes.length < 4) {
            throw new DomainException(String.format(
                    "Linha %d inválida: esperado 4 campos separados por ';' (carteiraId;ticker;valorAtual;taxaVolatilidade), mas recebeu: '%s'",
                    numeroLinha, linha
            ));
        }

        try {
            Long carteiraId = Long.parseLong(partes[0].trim());
            String ticker = partes[1].trim();
            Double valorAtual = Double.parseDouble(partes[2].trim().replace(',', '.'));
            Double taxaVolatilidade = Double.parseDouble(partes[3].trim().replace(',', '.'));

            return Ativo.create(carteiraId, ticker, valorAtual, taxaVolatilidade);
        } catch (NumberFormatException e) {
            throw new DomainException(String.format(
                    "Linha %d com formato numérico inválido: '%s'", numeroLinha, linha
            ));
        }
    }
}
