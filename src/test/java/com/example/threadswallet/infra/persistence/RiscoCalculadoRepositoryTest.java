package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.RepositoryAbstractTests;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.carteira.MetodoCalculo;
import com.example.threadswallet.domain.carteira.RiscoCalculado;
import com.example.threadswallet.domain.carteira.RiscoCalculadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RiscoCalculadoRepositoryTest extends RepositoryAbstractTests {

    @Autowired
    private RiscoCalculadoRepository riscoCalculadoRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    private Long carteiraId;

    @BeforeEach
    void setUp() {
        riscoCalculadoRepository.deleteAll();
        carteiraRepository.deleteAll();

        Carteira carteira = carteiraRepository.save(Carteira.create("Cliente Teste Risco"));
        carteiraId = carteira.getId();
    }

    @Test
    @DisplayName("Deve salvar novo risco calculado e atualizar registro existente no mesmo tipo")
    void deveSalvarEAtualizarRiscoExistente() {
        RiscoCalculado inicial = RiscoCalculado.create(carteiraId, 0.1250, MetodoCalculo.MONTE_CARLO);
        RiscoCalculado salvo = riscoCalculadoRepository.salvar(inicial);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getCarteiraId()).isEqualTo(carteiraId);
        assertThat(salvo.getValor()).isEqualTo(0.1250);
        assertThat(salvo.getTipo()).isEqualTo(MetodoCalculo.MONTE_CARLO);

        // Atualiza salvando novo valor para o mesmo tipo
        RiscoCalculado atualizado = RiscoCalculado.create(carteiraId, 0.1580, MetodoCalculo.MONTE_CARLO);
        RiscoCalculado salvoAtualizado = riscoCalculadoRepository.salvar(atualizado);

        assertThat(salvoAtualizado.getId()).isEqualTo(salvo.getId());
        assertThat(salvoAtualizado.getValor()).isEqualTo(0.1580);

        Optional<RiscoCalculado> buscado = riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, MetodoCalculo.MONTE_CARLO);
        assertThat(buscado).isPresent();
        assertThat(buscado.get().getValor()).isEqualTo(0.1580);
    }

    @Test
    @DisplayName("Deve armazenar riscos de tipos distintos para a mesma carteira")
    void deveArmazenarRiscosDistintosParaMesmaCarteira() {
        riscoCalculadoRepository.salvar(RiscoCalculado.create(carteiraId, 0.12, MetodoCalculo.MONTE_CARLO));
        riscoCalculadoRepository.salvar(RiscoCalculado.create(carteiraId, 0.05, MetodoCalculo.VAR_PARAMETRICO));

        List<RiscoCalculado> riscos = riscoCalculadoRepository.findByCarteiraId(carteiraId);
        assertThat(riscos).hasSize(2);

        Optional<RiscoCalculado> monteCarlo = riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, MetodoCalculo.MONTE_CARLO);
        assertThat(monteCarlo).isPresent();
        assertThat(monteCarlo.get().getValor()).isEqualTo(0.12);

        Optional<RiscoCalculado> varParametrico = riscoCalculadoRepository.findByCarteiraIdAndTipo(carteiraId, MetodoCalculo.VAR_PARAMETRICO);
        assertThat(varParametrico).isPresent();
        assertThat(varParametrico.get().getValor()).isEqualTo(0.05);
    }

    @Test
    @DisplayName("Deve limpar todos os registros com deleteAll")
    void deveLimparComDeleteAll() {
        riscoCalculadoRepository.salvar(RiscoCalculado.create(carteiraId, 0.10, MetodoCalculo.MONTE_CARLO));
        assertThat(riscoCalculadoRepository.findAll()).hasSize(1);

        riscoCalculadoRepository.deleteAll();
        assertThat(riscoCalculadoRepository.findAll()).isEmpty();
    }
}
