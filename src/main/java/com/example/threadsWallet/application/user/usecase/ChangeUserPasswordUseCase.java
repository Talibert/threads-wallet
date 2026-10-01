package com.example.api_docker.application.user.usecase;

import com.example.api_docker.application.user.command.ChangeUserPasswordCommand;
import com.example.api_docker.domain.shared.DomainEventPublisher;
import com.example.api_docker.domain.shared.exception.DomainException;
import com.example.api_docker.domain.user.PasswordEncoder;
import com.example.api_docker.domain.user.User;
import com.example.api_docker.domain.user.UserRepository;
import com.example.api_docker.domain.user.exception.InvalidCredentialsException;
import com.example.api_docker.domain.user.exception.UserNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ChangeUserPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;

    public void execute(ChangeUserPasswordCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if (!passwordEncoder.matches(command.rawCurrentPassword(), user.getPasswordHash()))
            throw new InvalidCredentialsException();

        if (passwordEncoder.matches(command.rawNewPassword(), user.getPasswordHash()))
            throw new DomainException("A nova senha não pode ser igual à senha atual");

        String newPasswordHash = passwordEncoder.encode(command.rawNewPassword());
        user.changePassword(newPasswordHash);

        userRepository.save(user);
        user.pullDomainEvents().forEach(eventPublisher::publish);
    }
}
