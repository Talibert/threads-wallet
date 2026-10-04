package com.example.threadswallet.infra.controller.dto;

import com.example.threadswallet.UnitAbstractTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArquivoUploadTest extends UnitAbstractTests {

    @Test
    @DisplayName("Deve instanciar ArquivoUpload com sucesso para arquivo CSV")
    void deveInstanciarComSucessoParaCsv() {
        byte[] conteudo = "PETR4;38.50;0.22".getBytes();
        MockMultipartFile file = new MockMultipartFile("arquivo", "ativos.csv", "text/csv", conteudo);

        ArquivoUpload upload = ArquivoUpload.from(file);

        assertThat(upload.nomeOriginal()).isEqualTo("ativos.csv");
        assertThat(upload.extensao()).isEqualTo("csv");
        assertThat(upload.tamanhoBytes()).isEqualTo(conteudo.length);
        assertThat(upload.inputStream()).isNotNull();
    }

    @Test
    @DisplayName("Deve instanciar ArquivoUpload com sucesso para arquivo TXT")
    void deveInstanciarComSucessoParaTxt() {
        byte[] conteudo = "1;VALE3;62.10;0.18".getBytes();
        MockMultipartFile file = new MockMultipartFile("arquivo", "carteira.txt", "text/plain", conteudo);

        ArquivoUpload upload = ArquivoUpload.from(file);

        assertThat(upload.nomeOriginal()).isEqualTo("carteira.txt");
        assertThat(upload.extensao()).isEqualTo("txt");
        assertThat(upload.tamanhoBytes()).isEqualTo(conteudo.length);
        assertThat(upload.inputStream()).isNotNull();
    }

    @Test
    @DisplayName("Deve rejeitar arquivo nulo")
    void deveRejeitarArquivoNulo() {
        assertThatThrownBy(() -> ArquivoUpload.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O arquivo de upload não pode ser nulo ou vazio.");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo vazio")
    void deveRejeitarArquivoVazio() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "vazio.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> ArquivoUpload.from(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O arquivo de upload não pode ser nulo ou vazio.");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo sem nome original")
    void deveRejeitarArquivoSemNome() {
        MockMultipartFile file = new MockMultipartFile("arquivo", null, "text/csv", "conteudo".getBytes());

        assertThatThrownBy(() -> ArquivoUpload.from(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O nome do arquivo não foi informado.");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo com nome em branco")
    void deveRejeitarArquivoComNomeEmBranco() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "   ", "text/csv", "conteudo".getBytes());

        assertThatThrownBy(() -> ArquivoUpload.from(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O nome do arquivo não foi informado.");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo com extensão não permitida")
    void deveRejeitarArquivoComExtensaoNaoPermitida() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "dados.pdf", "application/pdf", "conteudo".getBytes());

        assertThatThrownBy(() -> ArquivoUpload.from(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato de arquivo inválido ('.pdf'). São permitidos apenas arquivos .csv ou .txt.");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo sem extensão")
    void deveRejeitarArquivoSemExtensao() {
        MockMultipartFile file = new MockMultipartFile("arquivo", "dados_sem_extensao", "text/plain", "conteudo".getBytes());

        assertThatThrownBy(() -> ArquivoUpload.from(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato de arquivo inválido ('.'). São permitidos apenas arquivos .csv ou .txt.");
    }

    @Test
    @DisplayName("Deve lançar RuntimeException caso ocorra IOException ao obter InputStream")
    void deveLancarExcecaoQuandoOcorrerErroDeIo() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("ativos.csv");
        when(file.getInputStream()).thenThrow(new IOException("Falha simulada de leitura"));

        assertThatThrownBy(() -> ArquivoUpload.from(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao ler o fluxo de dados do arquivo enviado");
    }
}
