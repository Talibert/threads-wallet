package com.example.threadswallet.infra.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carteira")
@Getter
@Setter
@NoArgsConstructor
public class CarteiraJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_cliente", nullable = false)
    private String nomeCliente;

    @Column(name = "risco_calculado")
    private Double riscoCalculado;

    @OneToMany(mappedBy = "carteira", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AtivoJpaEntity> ativos = new ArrayList<>();

    public CarteiraJpaEntity(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public void adicionarAtivo(AtivoJpaEntity ativo) {
        ativos.add(ativo);
        ativo.setCarteira(this);
    }
}
