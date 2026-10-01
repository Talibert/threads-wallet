package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CarteiraRepositoryImpl implements CarteiraRepository {

    private final CarteiraJpaRepository carteiraJpaRepository;
    private final AtivoJpaRepository ativoJpaRepository;

    public CarteiraRepositoryImpl(CarteiraJpaRepository carteiraJpaRepository, AtivoJpaRepository ativoJpaRepository) {
        this.carteiraJpaRepository = carteiraJpaRepository;
        this.ativoJpaRepository = ativoJpaRepository;
    }

    @Override
    @Transactional
    public Carteira save(Carteira domain) {
        CarteiraJpaEntity jpaEntity = toJpaEntity(domain);
        CarteiraJpaEntity saved = carteiraJpaRepository.save(jpaEntity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public List<Carteira> saveAll(List<Carteira> carteiras) {
        List<CarteiraJpaEntity> entities = carteiras.stream()
                .map(this::toJpaEntity)
                .toList();

        List<CarteiraJpaEntity> savedEntities = carteiraJpaRepository.saveAll(entities);

        return savedEntities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Carteira> findById(Long id) {
        return carteiraJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ativo> findAtivosByCarteiraId(Long carteiraId) {
        return ativoJpaRepository.findByCarteiraId(carteiraId).stream()
                .map(this::toDomainAtivo)
                .toList();
    }

    @Override
    @Transactional
    public void atualizarRisco(Long carteiraId, Double riscoCalculado) {
        carteiraJpaRepository.atualizarRisco(carteiraId, riscoCalculado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAllIds() {
        return carteiraJpaRepository.findAllIds();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Carteira> findAll() {
        return carteiraJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return carteiraJpaRepository.count();
    }

    @Override
    @Transactional
    public void deleteAll() {
        carteiraJpaRepository.deleteAll();
    }

    private CarteiraJpaEntity toJpaEntity(Carteira domain) {
        CarteiraJpaEntity entity = new CarteiraJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setNomeCliente(domain.getNomeCliente());
        entity.setRiscoCalculado(domain.getRiscoCalculado());

        List<AtivoJpaEntity> ativosJpa = new ArrayList<>();
        for (Ativo ativoDomain : domain.getAtivos()) {
            AtivoJpaEntity ativoJpa = new AtivoJpaEntity(
                    entity,
                    ativoDomain.getTicker(),
                    ativoDomain.getValorAtual(),
                    ativoDomain.getTaxaVolatilidade()
            );
            if (ativoDomain.getId() != null) {
                ativoJpa.setId(ativoDomain.getId());
            }
            ativosJpa.add(ativoJpa);
        }
        entity.setAtivos(ativosJpa);

        return entity;
    }

    private Carteira toDomain(CarteiraJpaEntity entity) {
        List<Ativo> ativosDomain = entity.getAtivos().stream()
                .map(this::toDomainAtivo)
                .toList();

        return Carteira.restore(
                entity.getId(),
                entity.getNomeCliente(),
                entity.getRiscoCalculado(),
                ativosDomain
        );
    }

    private Ativo toDomainAtivo(AtivoJpaEntity entity) {
        return Ativo.restore(
                entity.getId(),
                entity.getCarteira() != null ? entity.getCarteira().getId() : null,
                entity.getTicker(),
                entity.getValorAtual(),
                entity.getTaxaVolatilidade()
        );
    }
}
