package com.example.threadswallet.infra.controller;

import com.example.threadswallet.ControllerAbstractTests;
import com.example.threadswallet.domain.carteira.CarteiraRepository;
import com.example.threadswallet.infra.calculation.MonteCarloCalculadoraRiscoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
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
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Domínio"));
    }

    @Test
    @DisplayName("Fluxo completo sob demanda: Gerar via controller e depois calcular via controller verificando SpyBean")
    void deveExecutarFluxoCompletoViaApi() throws Exception {
        // 1. Gera massa via controller
        mockMvc.perform(post("/api/simulador/massa-dados")
                        .param("totalCarteiras", "10")
                        .param("limparAntes", "true"))
                .andExpect(status().isOk());

        // 2. Calcula risco via controller passando iterações customizadas
        mockMvc.perform(post("/api/simulador/executar")
                        .param("limite", "10")
                        .param("iteracoes", "5000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCarteirasProcessadas").value(10))
                .andExpect(jsonPath("$.tempoTotalMs").isNumber())
                .andExpect(jsonPath("$.nucleosCpuDisponiveis").isNumber())
                .andExpect(jsonPath("$.iteracoesMonteCarloPorCarteira").value(5000));

        // 3. Verifica via Mockito que o SpyBean da calculadora foi chamado com as 5.000 iterações
        verify(calculadoraRisco, atLeastOnce()).calcularRisco(anyList(), org.mockito.ArgumentMatchers.eq(5000));

        // 4. Consulta lista de carteiras
        mockMvc.perform(get("/api/simulador/carteiras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }
}
