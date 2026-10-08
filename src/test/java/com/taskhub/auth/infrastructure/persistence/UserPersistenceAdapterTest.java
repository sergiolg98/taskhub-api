package com.taskhub.auth.infrastructure.persistence;

import com.taskhub.auth.domain.exception.EmailAlreadyUsedException;
import com.taskhub.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock SpringDataUserRepository repository;
    @InjectMocks UserPersistenceAdapter adapter;

    @Test
    void aUniqueViolationBecomesADomainException() {
        when(repository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThatThrownBy(() -> adapter.save(User.register("Eva", "eva@taskhub.com", "hash")))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessageContaining("eva@taskhub.com");
    }
}
