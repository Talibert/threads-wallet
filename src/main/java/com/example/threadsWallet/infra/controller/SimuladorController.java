package com.example.threadswallet.infra.controller;

import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.application.usecase.ExecutarSimulacaoCargaUseCase;
import com.example.threadswallet.application.usecase.GerarMassaDadosUseCase;
import com.example.threadswallet.application.usecase.ListarCarteirasUseCase;
import com.example.threadswallet.infra.controller.dto.CarteiraResponse;
import com.example.threadswallet.infra.controller.dto.SimulacaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Simulador de Concorrência", description = "Endpoints para simulação de risco de portfólios com isolamento de I/O em Virtual Threads e CPU em Pool Fixo")
@RestController
@RequestMapping("/api/simulador")
public class SimuladorController {

    private final ExecutarSimulacaoCargaUseCase executarSimulacaoCargaUseCase;
    private final GerarMassaDadosUseCase gerarMassaDadosUseCase;
    private final ListarCarteirasUseCase listarCarteirasUseCase;

    public SimuladorController(
            ExecutarSimulacaoCargaUseCase executarSimulacaoCargaUseCase,
            GerarMassaDadosUseCase gerarMassaDadosUseCase,
            ListarCarteirasUseCase listarCarteirasUseCase
    ) {
        this.executarSimulacaoCargaUseCase = executarSimulacaoCargaUseCase;
        this.gerarMassaDadosUseCase = gerarMassaDadosUseCase;
        this.listarCarteirasUseCase = listarCarteirasUseCase;
    }

    @Operation(
            summary = "1. Gerar massa de carteiras e ativos",
            description = "Gera uma quantidade definida de carteiras no banco de dados, cada uma contendo de 3 a 5 ativos aleatórios. Executado exclusivamente sob demanda por esta chamada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Massa gerada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    })
    @PostMapping("/massa-dados")
    public ResponseEntity<String> gerarMassa(
            @Parameter(description = "Total de carteiras a gerar", example = "1000")
            @RequestParam(defaultValue = "1000") int totalCarteiras,
            @Parameter(description = "Se verdadeiro, limpa a base antes de gerar", example = "true")
            @RequestParam(defaultValue = "true") boolean limparAntes
    ) {
        int geradas = gerarMassaDadosUseCase.execute(totalCarteiras, 3, 5, limparAntes);
        return ResponseEntity.ok(String.format("Massa de dados gerada com sucesso: %d carteiras criadas no banco.", geradas));
    }

    @Operation(
            summary = "2. Calcular risco das carteiras cadastradas",
            description = "Dispara o cálculo de risco concorrente apenas para as carteiras já presentes no banco. Para cada carteira, uma Virtual Thread exclusiva busca os ativos, despacha o cálculo de Monte Carlo (com iterações passadas por parâmetro) para o Pool de CPU e atualiza o risco no banco."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cálculo concluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Base vazia ou parâmetros inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno durante a execução concorrente")
    })
    @PostMapping("/executar")
    public ResponseEntity<SimulacaoResponse> executar(
            @Parameter(description = "Limite opcional de carteiras a processar (se omitido, processa todas as cadastradas)", example = "1000")
            @RequestParam(required = false) Integer limite,
            @Parameter(description = "Número opcional de iterações do Monte Carlo por carteira (padrão: 100000)", example = "100000")
            @RequestParam(required = false) Integer iteracoes
    ) {
        SimulacaoResult result = executarSimulacaoCargaUseCase.execute(limite, iteracoes);
        return ResponseEntity.ok(SimulacaoResponse.from(result));
    }

    @Operation(
            summary = "3. Listar carteiras e riscos calculados",
            description = "Retorna todas as carteiras cadastradas no banco com seus respectivos riscos calculados."
    )
    @GetMapping("/carteiras")
    public ResponseEntity<List<CarteiraResponse>> listarCarteiras() {
        List<CarteiraResponse> response = listarCarteirasUseCase.execute().stream()
                .map(CarteiraResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
