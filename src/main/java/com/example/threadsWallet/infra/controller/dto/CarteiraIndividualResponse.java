package com.example.threadswallet.infra.controller.dto;

import com.example.threadswallet.application.dto.CarteiraIndividualResult;
import com.example.threadswallet.domain.carteira.MetodoCalculo;

public record CarteiraIndividualResponse(
        Long carteiraId,
        Double riscoCalculado,
        MetodoCalculo metodoCalculo,
        long tempoExecucaoMs,
        int nucleosCpuUtilizados,
        int iteracoes
) {
    public static CarteiraIndividualResponse from(CarteiraIndividualResult result) {
        return new CarteiraIndividualResponse(
                result.carteiraId(),
                result.riscoCalculado(),
                result.metodo(),
                result.tempoExecucaoMs(),
                result.nucleosCpuUtilizados(),
                result.iteracoes()
        );
    }
}
