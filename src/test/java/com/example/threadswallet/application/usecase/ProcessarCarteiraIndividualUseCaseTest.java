package com.example.threadswallet.application.usecase;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.application.dto.CarteiraIndividualResult;
import com.example.threadswallet.domain.carteira.*;
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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ProcessarCarteiraIndividualUseCaseTest extends UnitAbstractTests {

    @Mock
    private CarteiraRepository carteiraRepository;

    @Mock
    private RiscoCalculadoRepository riscoCalculadoRepository;

    @Spy
    private MonteCarloCalculadoraRiscoImpl monteCarloCalculadora = new MonteCarloCalculadoraRiscoImpl();

    @Spy
    private VarParametricoCalculadoraRiscoImpl varParametricoCalculadora = new VarParametricoCalculadoraRiscoImpl();

    @Spy
    private ExecutorService cpuThreadPool = Executors.newFixedThreadPool(4);

    @Captor
    private ArgumentCaptor<RiscoCalculado> riscoCaptor;

    private ProcessarCarteiraIndividualUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessarCarteiraIndividualUseCase(
                carteiraRepository,
                riscoCalculadoRepository,
                List.of(monteCarloCalculadora, varParametricoCalculadora),
                cpuThreadPool,
                0 // 0 reservadas nos testes unitários para utilizar as threads disponíveis
        );
    }

    @AfterEach
    void tearDown() {
        cpuThreadPool.close();
    }

    private List<Ativo> criarAtivosExemplo() {
        return List.of(
                Ativo.restore(1L, 1L, "PETR4", 30.0, 0.25),
                Ativo.restore(2L, 1L, "VALE3", 70.0, 0.20)
        );
    }

    @Test
    @DisplayName("Deve processar carteira individual com MONTE_CARLO particionando em múltiplas threads de CPU")
    void deveProcessarCarteiraIndividualComMonteCarloParalelo() {
        Long carteiraId = 1L;
        ParametrosCalculo params = ParametrosCalculo.monteCarlo(8000);

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(criarAtivosExemplo());
        when(riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, MetodoCalculo.MONTE_CARLO))
                .thenReturn(Optional.empty());

        CarteiraIndividualResult result = useCase.execute(carteiraId, params);

        assertNotNull(result);
        assertEquals(carteiraId, result.carteiraId());
        assertEquals(MetodoCalculo.MONTE_CARLO, result.metodo());
        assertTrue(result.riscoCalculado() > 0.0);
        assertTrue(result.nucleosCpuUtilizados() > 1, "Deve ter particionado entre os núcleos de CPU");

        verify(monteCarloCalculadora, atLeast(2)).calcularAmostra(any(), anyInt());
        verify(monteCarloCalculadora, times(1)).consolidarAmostras(any());

        verify(riscoCalculadoRepository, times(1)).salvar(riscoCaptor.capture());
        RiscoCalculado salvo = riscoCaptor.getValue();
        assertEquals(carteiraId, salvo.getCarteiraId());
        assertEquals(MetodoCalculo.MONTE_CARLO, salvo.getTipo());
        assertEquals(result.riscoCalculado(), salvo.getValor());
    }

    @Test
    @DisplayName("Deve processar carteira individual com VAR_PARAMETRICO em uma única thread")
    void deveProcessarCarteiraIndividualComVarParametrico() {
        Long carteiraId = 2L;
        ParametrosCalculo params = ParametrosCalculo.varParametrico();

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(criarAtivosExemplo());
        when(riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, MetodoCalculo.VAR_PARAMETRICO))
                .thenReturn(Optional.empty());

        CarteiraIndividualResult result = useCase.execute(carteiraId, params);

        assertNotNull(result);
        assertEquals(carteiraId, result.carteiraId());
        assertEquals(MetodoCalculo.VAR_PARAMETRICO, result.metodo());
        assertEquals(1, result.nucleosCpuUtilizados(), "VaR Paramétrico deve utilizar exatamente 1 thread");
        assertTrue(result.riscoCalculado() > 0.0);

        verify(varParametricoCalculadora, times(1)).calcularRisco(any(), anyInt());
        verify(riscoCalculadoRepository, times(1)).salvar(any(RiscoCalculado.class));
    }

    @Test
    @DisplayName("Deve atualizar risco existente se carteira já possuir cálculo prévio")
    void deveAtualizarRiscoExistente() {
        Long carteiraId = 3L;
        ParametrosCalculo params = ParametrosCalculo.varParametrico();
        RiscoCalculado existente = RiscoCalculado.restore(10L, carteiraId, 0.05, MetodoCalculo.VAR_PARAMETRICO);

        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(criarAtivosExemplo());
        when(riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, MetodoCalculo.VAR_PARAMETRICO))
                .thenReturn(Optional.of(existente));

        CarteiraIndividualResult result = useCase.execute(carteiraId, params);

        verify(riscoCalculadoRepository, times(1)).salvar(existente);
        assertEquals(result.riscoCalculado(), existente.getValor());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando carteira não possuir ativos")
    void deveLancarExcecaoQuandoCarteiraSemAtivos() {
        Long carteiraId = 99L;
        when(carteiraRepository.findAtivosByCarteiraId(carteiraId)).thenReturn(List.of());

        DomainException ex = assertThrows(DomainException.class,
                () -> useCase.execute(carteiraId, ParametrosCalculo.monteCarlo(1000)));

        assertEquals("Carteira com ID 99 não possui ativos ou não foi encontrada.", ex.getMessage());
        verify(riscoCalculadoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve lançar DomainException se carteiraId ou parametros forem nulos")
    void deveValidarCamposObrigatorios() {
        assertThrows(DomainException.class, () -> useCase.execute(null, ParametrosCalculo.monteCarlo(1000)));
        assertThrows(DomainException.class, () -> useCase.execute(1L, null));
    }
}
