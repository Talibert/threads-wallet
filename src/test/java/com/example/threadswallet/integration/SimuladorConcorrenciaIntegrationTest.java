package com.example.threadswallet.integration;

import com.example.threadswallet.IntegrationAbstractTests;
import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.application.usecase.ExecutarSimulacaoCargaUseCase;
import com.example.threadswallet.application.usecase.GerarMassaDadosUseCase;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimuladorConcorrenciaIntegrationTest extends IntegrationAbstractTests {

    @Autowired
    private GerarMassaDadosUseCase gerarMassaDadosUseCase;

    @Autowired
    private ExecutarSimulacaoCargaUseCase simulacaoUseCase;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Test
    @DisplayName("Cenário de Teste Massivo: Gerar 1.000 carteiras e processar com Virtual Threads (I/O) e CPU Pool (Monte Carlo)")
    void deveExecutarSimulacaoMassivaComSucesso() {
        int totalCarteiras = 1000;

        // 1. Gera a massa de dados sob demanda
        int geradas = gerarMassaDadosUseCase.execute(totalCarteiras, 3, 5, true);
        assertEquals(totalCarteiras, geradas);

        // 2. Dispara o cálculo concorrente sobre as carteiras existentes com Monte Carlo
        SimulacaoResult resultado = simulacaoUseCase.execute(totalCarteiras);

        // Validações dos resultados
        assertNotNull(resultado);
        assertEquals(totalCarteiras, resultado.totalCarteirasProcessadas());
        assertEquals(MetodoCalculo.MONTE_CARLO, resultado.metodoCalculo());
        assertTrue(resultado.tempoTotalMs() > 0, "O tempo total deve ser maior que zero");
        assertTrue(resultado.nucleosCpuDisponiveis() > 0);

        // Verifica no banco se todas as 1.000 carteiras tiveram o risco calculado
        List<Carteira> carteiras = carteiraRepository.findAll();
        assertEquals(totalCarteiras, carteiras.size());

        for (Carteira carteira : carteiras) {
            assertNotNull(carteira.getRiscoCalculado(), "Carteira ID " + carteira.getId() + " deveria ter risco calculado");
            assertTrue(carteira.getRiscoCalculado() > 0.0, "O risco deve ser maior que zero");
            assertFalse(carteira.getAtivos().isEmpty(), "Carteira deve possuir ativos");
            assertTrue(carteira.getAtivos().size() >= 3 && carteira.getAtivos().size() <= 5,
                    "Carteira deve conter de 3 a 5 ativos");
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
        SimulacaoResult resultado = simulacaoUseCase.execute(totalCarteiras, 0, MetodoCalculo.VAR_PARAMETRICO);

        assertNotNull(resultado);
        assertEquals(totalCarteiras, resultado.totalCarteirasProcessadas());
        assertEquals(MetodoCalculo.VAR_PARAMETRICO, resultado.metodoCalculo());
        assertTrue(resultado.tempoTotalMs() > 0);

        List<Carteira> carteiras = carteiraRepository.findAll();
        assertEquals(totalCarteiras, carteiras.size());

        for (Carteira carteira : carteiras) {
            assertNotNull(carteira.getRiscoCalculado());
            assertTrue(carteira.getRiscoCalculado() > 0.0);
        }

        System.out.printf("⚡ Teste de Concorrência (VaR Paramétrico): %d carteiras processadas em %d ms!%n",
                totalCarteiras, resultado.tempoTotalMs());
    }
}
