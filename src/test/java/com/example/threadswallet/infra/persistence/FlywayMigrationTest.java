package com.example.threadswallet.infra.persistence;

import com.example.threadswallet.RepositoryAbstractTests;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest extends RepositoryAbstractTests {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Deve validar que todas as migrations do Flyway foram aplicadas com sucesso")
    void shouldValidateThatAllMigrationsAppliedSuccessfully() {
        MigrationInfo[] migrations = flyway.info().all();

        assertTrue(migrations.length > 0, "Deve haver ao menos uma migration registrada");

        for (MigrationInfo migration : migrations) {
            assertEquals(
                    MigrationState.SUCCESS,
                    migration.getState(),
                    "A migration " + migration.getScript() + " deveria estar em estado SUCCESS"
            );
            assertNotNull(migration.getInstalledOn(), "Data de instalação não pode ser nula");
            assertNotNull(migration.getChecksum(), "Checksum da migration não pode ser nulo");
        }

        MigrationInfo current = flyway.info().current();
        assertNotNull(current);
        assertEquals("1", current.getVersion().getVersion());
        assertEquals("create carteira and ativo tables", current.getDescription());
    }

    @Test
    @DisplayName("Deve validar que as tabelas e colunas foram criadas no banco pela migration")
    void shouldValidateSchemaCreatedByFlyway() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            try (ResultSet tables = metaData.getTables(null, null, "CARTEIRA", null)) {
                assertTrue(tables.next(), "Tabela CARTEIRA deve existir no banco de dados");
            }

            try (ResultSet tables = metaData.getTables(null, null, "ATIVO", null)) {
                assertTrue(tables.next(), "Tabela ATIVO deve existir no banco de dados");
            }

            try (ResultSet tables = metaData.getTables(null, null, "RISCO_CALCULADO", null)) {
                assertTrue(tables.next(), "Tabela RISCO_CALCULADO deve existir no banco de dados");
            }

            List<String> carteiraColumns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "CARTEIRA", null)) {
                while (rs.next()) {
                    carteiraColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            assertTrue(carteiraColumns.contains("id"), "Coluna id deve existir em CARTEIRA");
            assertTrue(carteiraColumns.contains("nome_cliente"), "Coluna nome_cliente deve existir em CARTEIRA");
            assertFalse(carteiraColumns.contains("risco_calculado"), "Coluna risco_calculado NÃO deve existir em CARTEIRA");

            List<String> ativoColumns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "ATIVO", null)) {
                while (rs.next()) {
                    ativoColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            assertTrue(ativoColumns.contains("id"), "Coluna id deve existir em ATIVO");
            assertTrue(ativoColumns.contains("carteira_id"), "Coluna carteira_id deve existir em ATIVO");
            assertTrue(ativoColumns.contains("ticker"), "Coluna ticker deve existir em ATIVO");
            assertTrue(ativoColumns.contains("valor_atual"), "Coluna valor_atual deve existir em ATIVO");
            assertTrue(ativoColumns.contains("taxa_volatilidade"), "Coluna taxa_volatilidade deve existir em ATIVO");

            List<String> riscoColumns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "RISCO_CALCULADO", null)) {
                while (rs.next()) {
                    riscoColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            assertTrue(riscoColumns.contains("id"), "Coluna id deve existir em RISCO_CALCULADO");
            assertTrue(riscoColumns.contains("carteira_id"), "Coluna carteira_id deve existir em RISCO_CALCULADO");
            assertTrue(riscoColumns.contains("valor"), "Coluna valor deve existir em RISCO_CALCULADO");
            assertTrue(riscoColumns.contains("tipo"), "Coluna tipo deve existir em RISCO_CALCULADO");
        }
    }
}
