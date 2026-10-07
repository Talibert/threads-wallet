package com.example.threadswallet.application.dto;

import com.example.threadswallet.domain.carteira.MetodoCalculo;

public record CarteiraIndividualResult(
        Long carteiraId,
        Double riscoCalculado,
        MetodoCalculo metodo,
        long tempoExecucaoMs,
        int nucleosCpuUtilizados,
        int iteracoes
) {}
