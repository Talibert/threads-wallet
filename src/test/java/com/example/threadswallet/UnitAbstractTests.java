package com.example.threadswallet;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

/**
 * Classe base abstrata para testes unitários.
 * Não carrega o contexto do Spring nem inicializa banco de dados.
 */
@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test-unit.properties")
public abstract class UnitAbstractTests {
}
