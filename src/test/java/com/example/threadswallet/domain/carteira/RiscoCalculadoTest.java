package com.example.threadswallet.domain.carteira;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiscoCalculadoTest extends UnitAbstractTests {

    @Test
    @DisplayName("Deve criar um risco calculado válido")
    void deveCriarRiscoCalculadoValido() {
        RiscoCalculado risco = RiscoCalculado.create(1L, 0.1542, MetodoCalculo.MONTE_CARLO);

        assertNotNull(risco);
        assertNull(risco.getId());
        assertEquals(1L, risco.getCarteiraId());
        assertEquals(0.1542, risco.getValor());
        assertEquals(MetodoCalculo.MONTE_CARLO, risco.getTipo());
    }

    @Test
    @DisplayName("Deve restaurar um risco calculado existente com ID")
    void deveRestaurarRiscoCalculado() {
        RiscoCalculado risco = RiscoCalculado.restore(10L, 1L, 0.0825, MetodoCalculo.VAR_PARAMETRICO);

        assertNotNull(risco);
        assertEquals(10L, risco.getId());
        assertEquals(1L, risco.getCarteiraId());
        assertEquals(0.0825, risco.getValor());
        assertEquals(MetodoCalculo.VAR_PARAMETRICO, risco.getTipo());
    }

    @Test
    @DisplayName("Deve atualizar o valor do risco calculado com sucesso")
    void deveAtualizarValorComSucesso() {
        RiscoCalculado risco = RiscoCalculado.create(1L, 0.10, MetodoCalculo.MONTE_CARLO);
        risco.atualizarValor(0.185);

        assertEquals(0.185, risco.getValor());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar com ID de carteira nulo")
    void deveLancarExcecaoCarteiraIdNulo() {
        assertThrows(DomainException.class, () -> RiscoCalculado.create(null, 0.10, MetodoCalculo.MONTE_CARLO));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar com valor nulo ou negativo")
    void deveLancarExcecaoValorInvalido() {
        assertThrows(DomainException.class, () -> RiscoCalculado.create(1L, null, MetodoCalculo.MONTE_CARLO));
        assertThrows(DomainException.class, () -> RiscoCalculado.create(1L, -0.01, MetodoCalculo.MONTE_CARLO));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar com tipo nulo")
    void deveLancarExcecaoTipoNulo() {
        assertThrows(DomainException.class, () -> RiscoCalculado.create(1L, 0.10, null));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar valor com número negativo ou nulo")
    void deveLancarExcecaoAoAtualizarValorInvalido() {
        RiscoCalculado risco = RiscoCalculado.create(1L, 0.10, MetodoCalculo.MONTE_CARLO);
        assertThrows(DomainException.class, () -> risco.atualizarValor(null));
        assertThrows(DomainException.class, () -> risco.atualizarValor(-0.05));
    }
}
