package com.example.threadswallet.application.dto;

public record ProcessamentoAtivosResult(
        int totalAtivosProcessados,
        long tempoProcessamentoMs,
        String mensagem
) {}
