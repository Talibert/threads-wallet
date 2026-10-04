package com.example.threadswallet.infra.controller;

import com.example.threadswallet.application.dto.ProcessamentoAtivosResult;
import com.example.threadswallet.application.usecase.ProcessarArquivoAtivosUseCase;
import com.example.threadswallet.infra.controller.dto.ArquivoUpload;
import com.example.threadswallet.infra.controller.dto.UploadArquivoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.domain.exception.DomainException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Upload de Ativos", description = "Endpoints para ingestão de arquivos de ativos por carteira")
@RestController
@RequestMapping("/api/carteiras")
public class UploadAtivoController {

    private final ProcessarArquivoAtivosUseCase processarArquivoAtivosUseCase;
    private final CarteiraRepository carteiraRepository;

    public UploadAtivoController(
            ProcessarArquivoAtivosUseCase processarArquivoAtivosUseCase,
            CarteiraRepository carteiraRepository
    ) {
        this.processarArquivoAtivosUseCase = processarArquivoAtivosUseCase;
        this.carteiraRepository = carteiraRepository;
    }

    @Operation(
            summary = "Upload de arquivo de ativos para uma carteira (.csv ou .txt)",
            description = "Recebe um arquivo multipart (.csv ou .txt) com os ativos separados por ponto e vírgula e associa todos à carteira informada no path."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo recebido e ativos salvos com sucesso",
                    content = @Content(schema = @Schema(implementation = UploadArquivoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente, vazio, formato inválido ou carteira inexistente")
    })
    @PostMapping(value = "/{carteiraId}/ativos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadArquivoResponse> uploadArquivo(
            @Parameter(description = "ID da carteira de destino", required = true)
            @PathVariable("carteiraId") Long carteiraId,
            @Parameter(description = "Arquivo .csv ou .txt a ser enviado", required = true)
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        if (!carteiraRepository.existsById(carteiraId)) {
            throw new DomainException(String.format("Carteira com ID %d não foi encontrada.", carteiraId));
        }

        ArquivoUpload arquivoUpload = ArquivoUpload.from(arquivo);
        ProcessamentoAtivosResult resultado = processarArquivoAtivosUseCase.execute(carteiraId, arquivoUpload.inputStream());

        return ResponseEntity.ok(UploadArquivoResponse.from(arquivoUpload, resultado));
    }
}
