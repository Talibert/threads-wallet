package com.example.api_docker.application.user.usecase;

import com.example.api_docker.application.user.command.CreateUserCommand;
import com.example.api_docker.domain.shared.DomainEventPublisher;
import com.example.api_docker.domain.user.*;
import com.example.api_docker.domain.user.exception.EmailAlreadyInUseException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;

    public void execute(CreateUserCommand command) {
        if (userRepository.existsByEmail(new Email(command.email())))
            throw new EmailAlreadyInUseException(command.email());

        User user = User.create(
                new FullName(command.firstName(), command.lastName()),
                new Email(command.email()),
                passwordEncoder.encode(command.rawPassword())
        );

        userRepository.save(user);
        user.pullDomainEvents().forEach(eventPublisher::publish);
    }
}
