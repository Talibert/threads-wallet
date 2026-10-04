package com.example.threadswallet.infra.controller;

import com.example.threadswallet.ControllerAbstractTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadAtivoControllerTest extends ControllerAbstractTests {

    @Test
    @DisplayName("POST /api/ativos/upload com arquivo CSV válido deve retornar HTTP 200")
    void deveFazerUploadDeArquivoCsvComSucesso() throws Exception {
        byte[] conteudo = "1;PETR4;38.50;0.22\n1;VALE3;62.10;0.18".getBytes();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "carteira_ativos.csv",
                "text/csv",
                conteudo
        );

        mockMvc.perform(multipart("/api/ativos/upload").file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("carteira_ativos.csv"))
                .andExpect(jsonPath("$.extensao").value("csv"))
                .andExpect(jsonPath("$.tamanhoBytes").value(conteudo.length))
                .andExpect(jsonPath("$.mensagem").value("Arquivo recebido com sucesso e validado. Pronto para processamento."));
    }

    @Test
    @DisplayName("POST /api/ativos/upload com arquivo TXT válido deve retornar HTTP 200")
    void deveFazerUploadDeArquivoTxtComSucesso() throws Exception {
        byte[] conteudo = "2;ITUB4;34.20;0.15\n2;BBDC4;14.80;0.19".getBytes();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "lote_ativos.txt",
                "text/plain",
                conteudo
        );

        mockMvc.perform(multipart("/api/ativos/upload").file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("lote_ativos.txt"))
                .andExpect(jsonPath("$.extensao").value("txt"))
                .andExpect(jsonPath("$.tamanhoBytes").value(conteudo.length));
    }

    @Test
    @DisplayName("POST /api/ativos/upload com arquivo vazio deve retornar HTTP 400 Bad Request")
    void deveRejeitarArquivoVazio() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "vazio.csv",
                "text/csv",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/ativos/upload").file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição Inválida"))
                .andExpect(jsonPath("$.message").value("O arquivo de upload não pode ser nulo ou vazio."));
    }

    @Test
    @DisplayName("POST /api/ativos/upload com extensão não permitida deve retornar HTTP 400 Bad Request")
    void deveRejeitarArquivoComExtensaoInvalida() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "ativos.pdf",
                "application/pdf",
                "%PDF-1.4 dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/ativos/upload").file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição Inválida"))
                .andExpect(jsonPath("$.message").value("Formato de arquivo inválido ('.pdf'). São permitidos apenas arquivos .csv ou .txt."));
    }

    @Test
    @DisplayName("POST /api/ativos/upload sem extensão no nome do arquivo deve retornar HTTP 400 Bad Request")
    void deveRejeitarArquivoSemExtensao() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "arquivo_sem_extensao",
                "text/plain",
                "1;PETR4;38.50;0.22".getBytes()
        );

        mockMvc.perform(multipart("/api/ativos/upload").file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição Inválida"))
                .andExpect(jsonPath("$.message").value("Formato de arquivo inválido ('.'). São permitidos apenas arquivos .csv ou .txt."));
    }
}
