package com.example.threadswallet.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AtivoJpaRepository extends JpaRepository<AtivoJpaEntity, Long> {

    @Query("SELECT a FROM AtivoJpaEntity a WHERE a.carteira.id = :carteiraId")
    List<AtivoJpaEntity> findByCarteiraId(@Param("carteiraId") Long carteiraId);
}
