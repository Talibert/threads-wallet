package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.application.dto.SimulacaoResult;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
                100000
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
        when(processarCarteiraUseCase.execute(anyLong(), anyInt())).thenReturn(0.15);

        int iteracoesCustom = 5000;
        SimulacaoResult resultado = useCase.execute(null, iteracoesCustom);

        assertNotNull(resultado);
        assertEquals(3, resultado.totalCarteirasProcessadas());
        assertEquals(iteracoesCustom, resultado.iteracoesMonteCarloPorCarteira());

        // Verifica que o executor de Virtual Threads (Spy) foi chamado para cada carteira
        verify(processarCarteiraUseCase, times(3)).execute(carteiraIdCaptor.capture(), eq(iteracoesCustom));
        assertThat(carteiraIdCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("Deve lançar DomainException quando não houver nenhuma carteira no repositório")
    void deveLancarExcecaoQuandoBaseVazia() {
        when(carteiraRepository.findAllIds()).thenReturn(List.of());

        assertThrows(DomainException.class, () -> useCase.execute(null));

        verifyNoInteractions(processarCarteiraUseCase);
    }
}
