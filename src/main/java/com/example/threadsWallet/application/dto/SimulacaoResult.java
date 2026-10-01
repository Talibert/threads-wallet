package com.example.threadswallet.application.dto;

public record SimulacaoResult(
        int totalCarteirasProcessadas,
        long tempoTotalMs,
        double tempoMedioPorCarteiraMs,
        int nucleosCpuDisponiveis,
        int iteracoesMonteCarloPorCarteira,
        String mensagem
) {}
