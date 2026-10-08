package com.example.threadswallet.domain.carteira;

import com.example.threadswallet.domain.exception.DomainException;

/**
 * Value Object imutável que encapsula os parâmetros de execução do cálculo de risco.
 * Agrupa o método selecionado (MONTE_CARLO, VAR_PARAMETRICO) e o número de iterações.
 */
public record ParametrosCalculo(MetodoCalculo metodo, Integer iteracoes) {

    public ParametrosCalculo {
        if (metodo == null)
            throw new DomainException("O método de cálculo de risco é obrigatório.");

        if (metodo == MetodoCalculo.VAR_PARAMETRICO)
            iteracoes = 0;
        else if (iteracoes == null || iteracoes <= 0)
            iteracoes = 100000;
    }

    public static ParametrosCalculo criar(MetodoCalculo metodo, Integer iteracoes) {
        return new ParametrosCalculo(metodo, iteracoes);
    }

    public static ParametrosCalculo monteCarlo(Integer iteracoes) {
        return new ParametrosCalculo(MetodoCalculo.MONTE_CARLO, iteracoes);
    }

    public static ParametrosCalculo varParametrico() {
        return new ParametrosCalculo(MetodoCalculo.VAR_PARAMETRICO, 0);
    }
}
