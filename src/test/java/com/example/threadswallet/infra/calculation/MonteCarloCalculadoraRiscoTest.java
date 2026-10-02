package com.example.threadswallet.infra.calculation;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.domain.carteira.Ativo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonteCarloCalculadoraRiscoTest extends UnitAbstractTests {

    @Spy
    private MonteCarloCalculadoraRiscoImpl calculadora;

    @Test
    @DisplayName("Deve identificar corretamente o método como MONTE_CARLO")
    void deveRetornarMetodoMonteCarlo() {
        assertEquals(com.example.threadswallet.domain.carteira.MetodoCalculo.MONTE_CARLO, calculadora.getMetodo());
    }

    @Test
    @DisplayName("Deve calcular risco de portfólio usando Monte Carlo com iterações por parâmetro")
    void deveCalcularRiscoMonteCarlo() {
        List<Ativo> ativos = List.of(
                Ativo.create(1L, "PETR4", 20000.0, 0.30),
                Ativo.create(1L, "VALE3", 30000.0, 0.25),
                Ativo.create(1L, "ITUB4", 50000.0, 0.15)
        );

        int iteracoes = 10000;
        Double risco = calculadora.calcularRisco(ativos, iteracoes);

        verify(calculadora, times(1)).calcularRisco(ativos, iteracoes);

        assertNotNull(risco);
        assertTrue(risco > 0.0, "O risco calculado deve ser positivo");
        assertTrue(risco < 1.0, "O risco calculado para esses ativos deve estar em patamar razoável");
    }

    @Test
    @DisplayName("Deve retornar zero se a lista de ativos for vazia ou iterações <= 0")
    void deveRetornarZeroListaVaziaOuIteracoesZero() {
        Double riscoVazio = calculadora.calcularRisco(List.of(), 5000);
        assertEquals(0.0, riscoVazio);
        verify(calculadora).calcularRisco(List.of(), 5000);

        List<Ativo> ativos = List.of(Ativo.create(1L, "PETR4", 1000.0, 0.2));
        Double riscoZeroIteracoes = calculadora.calcularRisco(ativos, 0);
        assertEquals(0.0, riscoZeroIteracoes);
        verify(calculadora).calcularRisco(ativos, 0);
    }

    @Test
    @DisplayName("Deve demonstrar comportamento parcial de Spy simulando retorno customizado")
    void deveUsarSpyParaSobrescreverCalculoParcialmente() {
        List<Ativo> ativos = List.of(Ativo.create(1L, "VALE3", 10000.0, 0.2));

        doReturn(0.1234).when(calculadora).calcularRisco(ativos, 500);

        Double riscoMockado = calculadora.calcularRisco(ativos, 500);

        assertEquals(0.1234, riscoMockado);
        verify(calculadora).calcularRisco(ativos, 500);
    }
}
