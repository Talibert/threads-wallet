package com.example.api_docker.infra.kafka.user;

import com.example.api_docker.domain.user.event.UserCreatedEvent;
import com.example.api_docker.domain.user.event.UserPasswordChangedEvent;
import com.example.api_docker.infra.kafka.KafkaConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class UserEventsConsumer extends KafkaConsumer {

    @KafkaListener(topics = "user.created", groupId = "${spring.kafka.consumer.group-id:my-group}")
    public void onUserCreated(UserCreatedEvent event) {
        log.info("📢 [KAFKA CONSUMER] Evento recebido: Usuário criado! ID: {}, Email: {}, Tipo: {}, Ocorrido em: {}",
                event.userId().value(), event.email().value(), event.eventType(), event.occurredAt());
    }

    @KafkaListener(topics = "admin.password.changed", groupId = "${spring.kafka.consumer.group-id:my-group}")
    public void onUserPasswordChanged(UserPasswordChangedEvent event) {
        log.info("📢 [KAFKA CONSUMER] Evento recebido: Senha do usuário alterada! ID: {}, Tipo: {}, Ocorrido em: {}",
                event.userId().value(), event.eventType(), event.occurredAt());
    }
}
