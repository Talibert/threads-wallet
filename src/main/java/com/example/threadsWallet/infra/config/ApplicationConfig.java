package com.example.api_docker.infra.config;

import com.example.api_docker.domain.shared.DomainEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {


    // ─── noOp para testes sem Kafka ─────────────────────────────
    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher noOpDomainEventPublisher() {
        return event -> {};
    }
}
