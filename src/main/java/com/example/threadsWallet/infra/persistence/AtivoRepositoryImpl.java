package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.AtivoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AtivoRepositoryImpl implements AtivoRepository {

    private final AtivoJpaRepository ativoJpaRepository;

    public AtivoRepositoryImpl(AtivoJpaRepository ativoJpaRepository) {
        this.ativoJpaRepository = ativoJpaRepository;
    }

    @Override
    public void salvarTodos(List<Ativo> ativos) {
        if (ativos == null || ativos.isEmpty()) {
            return;
        }

        List<AtivoJpaEntity> entities = ativos.stream()
                .map(this::toJpaEntityAtivo)
                .toList();

        ativoJpaRepository.saveAll(entities);
    }

    @Override
    public List<Ativo> findByCarteiraId(Long carteiraId) {
        return ativoJpaRepository.findByCarteiraId(carteiraId).stream()
                .map(this::toDomainAtivo)
                .toList();
    }

    @Override
    public void deleteAll() {
        ativoJpaRepository.deleteAll();
    }

    private AtivoJpaEntity toJpaEntityAtivo(Ativo domain) {
        CarteiraJpaEntity carteira = new CarteiraJpaEntity();
        carteira.setId(domain.getCarteiraId());

        AtivoJpaEntity entity = new AtivoJpaEntity(
                carteira,
                domain.getTicker(),
                domain.getValorAtual(),
                domain.getTaxaVolatilidade()
        );
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
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
