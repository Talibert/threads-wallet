package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.domain.carteira.MetodoCalculo;
import com.example.threadswallet.domain.carteira.RiscoCalculado;
import com.example.threadswallet.domain.carteira.RiscoCalculadoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class RiscoCalculadoRepositoryImpl implements RiscoCalculadoRepository {

    private final RiscoCalculadoJpaRepository riscoCalculadoJpaRepository;

    public RiscoCalculadoRepositoryImpl(RiscoCalculadoJpaRepository riscoCalculadoJpaRepository) {
        this.riscoCalculadoJpaRepository = riscoCalculadoJpaRepository;
    }

    @Override
    public RiscoCalculado salvar(RiscoCalculado domain) {
        RiscoCalculadoJpaEntity entity;

        if (domain.getId() != null)
            entity = toJpaEntity(domain);
        else {
            Optional<RiscoCalculadoJpaEntity> existente =
                    riscoCalculadoJpaRepository.findByCarteiraIdAndTipo(domain.getCarteiraId(), domain.getTipo());
            if (existente.isPresent()) {
                entity = existente.get();
                entity.setValor(domain.getValor());
            } else
                entity = toJpaEntity(domain);
        }

        RiscoCalculadoJpaEntity salvo = riscoCalculadoJpaRepository.save(entity);
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RiscoCalculado> findByCarteiraIdAndTipo(Long carteiraId, MetodoCalculo tipo) {
        return riscoCalculadoJpaRepository.findByCarteiraIdAndTipo(carteiraId, tipo)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiscoCalculado> findByCarteiraId(Long carteiraId) {
        return riscoCalculadoJpaRepository.findByCarteiraId(carteiraId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiscoCalculado> findAll() {
        return riscoCalculadoJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteAll() {
        riscoCalculadoJpaRepository.deleteAll();
    }

    private RiscoCalculadoJpaEntity toJpaEntity(RiscoCalculado domain) {
        CarteiraJpaEntity carteira = new CarteiraJpaEntity();
        carteira.setId(domain.getCarteiraId());

        return new RiscoCalculadoJpaEntity(
                domain.getId(),
                carteira,
                domain.getValor(),
                domain.getTipo()
        );
    }

    private RiscoCalculado toDomain(RiscoCalculadoJpaEntity entity) {
        return RiscoCalculado.restore(
                entity.getId(),
                entity.getCarteira().getId(),
                entity.getValor(),
                entity.getTipo()
        );
    }
}
