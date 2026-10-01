package com.example.api_docker.infra.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Essa abstrata vai ser extendida por classes especificas para consumo dos eventos.
 * Isso vai ser separado por tipo de evento e entidade.
 */
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public abstract class KafkaConsumer {
}
