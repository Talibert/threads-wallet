package com.example.threadswallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Classe base abstrata para testes de Controllers REST.
 * Herda o contexto completo de integração e disponibiliza o MockMvc para requisições HTTP simuladas.
 */
@AutoConfigureMockMvc
public abstract class ControllerAbstractTests extends IntegrationAbstractTests {

    @Autowired
    protected MockMvc mockMvc;
}
