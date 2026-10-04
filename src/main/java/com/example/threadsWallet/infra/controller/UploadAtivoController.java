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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Upload de Ativos", description = "Endpoints para ingestão de arquivos de ativos")
@RestController
@RequestMapping("/api/ativos")
public class UploadAtivoController {

    private final ProcessarArquivoAtivosUseCase processarArquivoAtivosUseCase;

    public UploadAtivoController(ProcessarArquivoAtivosUseCase processarArquivoAtivosUseCase) {
        this.processarArquivoAtivosUseCase = processarArquivoAtivosUseCase;
    }

    @Operation(
            summary = "Upload de arquivo de ativos (.csv ou .txt)",
            description = "Recebe um arquivo multipart (.csv ou .txt) com os ativos separados por ponto e vírgula e valida o formato na borda HTTP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo recebido e validado com sucesso",
                    content = @Content(schema = @Schema(implementation = UploadArquivoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente, vazio ou formato inválido")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadArquivoResponse> uploadArquivo(
            @Parameter(description = "Arquivo .csv ou .txt a ser enviado", required = true)
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        ArquivoUpload arquivoUpload = ArquivoUpload.from(arquivo);
        ProcessamentoAtivosResult resultado = processarArquivoAtivosUseCase.execute(arquivoUpload.inputStream());

        return ResponseEntity.ok(UploadArquivoResponse.from(arquivoUpload, resultado));
    }
}
