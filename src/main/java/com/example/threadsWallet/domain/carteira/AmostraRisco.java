package com.example.threadswallet.domain.carteira;

/**
 * Representa uma amostra parcial de simulação estocástica (Map-Reduce).
 * Agrega a soma dos choques simulados, a soma dos quadrados e o número de iterações executadas.
 */
public record AmostraRisco(double somaPerdas, double somaQuadrados, int iteracoes) {

    public AmostraRisco {
        if (iteracoes < 0)
            throw new IllegalArgumentException("O número de iterações da amostra não pode ser negativo.");
    }
}
