package com.example.threadswallet.domain.carteira;

import com.example.threadswallet.UnitAbstractTests;
import com.example.threadswallet.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CarteiraTest extends UnitAbstractTests {

    @Test
    @DisplayName("Deve criar uma carteira válida com nome e lista vazia de ativos")
    void deveCriarCarteiraValida() {
        Carteira carteira = Carteira.create("Cliente Teste");

        assertNotNull(carteira);
        assertEquals("Cliente Teste", carteira.getNomeCliente());
        assertNull(carteira.getRiscoCalculado());
        assertTrue(carteira.getAtivos().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar carteira com nome nulo ou em branco")
    void deveLancarExcecaoNomeInvalido() {
        assertThrows(DomainException.class, () -> Carteira.create(null));
        assertThrows(DomainException.class, () -> Carteira.create("   "));
    }

    @Test
    @DisplayName("Deve adicionar ativos e calcular o valor total da carteira corretamente")
    void deveAdicionarAtivosECalcularValorTotal() {
        Carteira carteira = Carteira.create("Investidor Alpha");

        carteira.adicionarAtivo(Ativo.create(null, "PETR4", 10000.0, 0.25));
        carteira.adicionarAtivo(Ativo.create(null, "VALE3", 15000.0, 0.20));

        assertEquals(2, carteira.getAtivos().size());
        assertEquals(25000.0, carteira.getValorTotal(), 0.001);
    }

    @Test
    @DisplayName("Deve atualizar o risco calculado com sucesso")
    void deveAtualizarRiscoCalculado() {
        Carteira carteira = Carteira.create("Investidor Beta");
        carteira.atualizarRisco(0.1542);

        assertEquals(0.1542, carteira.getRiscoCalculado());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar definir risco negativo")
    void deveLancarExcecaoRiscoNegativo() {
        Carteira carteira = Carteira.create("Investidor Gamma");
        assertThrows(DomainException.class, () -> carteira.atualizarRisco(-0.01));
    }
}
