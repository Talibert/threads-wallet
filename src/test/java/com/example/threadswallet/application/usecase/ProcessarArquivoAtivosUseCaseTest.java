package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.application.dto.ProcessamentoAtivosResult;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.exception.DomainException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProcessarArquivoAtivosUseCaseTest extends UnitAbstractTests {

    @Spy
    private ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private ProcessarArquivoAtivosUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessarArquivoAtivosUseCase(virtualThreadExecutor);
    }

    @AfterEach
    void tearDown() {
        virtualThreadExecutor.close();
    }

    @Test
    @DisplayName("Deve processar arquivo CSV com cabeçalho e instanciar ativos corretamente")
    void deveProcessarArquivoCsvComCabecalho() {
        String csv = """
                carteira_id;ticker;valor_atual;taxa_volatilidade
                1;PETR4;38.50;0.22
                1;VALE3;62,10;0.18
                2;ITUB4;34.20;0.15
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ProcessamentoAtivosResult result = useCase.execute(stream);

        assertNotNull(result);
        assertEquals(3, result.totalAtivosProcessados());
        assertEquals(3, result.ativos().size());

        Ativo petr4 = result.ativos().get(0);
        assertEquals(1L, petr4.getCarteiraId());
        assertEquals("PETR4", petr4.getTicker());
        assertEquals(38.50, petr4.getValorAtual());
        assertEquals(0.22, petr4.getTaxaVolatilidade());

        Ativo vale3 = result.ativos().get(1);
        assertEquals(62.10, vale3.getValorAtual());
    }

    @Test
    @DisplayName("Deve processar arquivo TXT sem cabeçalho e com linhas em branco ignoradas")
    void deveProcessarArquivoTxtSemCabecalho() {
        String txt = """
                10;BBAS3;28.50;0.20

                10;BBDC4;14.30;0.19
                """;
        InputStream stream = new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8));

        ProcessamentoAtivosResult result = useCase.execute(stream);

        assertEquals(2, result.totalAtivosProcessados());
        assertEquals("BBAS3", result.ativos().get(0).getTicker());
        assertEquals("BBDC4", result.ativos().get(1).getTicker());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando a linha tiver menos de 4 campos")
    void deveLancarExcecaoQuandoLinhaTiverCamposInsuficientes() {
        String csv = """
                carteira_id;ticker;valor_atual;taxa_volatilidade
                1;PETR4;38.50
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(stream));
        assertThat(ex.getMessage()).contains("Linha 2 inválida: esperado 4 campos");
    }

    @Test
    @DisplayName("Deve lançar DomainException quando campo numérico for inválido")
    void deveLancarExcecaoQuandoCampoNumericoForInvalido() {
        String csv = """
                1;PETR4;trinta_reais;0.22
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(stream));
        assertThat(ex.getMessage()).contains("Linha 1 com formato numérico inválido");
    }

    @Test
    @DisplayName("Deve lançar DomainException quando ativo violar invariante de negócio (valor menor ou igual a zero)")
    void deveLancarExcecaoQuandoAtivoViolarInvariante() {
        String csv = """
                1;PETR4;0.0;0.22
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(stream));
        assertEquals("O valor atual do ativo deve ser maior que zero.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando arquivo não contiver ativos válidos")
    void deveLancarExcecaoQuandoArquivoVazioOuSoCabecalho() {
        String csv = """
                carteira_id;ticker;valor_atual;taxa_volatilidade
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(stream));
        assertEquals("O arquivo não contém nenhum ativo válido para processamento.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando inputStream for nulo")
    void deveLancarExcecaoQuandoInputStreamNulo() {
        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(null));
        assertEquals("O fluxo de dados do arquivo não pode ser nulo.", ex.getMessage());
    }
}
