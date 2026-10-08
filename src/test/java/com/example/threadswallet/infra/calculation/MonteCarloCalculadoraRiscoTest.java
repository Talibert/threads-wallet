package com.example.threadswallet.infra.calculation;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
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
        assertEquals(MetodoCalculo.MONTE_CARLO, calculadora.getMetodo());
    }

    @Test
    @DisplayName("Deve calcular risco de portfólio usando Monte Carlo")
    void deveCalcularRiscoMonteCarlo() {
        List<Ativo> ativos = List.of(
                Ativo.create(1L, "PETR4", 20000.0, 0.30),
                Ativo.create(1L, "VALE3", 30000.0, 0.25),
                Ativo.create(1L, "ITUB4", 50000.0, 0.15)
        );

        Double risco = calculadora.calcularRisco(ativos, 10000);

        verify(calculadora, times(1)).calcularRisco(ativos, 10000);

        assertNotNull(risco);
        assertTrue(risco > 0.0, "O risco calculado deve ser positivo");
        assertTrue(risco < 1.0, "O risco calculado para esses ativos deve estar em patamar razoável");
    }

    @Test
    @DisplayName("Deve retornar zero se a lista de ativos for vazia")
    void deveRetornarZeroListaVazia() {
        Double riscoVazio = calculadora.calcularRisco(List.of(), 5000);
        assertEquals(0.0, riscoVazio);
        verify(calculadora).calcularRisco(List.of(), 5000);
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

    @Test
    @DisplayName("Deve retornar true para isParalelizavel, calcular amostra e consolidar com sucesso")
    void deveCalcularAmostraEConsolidar() {
        assertTrue(calculadora.isParalelizavel());

        List<Ativo> ativos = List.of(
                Ativo.create(1L, "PETR4", 20000.0, 0.30),
                Ativo.create(1L, "VALE3", 30000.0, 0.25)
        );

        var amostra1 = calculadora.calcularAmostra(ativos, 5000);
        var amostra2 = calculadora.calcularAmostra(ativos, 5000);

        assertNotNull(amostra1);
        assertEquals(5000, amostra1.iteracoes());
        assertNotNull(amostra2);
        assertEquals(5000, amostra2.iteracoes());

        Double riscoConsolidado = calculadora.consolidarAmostras(List.of(amostra1, amostra2));
        assertNotNull(riscoConsolidado);
        assertTrue(riscoConsolidado > 0.0);
    }
}
