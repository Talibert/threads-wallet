package com.example.api_docker.application.user.usecase;

import com.example.api_docker.application.user.query.ListUsersQuery;
import com.example.api_docker.application.user.result.UserResult;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.user.User;
import com.example.api_docker.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResult<UserResult> execute(ListUsersQuery query) {
        PageResult<User> pagedUsers = userRepository.findAll(query.pagination());
        return pagedUsers.map(UserResult::from);
    }
}
