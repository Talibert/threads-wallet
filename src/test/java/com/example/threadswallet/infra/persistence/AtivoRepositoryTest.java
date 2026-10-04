package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.RepositoryAbstractTests;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.AtivoRepository;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtivoRepositoryTest extends RepositoryAbstractTests {

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private AtivoRepository ativoRepository;

    @BeforeEach
    void setUp() {
        ativoRepository.deleteAll();
        carteiraRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve salvar ativos em lote via salvarTodos e recuperar por carteiraId")
    void deveSalvarAtivosEmLote() {
        Carteira carteira = carteiraRepository.save(Carteira.create("Cliente Lote Ativos"));

        Ativo a1 = Ativo.create(carteira.getId(), "PETR4", 38.50, 0.22);
        Ativo a2 = Ativo.create(carteira.getId(), "VALE3", 62.10, 0.18);
        Ativo a3 = Ativo.create(carteira.getId(), "ITUB4", 34.20, 0.15);

        ativoRepository.salvarTodos(List.of(a1, a2, a3));

        List<Ativo> ativosRecuperados = ativoRepository.findByCarteiraId(carteira.getId());
        assertThat(ativosRecuperados).hasSize(3);
        assertThat(ativosRecuperados).extracting(Ativo::getTicker)
                .containsExactlyInAnyOrder("PETR4", "VALE3", "ITUB4");
    }

    @Test
    @DisplayName("Deve limpar todos os ativos com deleteAll")
    void deveLimparAtivos() {
        Carteira carteira = carteiraRepository.save(Carteira.create("Cliente Teste"));
        Ativo a1 = Ativo.create(carteira.getId(), "BBAS3", 28.50, 0.20);
        ativoRepository.salvarTodos(List.of(a1));

        assertThat(ativoRepository.findByCarteiraId(carteira.getId())).hasSize(1);

        ativoRepository.deleteAll();

        assertThat(ativoRepository.findByCarteiraId(carteira.getId())).isEmpty();
    }
}
