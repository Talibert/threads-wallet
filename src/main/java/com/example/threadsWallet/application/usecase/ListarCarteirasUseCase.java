package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.CarteiraDTO;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.carteira.RiscoCalculado;
import com.example.threadswallet.domain.carteira.RiscoCalculadoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ListarCarteirasUseCase {

    private final CarteiraRepository carteiraRepository;
    private final RiscoCalculadoRepository riscoCalculadoRepository;

    public ListarCarteirasUseCase(
            CarteiraRepository carteiraRepository,
            RiscoCalculadoRepository riscoCalculadoRepository
    ) {
        this.carteiraRepository = carteiraRepository;
        this.riscoCalculadoRepository = riscoCalculadoRepository;
    }

    public List<CarteiraDTO> execute() {
        List<Carteira> carteiras = carteiraRepository.findAll();
        Map<Long, List<RiscoCalculado>> riscosPorCarteira = riscoCalculadoRepository.findAll().stream()
                .collect(Collectors.groupingBy(RiscoCalculado::getCarteiraId));

        return carteiras.stream()
                .map(c -> CarteiraDTO.fromDomain(c, riscosPorCarteira.getOrDefault(c.getId(), List.of())))
                .toList();
    }
}
