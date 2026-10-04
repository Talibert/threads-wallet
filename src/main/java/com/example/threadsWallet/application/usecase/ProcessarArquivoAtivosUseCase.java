package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.ProcessamentoAtivosResult;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.AtivoRepository;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final CarteiraRepository carteiraRepository;
    private final AtivoRepository ativoRepository;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    public ProcessarArquivoAtivosUseCase(
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor,
            CarteiraRepository carteiraRepository,
            AtivoRepository ativoRepository,
            TransactionTemplate transactionTemplate,
            @Value("${simulador.upload.batch-size:1000}") int batchSize
    ) {
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.carteiraRepository = carteiraRepository;
        this.ativoRepository = ativoRepository;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = batchSize > 0 ? batchSize : 1000;
    }

    /**
     * Processa um fluxo de entrada de arquivo (.csv ou .txt) para uma carteira específica,
     * utilizando uma Virtual Thread para streaming de I/O e persistindo em lote em transação atômica.
     */
    public ProcessamentoAtivosResult execute(Long carteiraId, InputStream inputStream) {
        if (carteiraId == null)
            throw new DomainException("O ID da carteira é obrigatório.");

        if (inputStream == null)
            throw new DomainException("O fluxo de dados do arquivo não pode ser nulo.");

        if (!carteiraRepository.existsById(carteiraId))
            throw new DomainException(String.format("Carteira com ID %d não foi encontrada.", carteiraId));

        // A transação é iniciada DENTRO da Virtual Thread através do transactionTemplate,
        // garantindo que todo o processamento em lotes e rollback pertençam à mesma transação
        // com um único commit atômico no final.
        Future<ProcessamentoAtivosResult> future = virtualThreadExecutor.submit(() ->
                transactionTemplate.execute(status -> processarESalvarLotes(carteiraId, inputStream))
        );

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("O processamento do arquivo de ativos foi interrompido", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException)
                throw runtimeException;

            throw new RuntimeException("Erro ao processar o arquivo de ativos", e.getCause());
        }
    }

    private ProcessamentoAtivosResult processarESalvarLotes(Long carteiraId, InputStream inputStream) {
        long inicio = System.currentTimeMillis();
        List<Ativo> lote = new ArrayList<>(batchSize);
        int totalAtivosSalvos = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            int numeroLinha = 0;

            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                String linhaLimpa = linha.trim();

                if (linhaLimpa.isEmpty())
                    continue;

                // Detecta e ignora cabeçalho se houver
                if (numeroLinha == 1 && isCabecalho(linhaLimpa)) {
                    log.debug("Cabeçalho detectado e ignorado na linha 1: {}", linhaLimpa);
                    continue;
                }

                Ativo ativo = converterLinhaParaAtivo(linhaLimpa, numeroLinha, carteiraId);
                lote.add(ativo);

                if (lote.size() >= batchSize) {
                    ativoRepository.salvarTodos(List.copyOf(lote));
                    totalAtivosSalvos += lote.size();
                    lote.clear();
                }
            }

            // Salva o lote residual final
            if (!lote.isEmpty()) {
                ativoRepository.salvarTodos(List.copyOf(lote));
                totalAtivosSalvos += lote.size();
                lote.clear();
            }

        } catch (IOException e) {
            throw new RuntimeException("Falha de I/O ao ler o arquivo de ativos", e);
        }

        if (totalAtivosSalvos == 0)
            throw new DomainException("O arquivo não contém nenhum ativo válido para processamento.");

        long tempoTotalMs = System.currentTimeMillis() - inicio;
        log.info("Processamento e gravação de ativos concluídos para a carteira {}: {} ativos salvos em lote em {} ms.",
                carteiraId, totalAtivosSalvos, tempoTotalMs);

        return new ProcessamentoAtivosResult(
                totalAtivosSalvos,
                tempoTotalMs,
                String.format("%d ativos processados e salvos com sucesso.", totalAtivosSalvos)
        );
    }

    private boolean isCabecalho(String linha) {
        String lower = linha.toLowerCase();
        return lower.contains("ticker") || lower.contains("volatilidade") || lower.contains("valor");
    }

    private Ativo converterLinhaParaAtivo(String linha, int numeroLinha, Long carteiraId) {
        String[] partes = linha.split(";");
        if (partes.length != 3)
            throw new DomainException(String.format(
                    "Linha %d inválida: esperado exatamente 3 campos separados por ';' (ticker;valorAtual;taxaVolatilidade), mas recebeu: '%s'",
                    numeroLinha, linha
            ));

        try {
            String ticker = partes[0].trim();
            Double valorAtual = Double.parseDouble(partes[1].trim().replace(',', '.'));
            Double taxaVolatilidade = Double.parseDouble(partes[2].trim().replace(',', '.'));

            return Ativo.create(carteiraId, ticker, valorAtual, taxaVolatilidade);
        } catch (NumberFormatException e) {
            throw new DomainException(String.format(
                    "Linha %d com formato numérico inválido: '%s'", numeroLinha, linha
            ));
        }
    }
}
