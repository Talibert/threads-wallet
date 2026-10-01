package com.example.threadswallet;

import com.example.threadswallet.infra.persistence.CarteiraRepositoryImpl;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classe base abstrata para testes de persistência / repositório.
 * Inicializa exclusivamente a camada JPA/Hibernate e o banco de dados via Flyway.
 */
@DataJpaTest
@Import(CarteiraRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test-repository.properties")
@Transactional
public abstract class RepositoryAbstractTests {
}
