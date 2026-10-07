package com.example.threadswallet.domain.carteira;

import java.util.List;
import java.util.Optional;

public interface CarteiraRepository {

    Carteira save(Carteira carteira);

    List<Carteira> saveAll(List<Carteira> carteiras);

    Optional<Carteira> findById(Long id);

    boolean existsById(Long id);

    List<Ativo> findAtivosByCarteiraId(Long carteiraId);

    List<Long> findAllIds();

    List<Carteira> findAll();

    long count();

    void deleteAll();
}
