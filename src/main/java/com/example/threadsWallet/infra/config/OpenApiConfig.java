package com.example.threadswallet.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Simulador de Risco de Portfólios (Java 21 Virtual Threads)")
                        .description("Demonstração prática de isolamento de concorrência: I/O com Virtual Threads e CPU-bound com Pool Fixo de Plataforma.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe de Arquitetura")
                                .email("contato@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
