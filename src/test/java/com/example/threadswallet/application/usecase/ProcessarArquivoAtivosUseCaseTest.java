package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.application.dto.ProcessamentoAtivosResult;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.AtivoRepository;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.exception.DomainException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessarArquivoAtivosUseCaseTest extends UnitAbstractTests {

    @Spy
    private ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Mock
    private CarteiraRepository carteiraRepository;

    @Mock
    private AtivoRepository ativoRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Captor
    private ArgumentCaptor<List<Ativo>> ativosCaptor;

    private ProcessarArquivoAtivosUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        lenient().when(carteiraRepository.existsById(1L)).thenReturn(true);

        // batchSize = 2 para testar múltiplos lotes com arquivos pequenos
        useCase = new ProcessarArquivoAtivosUseCase(virtualThreadExecutor, carteiraRepository, ativoRepository, transactionTemplate, 2);
    }

    @AfterEach
    void tearDown() {
        virtualThreadExecutor.close();
    }

    @Test
    @DisplayName("Deve processar arquivo CSV com cabeçalho e salvar ativos para a carteira especificada")
    void deveProcessarArquivoCsvComCabecalhoESalvarEmLotes() {
        String csv = """
                ticker;valor_atual;taxa_volatilidade
                PETR4;38.50;0.22
                VALE3;62,10;0.18
                ITUB4;34.20;0.15
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ProcessamentoAtivosResult result = useCase.execute(1L, stream);

        assertNotNull(result);
        assertEquals(3, result.totalAtivosProcessados());
        assertEquals("3 ativos processados e salvos com sucesso.", result.mensagem());

        // Com batchSize=2 e 3 ativos, devem ocorrer 2 chamadas de persistência (lote de 2 + lote residual de 1)
        verify(ativoRepository, times(2)).salvarTodos(ativosCaptor.capture());

        List<List<Ativo>> chamadas = ativosCaptor.getAllValues();
        assertEquals(2, chamadas.get(0).size());
        assertEquals(1, chamadas.get(1).size());

        Ativo petr4 = chamadas.get(0).get(0);
        assertEquals(1L, petr4.getCarteiraId());
        assertEquals("PETR4", petr4.getTicker());
        assertEquals(38.50, petr4.getValorAtual());
        assertEquals(0.22, petr4.getTaxaVolatilidade());

        Ativo vale3 = chamadas.get(0).get(1);
        assertEquals(1L, vale3.getCarteiraId());
        assertEquals(62.10, vale3.getValorAtual());

        Ativo itub4 = chamadas.get(1).get(0);
        assertEquals(1L, itub4.getCarteiraId());
        assertEquals("ITUB4", itub4.getTicker());
    }

    @Test
    @DisplayName("Deve processar arquivo TXT sem cabeçalho e ignorar linhas em branco")
    void deveProcessarArquivoTxtSemCabecalho() {
        String txt = """
                BBAS3;28.50;0.20

                BBDC4;14.30;0.19
                """;
        InputStream stream = new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8));

        ProcessamentoAtivosResult result = useCase.execute(1L, stream);

        assertEquals(2, result.totalAtivosProcessados());
        verify(ativoRepository, times(1)).salvarTodos(ativosCaptor.capture());
        assertEquals(2, ativosCaptor.getValue().size());
        assertEquals(1L, ativosCaptor.getValue().get(0).getCarteiraId());
        assertEquals("BBAS3", ativosCaptor.getValue().get(0).getTicker());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando a linha tiver formato com 4 colunas (sem suporte a formato legado)")
    void deveRejeitarArquivoComFormatoComQuatroColunas() {
        String csv = """
                carteira_id;ticker;valor_atual;taxa_volatilidade
                1;PETR4;38.50;0.22
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(1L, stream));
        assertThat(ex.getMessage()).contains("Linha 2 inválida: esperado exatamente 3 campos");

        verify(ativoRepository, never()).salvarTodos(any());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando a carteira referenciada não existir")
    void deveLancarExcecaoQuandoCarteiraNaoExistir() {
        when(carteiraRepository.existsById(999L)).thenReturn(false);
        String csv = "PETR4;38.50;0.22\n";
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(999L, stream));
        assertThat(ex.getMessage()).isEqualTo("Carteira com ID 999 não foi encontrada.");

        verify(virtualThreadExecutor, never()).submit(any(java.util.concurrent.Callable.class));
        verify(ativoRepository, never()).salvarTodos(any());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando carteiraId for nulo")
    void deveLancarExcecaoQuandoCarteiraIdNulo() {
        InputStream stream = new ByteArrayInputStream("PETR4;38.50;0.22".getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(null, stream));
        assertEquals("O ID da carteira é obrigatório.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando inputStream for nulo")
    void deveLancarExcecaoQuandoInputStreamNulo() {
        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(1L, null));
        assertEquals("O fluxo de dados do arquivo não pode ser nulo.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando a linha tiver menos de 3 campos")
    void deveLancarExcecaoQuandoLinhaTiverCamposInsuficientes() {
        String csv = """
                ticker;valor_atual;taxa_volatilidade
                PETR4;38.50
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(1L, stream));
        assertThat(ex.getMessage()).contains("Linha 2 inválida: esperado exatamente 3 campos");

        verify(ativoRepository, never()).salvarTodos(any());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando campo numérico for inválido")
    void deveLancarExcecaoQuandoCampoNumericoForInvalido() {
        String csv = """
                PETR4;trinta_reais;0.22
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(1L, stream));
        assertThat(ex.getMessage()).contains("Linha 1 com formato numérico inválido");

        verify(ativoRepository, never()).salvarTodos(any());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando ativo violar invariante de negócio (valor menor ou igual a zero)")
    void deveLancarExcecaoQuandoAtivoViolarInvariante() {
        String csv = """
                PETR4;0.0;0.22
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(1L, stream));
        assertEquals("O valor atual do ativo deve ser maior que zero.", ex.getMessage());

        verify(ativoRepository, never()).salvarTodos(any());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando arquivo não contiver ativos válidos")
    void deveLancarExcecaoQuandoArquivoVazioOuSoCabecalho() {
        String csv = """
                ticker;valor_atual;taxa_volatilidade
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> useCase.execute(1L, stream));
        assertEquals("O arquivo não contém nenhum ativo válido para processamento.", ex.getMessage());

        verify(ativoRepository, never()).salvarTodos(any());
    }
}
