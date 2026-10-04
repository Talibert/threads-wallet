package com.example.threadswallet.infra.controller.dto;

public record UploadArquivoResponse(
        String nomeArquivo,
        long tamanhoBytes,
        String extensao,
        String mensagem
) {
    public static UploadArquivoResponse sucesso(String nomeArquivo, long tamanhoBytes, String extensao) {
        return new UploadArquivoResponse(
                nomeArquivo,
                tamanhoBytes,
                extensao,
                "Arquivo recebido com sucesso e validado. Pronto para processamento."
        );
    }
}
