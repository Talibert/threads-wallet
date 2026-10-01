package com.example.threadswallet;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Classe base abstrata para testes de integração de ponta a ponta.
 * Inicializa todo o contexto do Spring Boot, pools de concorrência e banco de dados.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test-integration.properties")
public abstract class IntegrationAbstractTests {
}
