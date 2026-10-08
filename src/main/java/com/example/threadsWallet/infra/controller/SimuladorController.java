package com.example.threadswallet.infra.controller;

import com.example.threadswallet.application.dto.CarteiraIndividualResult;
import com.example.threadswallet.application.dto.SimulacaoResult;
import com.example.threadswallet.application.usecase.GerarMassaDadosUseCase;
import com.example.threadswallet.application.usecase.ListarCarteirasUseCase;
import com.example.threadswallet.application.usecase.ProcessarCarteiraIndividualUseCase;
import com.example.threadswallet.application.usecase.ProcessarMultiplasCarteirasUseCase;
import com.example.threadswallet.domain.carteira.ParametrosCalculo;
import com.example.threadswallet.domain.exception.DomainException;
import com.example.threadswallet.infra.controller.dto.CarteiraIndividualResponse;
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

    private final ProcessarMultiplasCarteirasUseCase processarMultiplasCarteirasUseCase;
    private final ProcessarCarteiraIndividualUseCase processarCarteiraIndividualUseCase;
    private final GerarMassaDadosUseCase gerarMassaDadosUseCase;
    private final ListarCarteirasUseCase listarCarteirasUseCase;

    public SimuladorController(
            ProcessarMultiplasCarteirasUseCase processarMultiplasCarteirasUseCase,
            ProcessarCarteiraIndividualUseCase processarCarteiraIndividualUseCase,
            GerarMassaDadosUseCase gerarMassaDadosUseCase,
            ListarCarteirasUseCase listarCarteirasUseCase
    ) {
        this.processarMultiplasCarteirasUseCase = processarMultiplasCarteirasUseCase;
        this.processarCarteiraIndividualUseCase = processarCarteiraIndividualUseCase;
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
            summary = "2. Calcular risco de múltiplas carteiras (em lote / batch)",
            description = "Dispara o cálculo de risco concorrente em lote para as carteiras cadastradas. Recebe o Value Object ParametrosCalculo via JSON body."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cálculo concluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Base vazia ou parâmetros inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno durante a execução concorrente")
    })
    @PostMapping("/executar")
    public ResponseEntity<SimulacaoResponse> processarMultiplasCarteiras(
            @Parameter(description = "Limite opcional de carteiras a processar (se omitido, processa todas as cadastradas)", example = "1000")
            @RequestParam(required = false) Integer limite,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Parâmetros de cálculo de risco (método e iterações)", required = true)
            @RequestBody(required = false) ParametrosCalculo parametros
    ) {
        if (parametros == null)
            throw new DomainException("Os parâmetros de cálculo de risco são obrigatórios.");

        SimulacaoResult result = processarMultiplasCarteirasUseCase.execute(limite, parametros);
        return ResponseEntity.ok(SimulacaoResponse.from(result));
    }

    @Operation(
            summary = "2.1. Calcular risco de uma carteira individual (sob demanda)",
            description = "Calcula o risco de uma carteira específica sob demanda. Recebe o Value Object ParametrosCalculo via JSON body."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cálculo individual concluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos ou carteira não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno durante a execução")
    })
    @PostMapping("/carteiras/{carteiraId}/executar")
    public ResponseEntity<CarteiraIndividualResponse> processarCarteiraIndividual(
            @Parameter(description = "ID da carteira a calcular", example = "1", required = true)
            @PathVariable Long carteiraId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Parâmetros de cálculo de risco (método e iterações)", required = true)
            @RequestBody(required = false) ParametrosCalculo parametros
    ) {
        if (parametros == null)
            throw new DomainException("Os parâmetros de cálculo de risco são obrigatórios.");

        CarteiraIndividualResult result = processarCarteiraIndividualUseCase.execute(carteiraId, parametros);
        return ResponseEntity.ok(CarteiraIndividualResponse.from(result));
    }

    @Operation(
            summary = "3. Listar carteiras e riscos calculados",
            description = "Retorna todas as carteiras cadastradas no banco com seus respectivos riscos calculados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    @GetMapping("/carteiras")
    public ResponseEntity<List<CarteiraResponse>> listarCarteiras() {
        List<CarteiraResponse> response = listarCarteirasUseCase.execute().stream()
                .map(CarteiraResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
