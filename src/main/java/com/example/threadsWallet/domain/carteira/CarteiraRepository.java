package com.example.threadswallet.domain.carteira;

import java.util.List;
import java.util.Optional;

public interface CarteiraRepository {

    Carteira save(Carteira carteira);

    List<Carteira> saveAll(List<Carteira> carteiras);

    Optional<Carteira> findById(Long id);

    List<Ativo> findAtivosByCarteiraId(Long carteiraId);

    void atualizarRisco(Long carteiraId, Double riscoCalculado);

    List<Long> findAllIds();

    List<Carteira> findAll();

    long count();

    void deleteAll();
}
