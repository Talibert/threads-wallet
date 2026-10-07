package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.domain.carteira.MetodoCalculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RiscoCalculadoJpaRepository extends JpaRepository<RiscoCalculadoJpaEntity, Long> {

    @Query("SELECT r FROM RiscoCalculadoJpaEntity r WHERE r.carteira.id = :carteiraId AND r.tipo = :tipo")
    Optional<RiscoCalculadoJpaEntity> findByCarteiraIdAndTipo(@Param("carteiraId") Long carteiraId, @Param("tipo") MetodoCalculo tipo);

    @Query("SELECT r FROM RiscoCalculadoJpaEntity r WHERE r.carteira.id = :carteiraId")
    List<RiscoCalculadoJpaEntity> findByCarteiraId(@Param("carteiraId") Long carteiraId);
}
