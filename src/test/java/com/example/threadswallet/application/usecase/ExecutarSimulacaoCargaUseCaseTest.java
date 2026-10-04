package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExecutarSimulacaoCargaUseCaseTest extends UnitAbstractTests {

    @Mock
    private ProcessarCarteiraUseCase processarCarteiraUseCase;

    @Mock
    private CarteiraRepository carteiraRepository;

    @Spy
    private ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Captor
    private ArgumentCaptor<Long> carteiraIdCaptor;

    private ExecutarSimulacaoCargaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExecutarSimulacaoCargaUseCase(
                processarCarteiraUseCase,
                carteiraRepository,
                virtualThreadExecutor,
                100000,
                2
        );
    }

    @AfterEach
    void tearDown() {
        virtualThreadExecutor.close();
    }

    @Test
    @DisplayName("Deve executar simulação disparando tarefas em Virtual Threads com iterações parametrizadas")
    void deveExecutarSimulacaoComSucesso() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(carteiraRepository.findAllIds()).thenReturn(ids);
        when(processarCarteiraUseCase.execute(anyLong(), anyInt(), any(MetodoCalculo.class))).thenReturn(0.15);

        int iteracoesCustom = 5000;
        SimulacaoResult resultado = useCase.execute(null, iteracoesCustom, MetodoCalculo.MONTE_CARLO);

        assertNotNull(resultado);
        assertEquals(3, resultado.totalCarteirasProcessadas());
        assertEquals(iteracoesCustom, resultado.iteracoesMonteCarloPorCarteira());
        assertEquals(MetodoCalculo.MONTE_CARLO, resultado.metodoCalculo());

        // Verifica que o executor de Virtual Threads (Spy) foi chamado para cada carteira
        verify(processarCarteiraUseCase, times(3)).execute(carteiraIdCaptor.capture(), eq(iteracoesCustom), eq(MetodoCalculo.MONTE_CARLO));
        assertThat(carteiraIdCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("Deve executar simulação utilizando a estratégia VAR_PARAMETRICO")
    void deveExecutarSimulacaoComMetodoParametrico() {
        List<Long> ids = List.of(10L, 20L);
        when(carteiraRepository.findAllIds()).thenReturn(ids);
        when(processarCarteiraUseCase.execute(anyLong(), anyInt(), eq(MetodoCalculo.VAR_PARAMETRICO))).thenReturn(0.08);

        SimulacaoResult resultado = useCase.execute(null, 0, MetodoCalculo.VAR_PARAMETRICO);

        assertNotNull(resultado);
        assertEquals(2, resultado.totalCarteirasProcessadas());
        assertEquals(MetodoCalculo.VAR_PARAMETRICO, resultado.metodoCalculo());

        verify(processarCarteiraUseCase, times(2)).execute(carteiraIdCaptor.capture(), eq(100000), eq(MetodoCalculo.VAR_PARAMETRICO));
        assertThat(carteiraIdCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("Deve lançar DomainException quando o método de cálculo não for informado")
    void deveLancarExcecaoQuandoMetodoCalculoNulo() {
        DomainException exception = assertThrows(DomainException.class,
                () -> useCase.execute(null, 1000, null));

        assertEquals("O método de cálculo de risco é obrigatório.", exception.getMessage());
        verifyNoInteractions(carteiraRepository);
        verifyNoInteractions(processarCarteiraUseCase);
    }

    @Test
    @DisplayName("Deve lançar DomainException quando não houver nenhuma carteira no repositório")
    void deveLancarExcecaoQuandoBaseVazia() {
        when(carteiraRepository.findAllIds()).thenReturn(List.of());

        assertThrows(DomainException.class, () -> useCase.execute(null, null, MetodoCalculo.MONTE_CARLO));

        verifyNoInteractions(processarCarteiraUseCase);
    }
}
