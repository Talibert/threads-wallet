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

class VarParametricoCalculadoraRiscoTest extends UnitAbstractTests {

    @Spy
    private VarParametricoCalculadoraRiscoImpl calculadora;

    @Test
    @DisplayName("Deve identificar corretamente o método de cálculo como VAR_PARAMETRICO")
    void deveRetornarMetodoCorreto() {
        assertEquals(MetodoCalculo.VAR_PARAMETRICO, calculadora.getMetodo());
    }

    @Test
    @DisplayName("Deve calcular risco de portfólio usando VaR Paramétrico analítico")
    void deveCalcularRiscoParametrico() {
        List<Ativo> ativos = List.of(
                Ativo.create(1L, "PETR4", 20000.0, 0.30),
                Ativo.create(1L, "VALE3", 30000.0, 0.25),
                Ativo.create(1L, "ITUB4", 50000.0, 0.15)
        );

        Double risco = calculadora.calcularRisco(ativos, 0);

        verify(calculadora, times(1)).calcularRisco(ativos, 0);

        assertNotNull(risco);
        assertTrue(risco > 0.0, "O risco calculado deve ser positivo");
        assertTrue(risco < 1.0, "O risco calculado para esses ativos deve estar em patamar razoável");
    }

    @Test
    @DisplayName("Deve retornar zero se a lista de ativos for nula ou vazia")
    void deveRetornarZeroListaVazia() {
        assertEquals(0.0, calculadora.calcularRisco(List.of(), 100));
        assertEquals(0.0, calculadora.calcularRisco(null, 100));
    }

    @Test
    @DisplayName("Deve demonstrar comportamento parcial de Spy com Mockito")
    void devePermitirSpyParcial() {
        List<Ativo> ativos = List.of(Ativo.create(1L, "VALE3", 10000.0, 0.2));

        doReturn(0.0999).when(calculadora).calcularRisco(ativos, 0);

        Double riscoMockado = calculadora.calcularRisco(ativos, 0);

        assertEquals(0.0999, riscoMockado);
        verify(calculadora).calcularRisco(ativos, 0);
    }

    @Test
    @DisplayName("Deve retornar false para isParalelizavel e lançar exceção ao chamar métodos de amostras")
    void deveValidarComportamentoNaoParalelizavel() {
        assertFalse(calculadora.isParalelizavel());

        assertThrows(UnsupportedOperationException.class,
                () -> calculadora.calcularAmostra(List.of(), 100));

        assertThrows(UnsupportedOperationException.class,
                () -> calculadora.consolidarAmostras(List.of()));
    }
}

