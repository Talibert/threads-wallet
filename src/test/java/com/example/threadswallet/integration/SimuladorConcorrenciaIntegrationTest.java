package com.example.threadswallet.integration;

import com.example.threadswallet.IntegrationAbstractTests;
import com.example.threadswallet.application.dto.CarteiraIndividualResult;
import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.application.usecase.GerarMassaDadosUseCase;
import com.example.threadswallet.application.usecase.ProcessarCarteiraIndividualUseCase;
import com.example.threadswallet.application.usecase.ProcessarMultiplasCarteirasUseCase;
import com.example.threadswallet.domain.carteira.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimuladorConcorrenciaIntegrationTest extends IntegrationAbstractTests {

    @Autowired
    private GerarMassaDadosUseCase gerarMassaDadosUseCase;

    @Autowired
    private ProcessarMultiplasCarteirasUseCase multiplasCarteirasUseCase;

    @Autowired
    private ProcessarCarteiraIndividualUseCase carteiraIndividualUseCase;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private RiscoCalculadoRepository riscoCalculadoRepository;

    @Test
    @DisplayName("Cenário de Teste Massivo: Gerar 1.000 carteiras e processar com Virtual Threads (I/O) e CPU Pool (Monte Carlo)")
    void deveExecutarSimulacaoMassivaComSucesso() {
        int totalCarteiras = 1000;

        // 1. Gera a massa de dados sob demanda
        int geradas = gerarMassaDadosUseCase.execute(totalCarteiras, 3, 5, true);
        assertEquals(totalCarteiras, geradas);

        // 2. Dispara o cálculo concorrente sobre as carteiras existentes com Monte Carlo
        ParametrosCalculo params = ParametrosCalculo.monteCarlo(100000);
        SimulacaoResult resultado = multiplasCarteirasUseCase.execute(totalCarteiras, params);

        // Validações dos resultados
        assertNotNull(resultado);
        assertEquals(totalCarteiras, resultado.totalCarteirasProcessadas());
        assertEquals(MetodoCalculo.MONTE_CARLO, resultado.metodoCalculo());
        assertTrue(resultado.tempoTotalMs() > 0, "O tempo total deve ser maior que zero");
        assertTrue(resultado.nucleosCpuDisponiveis() > 0);

        // Verifica no banco se todas as 1.000 carteiras existem e possuem ativos
        List<Carteira> carteiras = carteiraRepository.findAll();
        assertEquals(totalCarteiras, carteiras.size());

        for (Carteira carteira : carteiras) {
            assertFalse(carteira.getAtivos().isEmpty(), "Carteira deve possuir ativos");
            assertTrue(carteira.getAtivos().size() >= 3 && carteira.getAtivos().size() <= 5,
                    "Carteira deve conter de 3 a 5 ativos");
        }

        // Verifica que todos os 1.000 riscos foram salvos na tabela dedicada risco_calculado
        List<RiscoCalculado> riscos = riscoCalculadoRepository.findAll();
        assertEquals(totalCarteiras, riscos.size());

        for (RiscoCalculado risco : riscos) {
            assertNotNull(risco.getValor(), "Carteira ID " + risco.getCarteiraId() + " deveria ter risco calculado");
            assertTrue(risco.getValor() > 0.0, "O risco deve ser maior que zero");
            assertEquals(MetodoCalculo.MONTE_CARLO, risco.getTipo());
        }

        System.out.printf("✅ Teste de Concorrência (Monte Carlo): %d carteiras processadas em %d ms!%n",
                totalCarteiras, resultado.tempoTotalMs());
    }

    @Test
    @DisplayName("Cenário de Teste Massivo: Processar 1.000 carteiras com Virtual Threads e VaR Paramétrico")
    void deveExecutarSimulacaoMassivaComVarParametrico() {
        int totalCarteiras = 1000;

        // 1. Gera a massa de dados sob demanda
        int geradas = gerarMassaDadosUseCase.execute(totalCarteiras, 3, 5, true);
        assertEquals(totalCarteiras, geradas);

        // 2. Dispara o cálculo concorrente sobre as carteiras existentes com VaR Paramétrico
        ParametrosCalculo params = ParametrosCalculo.varParametrico();
        SimulacaoResult resultado = multiplasCarteirasUseCase.execute(totalCarteiras, params);

        assertNotNull(resultado);
        assertEquals(totalCarteiras, resultado.totalCarteirasProcessadas());
        assertEquals(MetodoCalculo.VAR_PARAMETRICO, resultado.metodoCalculo());
        assertTrue(resultado.tempoTotalMs() > 0);

        List<Carteira> carteiras = carteiraRepository.findAll();
        assertEquals(totalCarteiras, carteiras.size());

        List<RiscoCalculado> riscos = riscoCalculadoRepository.findAll();
        assertEquals(totalCarteiras, riscos.size());

        for (RiscoCalculado risco : riscos) {
            assertNotNull(risco.getValor());
            assertTrue(risco.getValor() > 0.0);
            assertEquals(MetodoCalculo.VAR_PARAMETRICO, risco.getTipo());
        }

        System.out.printf("⚡ Teste de Concorrência (VaR Paramétrico): %d carteiras processadas em %d ms!%n",
                totalCarteiras, resultado.tempoTotalMs());
    }

    @Test
    @DisplayName("Cenário de Carteira Individual: Calcular risco sob demanda com particionamento de CPU no Monte Carlo")
    void deveCalcularCarteiraIndividualComParticionamentoCpu() {
        gerarMassaDadosUseCase.execute(1, 3, 5, true);
        Long carteiraId = carteiraRepository.findAllIds().getFirst();

        ParametrosCalculo params = ParametrosCalculo.monteCarlo(10000);
        CarteiraIndividualResult result = carteiraIndividualUseCase.execute(carteiraId, params);

        assertNotNull(result);
        assertEquals(carteiraId, result.carteiraId());
        assertTrue(result.riscoCalculado() > 0.0);
        assertEquals(MetodoCalculo.MONTE_CARLO, result.metodo());
        assertTrue(result.nucleosCpuUtilizados() >= 1);

        RiscoCalculado salvo = riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, MetodoCalculo.MONTE_CARLO)
                .orElseThrow();
        assertEquals(result.riscoCalculado(), salvo.getValor());
    }
}
