package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.domain.carteira.*;
import com.example.threadswallet.domain.exception.DomainException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProcessarMultiplasCarteirasUseCaseTest extends UnitAbstractTests {

    @Mock
    private ProcessarCarteiraUseCase processarCarteiraUseCase;

    @Mock
    private CarteiraRepository carteiraRepository;

    @Spy
    private ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Captor
    private ArgumentCaptor<Long> carteiraIdCaptor;

    private ProcessarMultiplasCarteirasUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessarMultiplasCarteirasUseCase(
                processarCarteiraUseCase,
                carteiraRepository,
                virtualThreadExecutor,
                2
        );
    }

    @AfterEach
    void tearDown() {
        virtualThreadExecutor.close();
    }

    @Test
    @DisplayName("Deve executar simulação disparando tarefas em Virtual Threads com ParametrosCalculo (Monte Carlo)")
    void deveExecutarSimulacaoComSucesso() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(carteiraRepository.findAllIds()).thenReturn(ids);
        when(processarCarteiraUseCase.execute(anyLong(), any(ParametrosCalculo.class))).thenReturn(0.15);

        ParametrosCalculo params = ParametrosCalculo.monteCarlo(5000);
        SimulacaoResult resultado = useCase.execute(null, params);

        assertNotNull(resultado);
        assertEquals(3, resultado.totalCarteirasProcessadas());
        assertEquals(5000, resultado.iteracoesMonteCarloPorCarteira());
        assertEquals(MetodoCalculo.MONTE_CARLO, resultado.metodoCalculo());

        verify(processarCarteiraUseCase, times(3)).execute(carteiraIdCaptor.capture(), eq(params));
        assertThat(carteiraIdCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("Deve executar simulação utilizando ParametrosCalculo (VaR Paramétrico)")
    void deveExecutarSimulacaoComMetodoParametrico() {
        List<Long> ids = List.of(10L, 20L);
        when(carteiraRepository.findAllIds()).thenReturn(ids);
        when(processarCarteiraUseCase.execute(anyLong(), any(ParametrosCalculo.class))).thenReturn(0.08);

        ParametrosCalculo params = ParametrosCalculo.varParametrico();
        SimulacaoResult resultado = useCase.execute(null, params);

        assertNotNull(resultado);
        assertEquals(2, resultado.totalCarteirasProcessadas());
        assertEquals(MetodoCalculo.VAR_PARAMETRICO, resultado.metodoCalculo());

        verify(processarCarteiraUseCase, times(2)).execute(carteiraIdCaptor.capture(), eq(params));
        assertThat(carteiraIdCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("Deve lançar DomainException quando os parâmetros de cálculo forem nulos")
    void deveLancarExcecaoQuandoParametrosNulos() {
        DomainException exception = assertThrows(DomainException.class,
                () -> useCase.execute(null, null));

        assertEquals("Os parâmetros de cálculo de risco são obrigatórios.", exception.getMessage());
        verifyNoInteractions(carteiraRepository);
        verifyNoInteractions(processarCarteiraUseCase);
    }

    @Test
    @DisplayName("Deve lançar DomainException quando não houver nenhuma carteira no repositório")
    void deveLancarExcecaoQuandoBaseVazia() {
        when(carteiraRepository.findAllIds()).thenReturn(List.of());

        assertThrows(DomainException.class, () -> useCase.execute(null, ParametrosCalculo.varParametrico()));

        verifyNoInteractions(processarCarteiraUseCase);
    }
}
