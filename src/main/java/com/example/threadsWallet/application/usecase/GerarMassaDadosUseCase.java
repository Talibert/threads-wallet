package com.example.threadswallet.application.usecase;

import com.example.threadswallet.domain.carteira.Ativo;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class GerarMassaDadosUseCase {

    private static final String[] TICKERS = {
            "PETR4", "VALE3", "ITUB4", "BBDC4", "BBAS3",
            "WEGE3", "RENT3", "MGLU3", "PRIO3", "AAPL34",
            "NVDC34", "GGBR4", "SUZB3", "JBSS3", "RADL3"
    };

    private final CarteiraRepository carteiraRepository;
    private final Random random = new Random();

    public GerarMassaDadosUseCase(CarteiraRepository carteiraRepository) {
        this.carteiraRepository = carteiraRepository;
    }

    public int execute(int totalCarteiras, int minAtivosPorCarteira, int maxAtivosPorCarteira, boolean limparAntes) {
        if (limparAntes)
            carteiraRepository.deleteAll();

        List<Carteira> carteiras = new ArrayList<>(totalCarteiras);

        for (int i = 1; i <= totalCarteiras; i++) {
            Carteira carteira = Carteira.create("Cliente #" + i);

            int quantidadeAtivos = minAtivosPorCarteira + random.nextInt(maxAtivosPorCarteira - minAtivosPorCarteira + 1);
            for (int a = 0; a < quantidadeAtivos; a++) {
                String ticker = TICKERS[random.nextInt(TICKERS.length)];
                double valorAtual = Math.round((1000.0 + (random.nextDouble() * 49000.0)) * 100.0) / 100.0;
                double taxaVolatilidade = Math.round((0.05 + (random.nextDouble() * 0.35)) * 10000.0) / 10000.0;

                carteira.adicionarAtivo(Ativo.create(null, ticker, valorAtual, taxaVolatilidade));
            }

            carteiras.add(carteira);
        }

        carteiraRepository.saveAll(carteiras);
        return carteiras.size();
    }
}
