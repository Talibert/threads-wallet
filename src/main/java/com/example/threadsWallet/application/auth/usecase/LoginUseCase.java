package com.example.api_docker.application.auth.usecase;

import com.example.api_docker.application.auth.command.LoginCommand;
import com.example.api_docker.application.auth.result.LoginResult;
import com.example.api_docker.domain.user.*;
import com.example.api_docker.domain.user.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;

    public LoginResult execute(LoginCommand command) {
        Email email = new Email(command.email());

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent())
            return authenticate(user.get(), command.rawPassword(), UserRole.ADMIN);

        throw new InvalidCredentialsException();
    }

    private LoginResult authenticate(User user, String rawPassword, UserRole role) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash()))
            throw new InvalidCredentialsException();

        String token = tokenGenerator.generate(user.getId(), user.getEmail(), role);
        return new LoginResult(token, user.getId().value(), user.getName().full(), role.name());
    }
}
