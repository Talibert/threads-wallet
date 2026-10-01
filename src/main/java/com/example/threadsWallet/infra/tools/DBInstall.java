package com.example.api_docker.infra.tools;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.persistence.Entity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilitário para gerar e imprimir no console o script DDL SQL (PostgreSQL)
 * de todas as entidades JPA mapeadas no projeto.
 */
public class DBInstall {

    static {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("org.hibernate").setLevel(Level.ERROR);
        } catch (Exception ignored) {
        }
    }

    /**
     * Gera todos os comandos DDL formatados com ponto-e-vírgula para as entidades JPA do projeto.
     * Inclui CREATE TABLE, PRIMARY KEYS, FOREIGN KEYS, UNIQUE CONSTRAINTS, INDEXES e SEQUENCES.
     */
    public static List<String> generateFormattedDDL() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        settings.put("hibernate.boot.allow_jdbc_metadata_access", "false");

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        try {
            MetadataSources metadataSources = new MetadataSources(serviceRegistry);

            // Escaneia automaticamente todas as classes anotadas com @Entity
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);

            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

            Set<BeanDefinition> entityDefs = scanner.findCandidateComponents("com.example.api_docker");

            for (BeanDefinition def : entityDefs) {
                Class<?> entityClass = Class.forName(def.getBeanClassName());
                metadataSources.addAnnotatedClass(entityClass);
            }

            Metadata metadata = metadataSources.getMetadataBuilder().build();
            SchemaCreatorImpl schemaCreator = new SchemaCreatorImpl(serviceRegistry);

            List<String> rawCommands = schemaCreator.generateCreationCommands(metadata, false);
            Formatter formatter = FormatStyle.DDL.getFormatter();

            List<String> formattedCommands = new ArrayList<>();
            for (String command : rawCommands) {
                formattedCommands.add(formatter.format(command).trim() + ";");
            }

            return formattedCommands;
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" 🚀 DBInstall - Gerador de DDL SQL para Migrations (Dialeto: PostgreSQL)");
        System.out.println("================================================================================\n");

        try {
            // Escaneia e lista as entidades encontradas
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            Set<BeanDefinition> entityDefs = scanner.findCandidateComponents("com.example.api_docker");

            if (entityDefs.isEmpty()) {
                System.out.println("⚠️ Nenhuma entidade com @Entity encontrada no pacote com.example.api_docker.");
                return;
            }

            System.out.println("🔍 Entidades encontradas (" + entityDefs.size() + "):");
            for (BeanDefinition def : entityDefs) {
                System.out.println("   - " + def.getBeanClassName());
            }

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("--- INÍCIO DO DDL GERADO (Copie para sua migration Flyway) ---");
            System.out.println("--------------------------------------------------------------------------------\n");

            List<String> statements = generateFormattedDDL();
            for (String statement : statements) {
                System.out.println(statement + "\n");
            }

            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("--- FIM DO DDL GERADO ---");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("\n💡 Dica: Crie o arquivo em src/main/resources/db/migration/V<versao>__<descricao>.sql");
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("❌ Erro ao gerar DDL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
