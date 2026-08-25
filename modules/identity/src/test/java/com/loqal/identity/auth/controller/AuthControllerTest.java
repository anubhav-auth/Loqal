package com.loqal.identity.auth.controller;

import com.loqal.identity.auth.entity.dto.AuthResponse;
import com.loqal.identity.auth.entity.dto.LoginRequest;
import com.loqal.identity.auth.entity.dto.RegisterRequest;
import com.loqal.identity.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    void login_success_returns200WithTokens() {
        AuthResponse response = new AuthResponse("access-token", "refresh-token");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(controller.login(new LoginRequest("user@test.com", "pass")))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody()).isEqualTo(response);
                })
                .verifyComplete();
    }

    @Test
    void login_failure_propagatesError() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("bad credentials")));

        StepVerifier.create(controller.login(new LoginRequest("user@test.com", "wrong")))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void register_success_returns201WithMessage() {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.register(
                        new RegisterRequest("new@test.com", "pass", "Test User", "1234567890")))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().value()).isEqualTo(201);
                    assertThat(r.getBody()).containsEntry("message", "User registered successfully");
                })
                .verifyComplete();
    }

    @Test
    void refreshToken_success_returns200WithNewTokens() {
        AuthResponse response = new AuthResponse("new-access", "new-refresh");
        when(authService.refreshToken("valid-token"))
                .thenReturn(Mono.just(response));

        StepVerifier.create(controller.refreshToken("Bearer valid-token"))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody()).isEqualTo(response);
                })
                .verifyComplete();
    }

    @Test
    void handleGoogleCallback_success_returns200WithTokens() {
        AuthResponse response = new AuthResponse("google-access", "google-refresh");
        when(authService.handleOAuthCallback("auth-code-123"))
                .thenReturn(Mono.just(response));

        StepVerifier.create(controller.handleGoogleCallback("auth-code-123"))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody()).isEqualTo(response);
                })
                .verifyComplete();
    }
}
