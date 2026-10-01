package com.example.api_docker.infra.persistence.user;

import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;
import com.example.api_docker.domain.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.value());
    }

    @Override
    public void save(User user) {
        userJpaRepository.save(toJpaEntity(user));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return userJpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userJpaRepository.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public PageResult<User> findAll(PaginationRequest pagination) {
        Sort sort = Sort.unsorted();
        if (pagination.sortBy() != null && !pagination.sortBy().isBlank()) {
            Sort.Direction direction = "DESC".equalsIgnoreCase(pagination.sortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(direction, pagination.sortBy());
        }

        Pageable pageable = PageRequest.of(pagination.page(), pagination.size(), sort);
        Page<UserJpaEntity> entityPage = userJpaRepository.findAll(pageable);

        List<User> users = entityPage.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PageResult<>(
                users,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    private UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().value());
        entity.setFirstName(user.getName().firstName());
        entity.setLastName(user.getName().lastName());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        return User.restore(
                new UserId(entity.getId()),
                new FullName(entity.getFirstName(), entity.getLastName()),
                new Email(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }
}
