package com.example.threadswallet;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classe base abstrata para testes de persistência / repositório.
 * Inicializa exclusivamente a camada JPA/Hibernate e o banco de dados via Flyway.
 * Escaneia automaticamente todos os componentes do pacote infra.persistence.
 */
@DataJpaTest
@ComponentScan(basePackages = "com.example.threadswallet.infra.persistence")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test-repository.properties")
@Transactional
public abstract class RepositoryAbstractTests {
}
