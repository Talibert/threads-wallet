package com.example.threadswallet.infra.controller;

import com.example.threadswallet.ControllerAbstractTests;
import com.example.threadswallet.domain.carteira.AtivoRepository;
import com.example.threadswallet.domain.carteira.Carteira;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadAtivoControllerTest extends ControllerAbstractTests {

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private AtivoRepository ativoRepository;

    private Long carteiraId1;
    private Long carteiraId2;

    @BeforeEach
    void setUp() {
        ativoRepository.deleteAll();
        carteiraRepository.deleteAll();
        carteiraId1 = carteiraRepository.save(Carteira.create("Cliente 1")).getId();
        carteiraId2 = carteiraRepository.save(Carteira.create("Cliente 2")).getId();
    }

    @Test
    @DisplayName("POST /api/carteiras/{carteiraId}/ativos/upload com arquivo CSV válido deve salvar ativos e retornar HTTP 200")
    void deveFazerUploadDeArquivoCsvComSucesso() throws Exception {
        byte[] conteudo = "PETR4;38.50;0.22\nVALE3;62.10;0.18".getBytes();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "carteira_ativos.csv",
                "text/csv",
                conteudo
        );

        mockMvc.perform(multipart("/api/carteiras/{carteiraId}/ativos/upload", carteiraId1).file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("carteira_ativos.csv"))
                .andExpect(jsonPath("$.extensao").value("csv"))
                .andExpect(jsonPath("$.tamanhoBytes").value(conteudo.length))
                .andExpect(jsonPath("$.totalAtivosProcessados").value(2))
                .andExpect(jsonPath("$.tempoProcessamentoMs").isNumber())
                .andExpect(jsonPath("$.mensagem").value("2 ativos processados e salvos com sucesso."));
    }

    @Test
    @DisplayName("POST /api/carteiras/{carteiraId}/ativos/upload com arquivo TXT válido deve salvar ativos e retornar HTTP 200")
    void deveFazerUploadDeArquivoTxtComSucesso() throws Exception {
        byte[] conteudo = "ITUB4;34.20;0.15\nBBDC4;14.80;0.19".getBytes();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "lote_ativos.txt",
                "text/plain",
                conteudo
        );

        mockMvc.perform(multipart("/api/carteiras/{carteiraId}/ativos/upload", carteiraId2).file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("lote_ativos.txt"))
                .andExpect(jsonPath("$.extensao").value("txt"))
                .andExpect(jsonPath("$.tamanhoBytes").value(conteudo.length))
                .andExpect(jsonPath("$.totalAtivosProcessados").value(2))
                .andExpect(jsonPath("$.tempoProcessamentoMs").isNumber())
                .andExpect(jsonPath("$.mensagem").value("2 ativos processados e salvos com sucesso."));
    }

    @Test
    @DisplayName("POST /api/carteiras/{carteiraId}/ativos/upload com carteira inexistente deve retornar HTTP 400 Bad Request")
    void deveRejeitarQuandoCarteiraInexistente() throws Exception {
        byte[] conteudo = "PETR4;38.50;0.22".getBytes();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "carteira_invalida.csv",
                "text/csv",
                conteudo
        );

        mockMvc.perform(multipart("/api/carteiras/{carteiraId}/ativos/upload", 999999L).file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Domínio"))
                .andExpect(jsonPath("$.message").value("Carteira com ID 999999 não foi encontrada."));

        assertThat(ativoRepository.findByCarteiraId(999999L)).isEmpty();
    }

    @Test
    @DisplayName("POST /api/carteiras/{carteiraId}/ativos/upload com arquivo vazio deve retornar HTTP 400 Bad Request")
    void deveRejeitarArquivoVazio() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "vazio.csv",
                "text/csv",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/carteiras/{carteiraId}/ativos/upload", carteiraId1).file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição Inválida"))
                .andExpect(jsonPath("$.message").value("O arquivo de upload não pode ser nulo ou vazio."));
    }

    @Test
    @DisplayName("POST /api/carteiras/{carteiraId}/ativos/upload com extensão não permitida deve retornar HTTP 400 Bad Request")
    void deveRejeitarArquivoComExtensaoInvalida() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "ativos.pdf",
                "application/pdf",
                "%PDF-1.4 dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/carteiras/{carteiraId}/ativos/upload", carteiraId1).file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição Inválida"))
                .andExpect(jsonPath("$.message").value("Formato de arquivo inválido ('.pdf'). São permitidos apenas arquivos .csv ou .txt."));
    }

    @Test
    @DisplayName("POST /api/carteiras/{carteiraId}/ativos/upload sem extensão no nome do arquivo deve retornar HTTP 400 Bad Request")
    void deveRejeitarArquivoSemExtensao() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "arquivo_sem_extensao",
                "text/plain",
                "PETR4;38.50;0.22".getBytes()
        );

        mockMvc.perform(multipart("/api/carteiras/{carteiraId}/ativos/upload", carteiraId1).file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requisição Inválida"))
                .andExpect(jsonPath("$.message").value("Formato de arquivo inválido ('.'). São permitidos apenas arquivos .csv ou .txt."));
    }
}
