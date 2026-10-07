package com.example.threadswallet.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CarteiraJpaRepository extends JpaRepository<CarteiraJpaEntity, Long> {

    @Query("SELECT c.id FROM CarteiraJpaEntity c ORDER BY c.id")
    List<Long> findAllIds();
}
