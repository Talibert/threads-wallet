package com.example.threadswallet.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CarteiraJpaRepository extends JpaRepository<CarteiraJpaEntity, Long> {

    @Query("SELECT c.id FROM CarteiraJpaEntity c ORDER BY c.id")
    List<Long> findAllIds();

    /**
     * Atualiza o risco calculado de forma atômica diretamente no banco via JPQL.
     *
     * NOTA ARQUITETURAL:
     * No JPA/Hibernate, qualquer instrução DML com @Modifying exige obrigatoriamente uma transação
     * ativa (caso contrário lança TransactionRequiredException).
     * Declaramos @Transactional diretamente aqui na interface do Spring Data para encapsular
     * essa restrição técnica da query no ponto exato onde ela reside, mantendo o CarteiraRepositoryImpl
     * como um adaptador puro e evitando que o caso de uso precise abrir transações longas que retenham
     * conexões de banco durante o processamento de CPU.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CarteiraJpaEntity c SET c.riscoCalculado = :risco WHERE c.id = :id")
    void atualizarRisco(@Param("id") Long id, @Param("risco") Double risco);
}
