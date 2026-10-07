package com.example.threadswallet.domain.carteira;

import java.util.List;
import java.util.Optional;

public interface RiscoCalculadoRepository {

    RiscoCalculado salvar(RiscoCalculado riscoCalculado);

    Optional<RiscoCalculado> findByCarteiraIdAndTipo(Long carteiraId, MetodoCalculo tipo);

    List<RiscoCalculado> findByCarteiraId(Long carteiraId);

    List<RiscoCalculado> findAll();

    void deleteAll();
}
