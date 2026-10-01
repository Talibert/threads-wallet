package com.example.api_docker.application.user.usecase;

import com.example.api_docker.application.user.query.GetUserQuery;
import com.example.api_docker.application.user.result.UserResult;
import com.example.api_docker.domain.user.User;
import com.example.api_docker.domain.user.UserRepository;
import com.example.api_docker.domain.user.exception.UserNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GetUserUseCase {

    private final UserRepository userRepository;

    public UserResult execute(GetUserQuery query) {
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new UserNotFoundException(query.userId()));

        return UserResult.from(user);
    }
}
