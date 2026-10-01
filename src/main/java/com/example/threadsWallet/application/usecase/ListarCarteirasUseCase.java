package com.example.threadswallet.application.usecase;

import com.example.threadswallet.application.dto.CarteiraDTO;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListarCarteirasUseCase {

    private final CarteiraRepository carteiraRepository;

    public ListarCarteirasUseCase(CarteiraRepository carteiraRepository) {
        this.carteiraRepository = carteiraRepository;
    }

    public List<CarteiraDTO> execute() {
        return carteiraRepository.findAll().stream()
                .map(CarteiraDTO::fromDomain)
                .toList();
    }
}
