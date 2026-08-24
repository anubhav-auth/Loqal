package com.loqal.identity.users;

import com.loqal.identity.users.entity.User;
import com.loqal.identity.users.repository.UserRepository;
import com.loqal.identity.users.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void findAuthSnapshotByEmailMapsRolesAndTenant() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("priya@example.com");
        user.setRoles(List.of(com.loqal.identity.users.entity.dto.UserRoles.USER));
        user.setTenantId(UUID.randomUUID());
        when(userRepository.findByEmail(anyString())).thenReturn(Mono.just(user));

        StepVerifier.create(userService.findAuthSnapshotByEmail("priya@example.com"))
                .assertNext(snapshot -> {
                    org.junit.jupiter.api.Assertions.assertEquals(user.getId(), snapshot.getUserId());
                    org.junit.jupiter.api.Assertions.assertFalse(snapshot.getRoles().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void findAuthSnapshotByEmailIsEmptyForUnknownEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(userService.findAuthSnapshotByEmail("nobody@example.com"))
                .verifyComplete();
    }
}
