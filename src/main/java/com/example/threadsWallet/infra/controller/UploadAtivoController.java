package com.example.threadswallet.infra.controller;

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

import java.util.Set;

@Tag(name = "Upload de Ativos", description = "Endpoints para ingestão de arquivos de ativos")
@RestController
@RequestMapping("/api/ativos")
public class UploadAtivoController {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("csv", "txt");

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
        validarArquivo(arquivo);

        String nomeOriginal = arquivo.getOriginalFilename();
        String extensao = extrairExtensao(nomeOriginal);

        return ResponseEntity.ok(
                UploadArquivoResponse.sucesso(nomeOriginal, arquivo.getSize(), extensao)
        );
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty())
            throw new IllegalArgumentException("O arquivo de upload não pode ser nulo ou vazio.");

        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank())
            throw new IllegalArgumentException("O nome do arquivo não foi informado.");

        String extensao = extrairExtensao(nomeOriginal);
        if (!EXTENSOES_PERMITIDAS.contains(extensao.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("Formato de arquivo inválido ('.%s'). São permitidos apenas arquivos .csv ou .txt.", extensao)
            );
        }
    }

    private String extrairExtensao(String nomeArquivo) {
        int ultimoPonto = nomeArquivo.lastIndexOf('.');
        if (ultimoPonto == -1 || ultimoPonto == nomeArquivo.length() - 1)
            return "";

        return nomeArquivo.substring(ultimoPonto + 1);
    }
}
