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
    @DisplayName("Deve lançar exceção ao tentar adicionar ativo nulo")
    void deveLancarExcecaoAoAdicionarAtivoNulo() {
        Carteira carteira = Carteira.create("Investidor Alpha");
        assertThrows(DomainException.class, () -> carteira.adicionarAtivo(null));
    }
}
