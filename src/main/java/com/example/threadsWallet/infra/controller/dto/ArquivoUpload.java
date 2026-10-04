package com.example.threadswallet.infra.controller.dto;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public record ArquivoUpload(
        String nomeOriginal,
        String extensao,
        InputStream inputStream,
        long tamanhoBytes
) {
    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("csv", "txt");

    public static ArquivoUpload from(MultipartFile arquivo) {
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

        try {
            return new ArquivoUpload(nomeOriginal, extensao, arquivo.getInputStream(), arquivo.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o fluxo de dados do arquivo enviado", e);
        }
    }

    private static String extrairExtensao(String nomeArquivo) {
        int ultimoPonto = nomeArquivo.lastIndexOf('.');
        if (ultimoPonto == -1 || ultimoPonto == nomeArquivo.length() - 1) {
            return "";
        }
        return nomeArquivo.substring(ultimoPonto + 1);
    }
}
