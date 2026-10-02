package com.example.threadswallet.infra.controller.dto;

import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.domain.carteira.MetodoCalculo;

public record SimulacaoResponse(
        int totalCarteirasProcessadas,
        long tempoTotalMs,
        double tempoTotalSegundos,
        double tempoMedioPorCarteiraMs,
        int nucleosCpuDisponiveis,
        int iteracoesMonteCarloPorCarteira,
        MetodoCalculo metodoCalculo,
        String mensagem
) {
    public static SimulacaoResponse from(SimulacaoResult result) {
        return new SimulacaoResponse(
                result.totalCarteirasProcessadas(),
                result.tempoTotalMs(),
                Math.round((result.tempoTotalMs() / 1000.0) * 100.0) / 100.0,
                Math.round(result.tempoMedioPorCarteiraMs() * 100.0) / 100.0,
                result.nucleosCpuDisponiveis(),
                result.iteracoesMonteCarloPorCarteira(),
                result.metodoCalculo(),
                result.mensagem()
        );
    }
}
