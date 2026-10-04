package com.example.threadswallet.application.dto;

import com.example.threadswallet.domain.carteira.Ativo;

import java.util.List;

public record ProcessamentoAtivosResult(
        int totalAtivosProcessados,
        long tempoProcessamentoMs,
        List<Ativo> ativos,
        String mensagem
) {}
