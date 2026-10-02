package com.example.threadswallet.application.dto;

import com.example.threadswallet.domain.carteira.MetodoCalculo;

public record SimulacaoResult(
        int totalCarteirasProcessadas,
        long tempoTotalMs,
        double tempoMedioPorCarteiraMs,
        int nucleosCpuDisponiveis,
        int iteracoesMonteCarloPorCarteira,
        MetodoCalculo metodoCalculo,
        String mensagem
) {}
