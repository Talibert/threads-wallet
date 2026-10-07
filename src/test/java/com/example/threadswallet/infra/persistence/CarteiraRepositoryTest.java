package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.RepositoryAbstractTests;
import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CarteiraRepositoryTest extends RepositoryAbstractTests {

    @Autowired
    private CarteiraRepository carteiraRepository;

    @BeforeEach
    void setUp() {
        carteiraRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve salvar e recuperar uma carteira com seus ativos associados")
    void deveSalvarERecuperarCarteira() {
        Carteira carteira = Carteira.create("Investidor XPTO");
        carteira.adicionarAtivo(Ativo.create(null, "PETR4", 38.50, 0.22));
        carteira.adicionarAtivo(Ativo.create(null, "VALE3", 61.20, 0.18));

        Carteira salva = carteiraRepository.save(carteira);

        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getNomeCliente()).isEqualTo("Investidor XPTO");
        assertThat(salva.getAtivos()).hasSize(2);

        Optional<Carteira> buscada = carteiraRepository.findById(salva.getId());
        assertThat(buscada).isPresent();
        assertThat(buscada.get().getNomeCliente()).isEqualTo("Investidor XPTO");
        assertThat(buscada.get().getAtivos()).hasSize(2);
    }

    @Test
    @DisplayName("Deve salvar múltiplas carteiras em lote via saveAll e contar registros")
    void deveSalvarTodasECalcularTotal() {
        Carteira c1 = Carteira.create("Cliente 1");
        c1.adicionarAtivo(Ativo.create(null, "ITUB4", 34.0, 0.15));

        Carteira c2 = Carteira.create("Cliente 2");
        c2.adicionarAtivo(Ativo.create(null, "BBDC4", 15.0, 0.17));

        List<Carteira> salvas = carteiraRepository.saveAll(List.of(c1, c2));

        assertThat(salvas).hasSize(2);
        assertThat(carteiraRepository.count()).isEqualTo(2);

        List<Long> ids = carteiraRepository.findAllIds();
        assertThat(ids).hasSize(2);
    }

    @Test
    @DisplayName("Deve buscar ativos isoladamente por ID da carteira")
    void deveBuscarAtivosPorCarteiraId() {
        Carteira c = Carteira.create("Cliente VIP");
        c.adicionarAtivo(Ativo.create(null, "WEGE3", 52.0, 0.12));
        Carteira salva = carteiraRepository.save(c);

        List<Ativo> ativos = carteiraRepository.findAtivosByCarteiraId(salva.getId());

        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).getTicker()).isEqualTo("WEGE3");
        assertThat(ativos.get(0).getValorAtual()).isEqualTo(52.0);
    }

    @Test
    @DisplayName("Deve retornar todos os registros e limpar a base com deleteAll")
    void deveListarTodasELimpar() {
        carteiraRepository.save(Carteira.create("C1"));
        carteiraRepository.save(Carteira.create("C2"));

        assertThat(carteiraRepository.findAll()).hasSize(2);

        carteiraRepository.deleteAll();

        assertThat(carteiraRepository.count()).isZero();
        assertThat(carteiraRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Deve verificar existência por ID via existsById")
    void deveVerificarExistenciaPorId() {
        Carteira salva = carteiraRepository.save(Carteira.create("Cliente Exists"));
        assertThat(carteiraRepository.existsById(salva.getId())).isTrue();
        assertThat(carteiraRepository.existsById(999999L)).isFalse();
    }
}
