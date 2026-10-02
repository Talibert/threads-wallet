package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import com.example.threadswallet.domain.exception.DomainException;
import com.example.threadswallet.infra.calculation.MonteCarloCalculadoraRiscoImpl;
import com.example.threadswallet.infra.calculation.VarParametricoCalculadoraRiscoImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProcessarCarteiraUseCaseTest extends UnitAbstractTests {

    @Mock
    private CarteiraRepository carteiraRepository;

    @Spy
    private MonteCarloCalculadoraRiscoImpl monteCarloCalculadora;

    @Spy
    private VarParametricoCalculadoraRiscoImpl parametricoCalculadora;

    @Captor
    private ArgumentCaptor<Double> riscoCaptor;

    private ExecutorService cpuThreadPool;
    private ProcessarCarteiraUseCase processarCarteiraUseCase;

    @BeforeEach
    void setUp() {
        cpuThreadPool = Executors.newFixedThreadPool(2);
        processarCarteiraUseCase = new ProcessarCarteiraUseCase(
                carteiraRepository,
                List.of(monteCarloCalculadora, parametricoCalculadora),
                cpuThreadPool
        );
        clearInvocations(monteCarloCalculadora, parametricoCalculadora);
    }

    @AfterEach
    void tearDown() {
        cpuThreadPool.shutdownNow();
    }

    @Test
    @DisplayName("Deve processar carteira com Monte Carlo delegando para o Spy correspondente")
    void deveProcessarCarteiraComMonteCarlo() {
        Long carteiraId = 42L;
        int iteracoes = 1000;
        List<Ativo> ativos = List.of(
                Ativo.create(carteiraId, "PETR4", 15000.0, 0.25),
                Ativo.create(carteiraId, "VALE3", 20000.0, 0.20)
        );

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(ativos);

        Double resultado = processarCarteiraUseCase.execute(carteiraId, iteracoes, MetodoCalculo.MONTE_CARLO);

        assertNotNull(resultado);
        assertTrue(resultado > 0.0);

        // Verifica que o Spy do Monte Carlo foi acionado e o paramétrico não
        verify(monteCarloCalculadora, times(1)).calcularRisco(eq(ativos), eq(iteracoes));
        verifyNoInteractions(parametricoCalculadora);

        verify(carteiraRepository, times(1)).atualizarRisco(eq(carteiraId), riscoCaptor.capture());
        assertEquals(resultado, riscoCaptor.getValue());
    }

    @Test
    @DisplayName("Deve processar carteira com VaR Paramétrico delegando para a estratégia analítica via Strategy")
    void deveProcessarCarteiraComVarParametrico() {
        Long carteiraId = 55L;
        List<Ativo> ativos = List.of(
                Ativo.create(carteiraId, "ITUB4", 25000.0, 0.18),
                Ativo.create(carteiraId, "BBDC4", 25000.0, 0.22)
        );

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(ativos);

        Double resultado = processarCarteiraUseCase.execute(carteiraId, 0, MetodoCalculo.VAR_PARAMETRICO);

        assertNotNull(resultado);
        assertTrue(resultado > 0.0);

        // Verifica que a estratégia Paramétrica foi chamada e o Monte Carlo não
        verify(parametricoCalculadora, times(1)).calcularRisco(eq(ativos), eq(0));
        verifyNoInteractions(monteCarloCalculadora);

        verify(carteiraRepository, times(1)).atualizarRisco(eq(carteiraId), riscoCaptor.capture());
        assertEquals(resultado, riscoCaptor.getValue());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando o método de cálculo não for informado")
    void deveLancarExcecaoQuandoMetodoCalculoNulo() {
        Long carteiraId = 1L;

        DomainException exception = assertThrows(DomainException.class,
                () -> processarCarteiraUseCase.execute(carteiraId, 1000, null));

        assertEquals("O método de cálculo de risco é obrigatório.", exception.getMessage());
        verifyNoInteractions(carteiraRepository);
        verifyNoInteractions(monteCarloCalculadora);
        verifyNoInteractions(parametricoCalculadora);
    }

    @Test
    @DisplayName("Deve lançar DomainException e não consultar o banco quando calculadora não for encontrada")
    void deveLancarExcecaoSemConsultarBancoQuandoCalculadoraNaoEncontrada() {
        ProcessarCarteiraUseCase useCaseSemParametrico = new ProcessarCarteiraUseCase(
                carteiraRepository,
                List.of(monteCarloCalculadora),
                cpuThreadPool
        );
        Long carteiraId = 1L;

        DomainException exception = assertThrows(DomainException.class,
                () -> useCaseSemParametrico.execute(carteiraId, 1000, MetodoCalculo.VAR_PARAMETRICO));

        assertTrue(exception.getMessage().contains("Nenhuma calculadora de risco encontrada"));
        verifyNoInteractions(carteiraRepository);
    }

    @Test
    @DisplayName("Deve lançar DomainException e não acionar a calculadora quando a carteira não tiver ativos")
    void deveLancarExcecaoQuandoCarteiraSemAtivos() {
        Long carteiraId = 99L;
        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(List.of());

        assertThrows(DomainException.class, () -> processarCarteiraUseCase.execute(carteiraId, 1000, MetodoCalculo.MONTE_CARLO));

        verifyNoInteractions(monteCarloCalculadora);
        verifyNoInteractions(parametricoCalculadora);
        verify(carteiraRepository, never()).atualizarRisco(anyLong(), anyDouble());
    }

    @Test
    @DisplayName("Deve permitir sobrescrever o cálculo da calculadora via Mockito doReturn no Spy")
    void devePermitirMockParcialComSpy() {
        Long carteiraId = 10L;
        List<Ativo> ativos = List.of(Ativo.create(carteiraId, "ITUB4", 30000.0, 0.15));

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(ativos);
        doReturn(0.0888).when(monteCarloCalculadora).calcularRisco(anyList(), anyInt());

        Double resultado = processarCarteiraUseCase.execute(carteiraId, 500, MetodoCalculo.MONTE_CARLO);

        assertEquals(0.0888, resultado);
        verify(monteCarloCalculadora).calcularRisco(ativos, 500);
        verify(carteiraRepository).atualizarRisco(carteiraId, 0.0888);
    }
}
