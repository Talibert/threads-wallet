package com.example.threadswallet.domain.carteira;

import java.util.List;

public interface AtivoRepository {

    void salvarTodos(List<Ativo> ativos);

    default void saveAll(List<Ativo> ativos) {
        salvarTodos(ativos);
    }

    List<Ativo> findByCarteiraId(Long carteiraId);

    void deleteAll();
}
