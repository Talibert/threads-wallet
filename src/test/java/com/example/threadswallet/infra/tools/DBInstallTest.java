package com.example.threadswallet.infra.tools;

import com.example.threadswallet.UnitAbstractTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DBInstallTest extends UnitAbstractTests {

    @Test
    @DisplayName("Deve gerar comandos DDL contendo CREATE TABLE para as entidades mapeadas")
    void shouldGenerateFormattedDDLSuccessfully() throws Exception {
        List<String> ddlStatements = DBInstall.generateFormattedDDL();

        assertThat(ddlStatements).isNotEmpty();

        String fullDDL = String.join("\n", ddlStatements);
        assertThat(fullDDL)
                .containsIgnoringCase("create table carteira")
                .containsIgnoringCase("nome_cliente")
                .containsIgnoringCase("risco_calculado")
                .containsIgnoringCase("create table ativo")
                .containsIgnoringCase("carteira_id")
                .containsIgnoringCase("ticker")
                .containsIgnoringCase("valor_atual")
                .containsIgnoringCase("taxa_volatilidade");
    }

    @Test
    @DisplayName("Deve executar o método main() sem lançar exceções")
    void shouldExecuteMainWithoutThrowing() {
        assertDoesNotThrow(() -> DBInstall.main(new String[0]));
    }
}
