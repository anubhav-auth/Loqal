package com.loqal.identity.auth.service;

import com.loqal.identity.auth.entity.RefreshToken;
import com.loqal.identity.auth.entity.UserCredential;
import com.loqal.identity.auth.entity.dto.AuthResponse;
import com.loqal.identity.auth.entity.dto.LoginRequest;
import com.loqal.identity.auth.entity.dto.RegisterRequest;
import com.loqal.identity.auth.repository.RefreshTokenRepository;
import com.loqal.identity.auth.repository.UserCredentialRepository;
import com.loqal.identity.users.api.UsersApi;
import com.loqal.identity.users.entity.dto.UserInfoDto;
import com.loqal.identity.users.entity.dto.UserRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private ReactiveAuthenticationManager reactiveAuthManager;
    @Mock private JwtService jwtService;
    @Mock private UserCredentialRepository userCredentialRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UsersApi usersApi;

    private AuthService authService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_TENANT_ID = UUID.randomUUID();
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                reactiveAuthManager, jwtService, userCredentialRepository,
                passwordEncoder, refreshTokenRepository, usersApi);
        ReflectionTestUtils.setField(authService, "googleClientId", "client-id");
        ReflectionTestUtils.setField(authService, "googleClientSecret", "client-secret");
        ReflectionTestUtils.setField(authService, "googleRedirectUri", "http://redirect");
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 86400000L);
    }

    @Test
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        Authentication auth = mock(Authentication.class);
        when(reactiveAuthManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.just(auth));
        mockGenerateTokensForUser();

        StepVerifier.create(authService.login(request))
                .assertNext(response -> {
                    assertEquals(ACCESS_TOKEN, response.accessToken());
                    assertEquals(REFRESH_TOKEN, response.refreshToken());
                })
                .verifyComplete();
    }

    @Test
    void login_failure() {
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrong");
        when(reactiveAuthManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.error(new org.springframework.security.authentication
                        .BadCredentialsException("Bad credentials")));

        StepVerifier.create(authService.login(request))
                .verifyError(ResponseStatusException.class);
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, "Test User", "12345");
        when(userCredentialRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.empty());
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("hashed");
        when(userCredentialRepository.insertNewUser(any(UUID.class), eq(TEST_EMAIL), eq("hashed"), any(LocalDateTime.class)))
                .thenReturn(Mono.just(new UserCredential()));
        when(usersApi.registerOrUpdateFromOAuth(any()))
                .thenReturn(Mono.just(UserInfoDto.builder().userId(TEST_USER_ID).build()));

        StepVerifier.create(authService.register(request))
                .verifyComplete();

        verify(userCredentialRepository).insertNewUser(any(UUID.class), eq(TEST_EMAIL), eq("hashed"), any(LocalDateTime.class));
        verify(usersApi).registerOrUpdateFromOAuth(argThat(dto ->
                TEST_EMAIL.equals(dto.getEmail()) && "Test User".equals(dto.getFullName())));
    }

    @Test
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD, "Test User", "12345");
        when(userCredentialRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Mono.just(new UserCredential()));

        StepVerifier.create(authService.register(request))
                .verifyError(ResponseStatusException.class);
    }

    @Test
    void refreshToken_success() throws Exception {
        RefreshToken dbToken = new RefreshToken(UUID.randomUUID(), REFRESH_TOKEN, TEST_EMAIL, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN)).thenReturn(Mono.just(dbToken));
        when(jwtService.isTokenExpired(REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        mockGenerateTokensForUser();

        StepVerifier.create(authService.refreshToken(REFRESH_TOKEN))
                .assertNext(response -> {
                    assertEquals(ACCESS_TOKEN, response.accessToken());
                    assertEquals(REFRESH_TOKEN, response.refreshToken());
                })
                .verifyComplete();
    }

    @Test
    void refreshToken_expired() throws Exception {
        RefreshToken dbToken = new RefreshToken(UUID.randomUUID(), REFRESH_TOKEN, TEST_EMAIL, Instant.now());
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN)).thenReturn(Mono.just(dbToken));
        when(jwtService.isTokenExpired(REFRESH_TOKEN)).thenReturn(true);

        StepVerifier.create(authService.refreshToken(REFRESH_TOKEN))
                .verifyError(ResponseStatusException.class);
    }

    @Test
    void refreshToken_notFound() {
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(authService.refreshToken("nonexistent"))
                .verifyError(ResponseStatusException.class);
    }

    @Test
    void refreshToken_invalidTokenDetails() throws Exception {
        RefreshToken dbToken = new RefreshToken(UUID.randomUUID(), REFRESH_TOKEN, TEST_EMAIL, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(REFRESH_TOKEN)).thenReturn(Mono.just(dbToken));
        when(jwtService.isTokenExpired(REFRESH_TOKEN)).thenThrow(new java.text.ParseException("error", 0));

        StepVerifier.create(authService.refreshToken(REFRESH_TOKEN))
                .verifyError(ResponseStatusException.class);
    }

    private void mockGenerateTokensForUser() throws Exception {
        UserInfoDto userInfo = UserInfoDto.builder()
                .userId(TEST_USER_ID)
                .roles(List.of(UserRoles.USER))
                .tenantId(TEST_TENANT_ID)
                .build();
        when(usersApi.registerOrUpdateFromOAuth(any())).thenReturn(Mono.just(userInfo));
        when(jwtService.generateAccessToken(eq(TEST_EMAIL), eq(List.of("USER")), eq(TEST_TENANT_ID), eq(TEST_USER_ID)))
                .thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(eq(TEST_EMAIL), eq(TEST_USER_ID))).thenReturn(REFRESH_TOKEN);
        when(refreshTokenRepository.findByEmail(TEST_EMAIL)).thenReturn(Mono.empty());
        when(refreshTokenRepository.insertNewToken(any(UUID.class), eq(REFRESH_TOKEN), eq(TEST_EMAIL), any(Instant.class)))
                .thenReturn(Mono.just(new RefreshToken()));
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
