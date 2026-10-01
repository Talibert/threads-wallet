package com.example.threadswallet.infra.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ativo")
@Getter
@Setter
@NoArgsConstructor
public class AtivoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_id", nullable = false)
    private CarteiraJpaEntity carteira;

    @Column(name = "ticker", nullable = false)
    private String ticker;

    @Column(name = "valor_atual", nullable = false)
    private Double valorAtual;

    @Column(name = "taxa_volatilidade", nullable = false)
    private Double taxaVolatilidade;

    public AtivoJpaEntity(CarteiraJpaEntity carteira, String ticker, Double valorAtual, Double taxaVolatilidade) {
        this.carteira = carteira;
        this.ticker = ticker;
        this.valorAtual = valorAtual;
        this.taxaVolatilidade = taxaVolatilidade;
    }
}
