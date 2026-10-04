package com.example.threadswallet.infra.controller.dto;

import com.example.threadswallet.application.dto.ProcessamentoAtivosResult;

public record UploadArquivoResponse(
        String nomeArquivo,
        long tamanhoBytes,
        String extensao,
        int totalAtivosProcessados,
        long tempoProcessamentoMs,
        String mensagem
) {
    public static UploadArquivoResponse from(
            ArquivoUpload upload,
            ProcessamentoAtivosResult result
    ) {
        return new UploadArquivoResponse(
                upload.nomeOriginal(),
                upload.tamanhoBytes(),
                upload.extensao(),
                result.totalAtivosProcessados(),
                result.tempoProcessamentoMs(),
                result.mensagem()
        );
    }
}
