package com.example.threadswallet.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarteiraJpaRepository extends JpaRepository<CarteiraJpaEntity, Long> {

    @Query("SELECT c.id FROM CarteiraJpaEntity c ORDER BY c.id")
    List<Long> findAllIds();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CarteiraJpaEntity c SET c.riscoCalculado = :risco WHERE c.id = :id")
    void atualizarRisco(@Param("id") Long id, @Param("risco") Double risco);
}
