package com.example.threadswallet.domain.carteira;

import com.example.threadswallet.domain.exception.DomainException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Carteira {

    private final Long id;
    private final String nomeCliente;
    private final List<Ativo> ativos = new ArrayList<>();

    private Carteira(Long id, String nomeCliente, List<Ativo> ativos) {
        if (nomeCliente == null || nomeCliente.isBlank())
            throw new DomainException("O nome do cliente não pode ser vazio.");
        this.id = id;
        this.nomeCliente = nomeCliente.trim();
        if (ativos != null)
            this.ativos.addAll(ativos);
    }

    public static Carteira create(String nomeCliente) {
        return new Carteira(null, nomeCliente, null);
    }

    public static Carteira restore(Long id, String nomeCliente, List<Ativo> ativos) {
        return new Carteira(id, nomeCliente, ativos);
    }

    public void adicionarAtivo(Ativo ativo) {
        if (ativo == null)
            throw new DomainException("Ativo não pode ser nulo.");
        this.ativos.add(ativo);
    }

    public List<Ativo> getAtivos() {
        return Collections.unmodifiableList(ativos);
    }

    public double getValorTotal() {
        return ativos.stream()
                .mapToDouble(Ativo::getValorAtual)
                .sum();
    }
}
