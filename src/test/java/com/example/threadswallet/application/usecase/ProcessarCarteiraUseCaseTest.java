package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.exception.DomainException;
import com.example.threadswallet.infra.calculation.MonteCarloCalculadoraRiscoImpl;
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
    private MonteCarloCalculadoraRiscoImpl calculadoraRisco;

    @Captor
    private ArgumentCaptor<Double> riscoCaptor;

    private ExecutorService cpuThreadPool;
    private ProcessarCarteiraUseCase processarCarteiraUseCase;

    @BeforeEach
    void setUp() {
        cpuThreadPool = Executors.newFixedThreadPool(2);
        processarCarteiraUseCase = new ProcessarCarteiraUseCase(
                carteiraRepository,
                calculadoraRisco,
                cpuThreadPool
        );
    }

    @AfterEach
    void tearDown() {
        cpuThreadPool.shutdownNow();
    }

    @Test
    @DisplayName("Deve processar carteira delegando para a calculadora Spy e atualizando o risco no repositório")
    void deveProcessarCarteiraComSucessoDelegandoParaCalculadoraSpy() {
        Long carteiraId = 42L;
        int iteracoes = 1000;
        List<Ativo> ativos = List.of(
                Ativo.create(carteiraId, "PETR4", 15000.0, 0.25),
                Ativo.create(carteiraId, "VALE3", 20000.0, 0.20)
        );

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(ativos);

        Double resultado = processarCarteiraUseCase.execute(carteiraId, iteracoes);

        assertNotNull(resultado);
        assertTrue(resultado > 0.0);

        // Verifica que o Spy da calculadora stateless foi acionado com os parâmetros corretos
        verify(calculadoraRisco, times(1)).calcularRisco(eq(ativos), eq(iteracoes));

        // Captura e valida o risco gravado no repositório com ArgumentCaptor
        verify(carteiraRepository, times(1)).atualizarRisco(eq(carteiraId), riscoCaptor.capture());
        assertEquals(resultado, riscoCaptor.getValue());
    }

    @Test
    @DisplayName("Deve lançar DomainException e não acionar a calculadora quando a carteira não tiver ativos")
    void deveLancarExcecaoQuandoCarteiraSemAtivos() {
        Long carteiraId = 99L;
        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(List.of());

        assertThrows(DomainException.class, () -> processarCarteiraUseCase.execute(carteiraId, 1000));

        // Garante que a calculadora nem o repositório foram chamados para atualização
        verifyNoInteractions(calculadoraRisco);
        verify(carteiraRepository, never()).atualizarRisco(anyLong(), anyDouble());
    }

    @Test
    @DisplayName("Deve permitir sobrescrever o cálculo da calculadora via Mockito doReturn no Spy")
    void devePermitirMockParcialComSpy() {
        Long carteiraId = 10L;
        List<Ativo> ativos = List.of(Ativo.create(carteiraId, "ITUB4", 30000.0, 0.15));

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(ativos);
        doReturn(0.0888).when(calculadoraRisco).calcularRisco(anyList(), anyInt());

        Double resultado = processarCarteiraUseCase.execute(carteiraId, 500);

        assertEquals(0.0888, resultado);
        verify(calculadoraRisco).calcularRisco(ativos, 500);
        verify(carteiraRepository).atualizarRisco(carteiraId, 0.0888);
    }
}
