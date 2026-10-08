package com.example.threadswallet.infra.controller;

import com.example.threadswallet.ControllerAbstractTests;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.infra.calculation.MonteCarloCalculadoraRiscoImpl;
import com.example.threadswallet.infra.calculation.VarParametricoCalculadoraRiscoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SimuladorControllerTest extends ControllerAbstractTests {

    @Autowired
    private CarteiraRepository carteiraRepository;

    @MockitoSpyBean
    private MonteCarloCalculadoraRiscoImpl calculadoraRisco;

    @MockitoSpyBean
    private VarParametricoCalculadoraRiscoImpl parametricoCalculadora;

    @BeforeEach
    void setUp() {
        carteiraRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/simulador/massa-dados deve gerar carteiras sob demanda")
    void deveGerarMassaViaApi() throws Exception {
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "15")
                        .param("limparAntes", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/simulador/executar deve falhar com 400 se base estiver vazia")
    void deveFalharExecutarSeBaseVazia() throws Exception {
        mockMvc.perform(post("/api/simulador/executar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "metodo": "MONTE_CARLO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Domínio"));
    }

    @Test
    @DisplayName("Fluxo completo sob demanda: Gerar via controller e depois calcular via Monte Carlo")
    void deveExecutarFluxoCompletoViaApi() throws Exception {
        // 1. Gera massa via controller
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "10")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        // 2. Calcula risco via controller passando body com iterações customizadas
        mockMvc.perform(post("/api/simulador/executar")
                        .param("limite", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "metodo": "MONTE_CARLO",
                                    "iteracoes": 5000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCarteirasProcessadas").value(10))
                .andExpect(jsonPath("$.tempoTotalMs").isNumber())
                .andExpect(jsonPath("$.nucleosCpuDisponiveis").isNumber())
                .andExpect(jsonPath("$.iteracoesMonteCarloPorCarteira").value(5000))
                .andExpect(jsonPath("$.metodoCalculo").value("MONTE_CARLO"));

        // 3. Verifica via Mockito que o SpyBean da calculadora Monte Carlo foi chamado com 5.000 iterações
        verify(calculadoraRisco, atLeastOnce()).calcularRisco(anyList(), eq(5000));

        // 4. Consulta lista de carteiras
        mockMvc.perform(get("/api/simulador/carteiras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    @DisplayName("POST /api/simulador/executar com JSON Body de Monte Carlo")
    void deveExecutarViaApiComJsonBodyMonteCarlo() throws Exception {
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "5")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        String jsonBody = """
                {
                    "metodo": "MONTE_CARLO",
                    "iteracoes": 2500
                }
                """;

        mockMvc.perform(post("/api/simulador/executar")
                        .param("limite", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCarteirasProcessadas").value(5))
                .andExpect(jsonPath("$.iteracoesMonteCarloPorCarteira").value(2500))
                .andExpect(jsonPath("$.metodoCalculo").value("MONTE_CARLO"));

        verify(calculadoraRisco, atLeastOnce()).calcularRisco(anyList(), eq(2500));
    }

    @Test
    @DisplayName("POST /api/simulador/executar com metodo=VAR_PARAMETRICO deve acionar a estratégia analítica")
    void deveExecutarViaApiComVarParametrico() throws Exception {
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "5")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/simulador/executar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "metodo": "VAR_PARAMETRICO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCarteirasProcessadas").value(5))
                .andExpect(jsonPath("$.metodoCalculo").value("VAR_PARAMETRICO"));

        verify(parametricoCalculadora, atLeastOnce()).calcularRisco(anyList(), anyInt());
    }

    @Test
    @DisplayName("POST /api/simulador/executar sem body deve retornar HTTP 400 Bad Request")
    void deveRetornar400QuandoBodyNulo() throws Exception {
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "1")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/simulador/executar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Domínio"))
                .andExpect(jsonPath("$.message").value("Os parâmetros de cálculo de risco são obrigatórios."));
    }

    @Test
    @DisplayName("POST /api/simulador/executar com body vazio sem metodo deve retornar HTTP 400 Bad Request")
    void deveRetornar400QuandoMetodoNaoInformado() throws Exception {
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "1")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/simulador/executar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Domínio"))
                .andExpect(jsonPath("$.message").value("O método de cálculo de risco é obrigatório."));
    }

    @Test
    @DisplayName("POST /api/simulador/carteiras/{carteiraId}/executar com MONTE_CARLO deve processar individualmente com paralelismo")
    void deveCalcularCarteiraIndividualViaApiMonteCarlo() throws Exception {
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "1")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        Long carteiraId = carteiraRepository.findAllIds().getFirst();

        mockMvc.perform(post("/api/simulador/carteiras/" + carteiraId + "/executar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "metodo": "MONTE_CARLO",
                                    "iteracoes": 10000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carteiraId").value(carteiraId))
                .andExpect(jsonPath("$.riscoCalculado").isNumber())
                .andExpect(jsonPath("$.metodoCalculo").value("MONTE_CARLO"))
                .andExpect(jsonPath("$.nucleosCpuUtilizados").isNumber())
                .andExpect(jsonPath("$.iteracoes").value(10000));
    }

    @Test
    @DisplayName("POST /api/simulador/carteiras/{carteiraId}/executar com VAR_PARAMETRICO deve processar com 1 thread")
    void deveCalcularCarteiraIndividualViaApiVarParametrico() throws Exception {
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "1")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        Long carteiraId = carteiraRepository.findAllIds().getFirst();

        mockMvc.perform(post("/api/simulador/carteiras/" + carteiraId + "/executar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "metodo": "VAR_PARAMETRICO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carteiraId").value(carteiraId))
                .andExpect(jsonPath("$.riscoCalculado").isNumber())
                .andExpect(jsonPath("$.metodoCalculo").value("VAR_PARAMETRICO"))
                .andExpect(jsonPath("$.nucleosCpuUtilizados").value(1));
    }

    @Test
    @DisplayName("POST /api/simulador/carteiras/{carteiraId}/executar sem body deve retornar HTTP 400 Bad Request")
    void deveRetornar400CarteiraIndividualSemBody() throws Exception {
        mockMvc.perform(post("/api/simulador/carteiras/1/executar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Domínio"))
                .andExpect(jsonPath("$.message").value("Os parâmetros de cálculo de risco são obrigatórios."));
    }
}
