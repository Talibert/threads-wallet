package com.example.threadswallet.domain.carteira;

import java.util.List;

public interface CalculadoraRisco {
    Double calcularRisco(List<Ativo> ativos, int iteracoes);
    MetodoCalculo getMetodo();
}
