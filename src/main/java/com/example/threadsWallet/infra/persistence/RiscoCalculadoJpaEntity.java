package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.domain.carteira.MetodoCalculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "risco_calculado",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_risco_carteira_tipo", columnNames = {"carteira_id", "tipo"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiscoCalculadoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_id", nullable = false)
    private CarteiraJpaEntity carteira;

    @Column(name = "valor", nullable = false)
    private Double valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private MetodoCalculo tipo;
}
