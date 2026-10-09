package com.example.threadswallet.domain.carteira;

/**
 * Value Object que encapsula o resultado numérico de uma execução de cálculo de risco,
 * acompanhado da quantidade de núcleos de CPU efetivamente alocados/utilizados na computação.
 */
public record ResultadoCalculo(Double valor, int nucleosUtilizados) {

    public ResultadoCalculo {
        if (valor == null)
            valor = 0.0;

        if (nucleosUtilizados < 1)
            nucleosUtilizados = 1;
    }
}
