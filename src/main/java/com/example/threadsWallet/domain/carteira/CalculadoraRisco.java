package com.example.threadswallet.domain.carteira;

import java.util.List;

public interface CalculadoraRisco {

    Double calcularRisco(List<Ativo> ativos, int iteracoes);

    MetodoCalculo getMetodo();

    boolean isParalelizavel();

    AmostraRisco calcularAmostra(List<Ativo> ativos, int iteracoes);

    Double consolidarAmostras(List<AmostraRisco> amostras);
}
