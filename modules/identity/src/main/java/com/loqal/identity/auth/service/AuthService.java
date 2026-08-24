package com.loqal.identity.auth.service;

import com.loqal.identity.auth.entity.RefreshToken;
import com.loqal.identity.auth.entity.UserCredential;
import com.loqal.identity.auth.entity.dto.AuthResponse;
import com.loqal.identity.auth.entity.dto.LoginRequest;
import com.loqal.identity.auth.entity.dto.RegisterRequest;
import com.loqal.identity.users.api.UsersApi;
import com.loqal.identity.users.entity.Address;
import com.loqal.identity.users.entity.dto.UserOauthRegisterDto;
import com.loqal.identity.auth.repository.RefreshTokenRepository;
import com.loqal.identity.auth.repository.UserCredentialRepository;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final UUID NON_TENANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final ReactiveAuthenticationManager reactiveAuthManager;
    private final JwtService jwtService;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsersApi usersApi;
    private final WebClient webClient = WebClient.create();
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public Mono<AuthResponse> login(LoginRequest request) {
        return reactiveAuthManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()))
                .onErrorMap(AuthenticationException.class, ex -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"))
                .flatMap(authentication -> generateTokensForUser(request.email()));
    }

    @Transactional
    public Mono<Void> register(RegisterRequest request) {
        return userCredentialRepository.findByEmail(request.email())
                .flatMap(existingUser -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered")))
                .switchIfEmpty(Mono.defer(() -> {
                    UUID newUserId = UUID.randomUUID();
                    String hashedPassword = passwordEncoder.encode(request.password());
                    LocalDateTime creationTime = LocalDateTime.now();
                    return userCredentialRepository.insertNewUser(newUserId, request.email(), hashedPassword, creationTime);
                }))
                .flatMap(savedUser -> {
                    UserOauthRegisterDto dto = UserOauthRegisterDto.builder()
                            .email(request.email())
                            .fullName(request.fullName())
                            .phoneNumber(request.phoneNumber())
                            .tenantId(NON_TENANT_UUID)
                            .address(Address.defaultAddress())
                            .build();
                    return usersApi.registerOrUpdateFromOAuth(dto);
                })
                .then();
    }

    public Mono<AuthResponse> refreshToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")))
                .flatMap(dbToken -> {
                    try {
                        if (jwtService.isTokenExpired(refreshToken)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is expired"));
                        }
                        String email = jwtService.extractUsername(refreshToken);
                        return generateTokensForUser(email);
                    } catch (Exception e) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token details"));
                    }
                });
    }

    @Transactional
    public Mono<AuthResponse> handleOAuthCallback(String code) {
        return exchangeCodeForGoogleTokens(code)
                .flatMap(tokens -> {
                    String idToken = (String) tokens.get("id_token");
                    String accessToken = (String) tokens.get("access_token");
                    return fetchGoogleUserInfo(idToken, accessToken);
                })
                .flatMap(userInfo -> {
                    String email = (String) userInfo.get("email");
                    String fullName = (String) userInfo.getOrDefault("name", "Unknown User");
                    String phoneNumber = extractPhoneNumber(userInfo);
                    return registerOrFindOAuthUser(email, fullName, phoneNumber)
                            .then(generateTokensForUser(email));
                });
    }

    @Transactional
    public Mono<AuthResponse> handleMobileOAuth(Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing ID token"));
        }

        return validateGoogleIdToken(idToken)
                .flatMap(userInfo -> {
                    String email = (String) userInfo.get("email");
                    String fullName = (String) userInfo.getOrDefault("name", "Unknown User");
                    if (email == null) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email not found in token"));
                    }
                    // Phone number is not available from ID token validation alone.
                    return registerOrFindOAuthUser(email, fullName, null)
                            .then(generateTokensForUser(email));
                });
    }

    private Mono<AuthResponse> generateTokensForUser(String email) {
        UserOauthRegisterDto dto = UserOauthRegisterDto.builder().email(email).build();
        return usersApi.registerOrUpdateFromOAuth(dto)
                .flatMap(userInfo -> {
                    if (userInfo == null || userInfo.getUserId() == null || userInfo.getRoles() == null) {
                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch user info"));
                    }
                    try {
                        String accessToken = jwtService.generateAccessToken(email,
                                userInfo.getRoles().stream().map(Enum::name).toList(),
                                userInfo.getTenantId(), userInfo.getUserId());
                        String newRefreshToken = jwtService.generateRefreshToken(email, userInfo.getUserId());
                        return saveRefreshToken(email, newRefreshToken)
                                .map(savedToken -> new AuthResponse(accessToken, newRefreshToken));
                    } catch (JOSEException e) {
                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate tokens"));
                    }
                });
    }

    private Mono<RefreshToken> saveRefreshToken(String email, String tokenValue) {
        return refreshTokenRepository.findByEmail(email)
                .flatMap(existingToken -> refreshTokenRepository.updateTokenByEmail(
                        email,
                        tokenValue,
                        Instant.now().plusMillis(refreshTokenExpiration)
                ))
                .switchIfEmpty(Mono.defer(() -> refreshTokenRepository.insertNewToken(
                        UUID.randomUUID(),
                        tokenValue,
                        email,
                        Instant.now().plusMillis(refreshTokenExpiration)
                )));
    }

    private Mono<UserCredential> registerOrFindOAuthUser(String email, String fullName, String phoneNumber) {
        return userCredentialRepository.findByEmail(email)
                .switchIfEmpty(Mono.defer(() -> userCredentialRepository.insertNewUser(
                        UUID.randomUUID(),
                        email,
                        passwordEncoder.encode(UUID.randomUUID().toString()),
                        LocalDateTime.now()
                )));
    }

    private Mono<Map<String, Object>> exchangeCodeForGoogleTokens(String code) {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        return webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                }) // FIX for generics
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to exchange code for token", e));
    }

    private Mono<Map<String, Object>> fetchGoogleUserInfo(String idToken, String accessToken) {
        String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        return webClient.get().uri(userInfoUrl).retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMap(userInfo -> {
                    String peopleApiUrl = "https://people.googleapis.com/v1/people/me?personFields=phoneNumbers";
                    return webClient.get()
                            .uri(peopleApiUrl)
                            .headers(h -> h.setBearerAuth(accessToken))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                            })
                            .map(peopleInfo -> {
                                userInfo.put("phoneNumbers", peopleInfo.get("phoneNumbers"));
                                return userInfo;
                            });
                })
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to fetch user info from Google", e));
    }

    private Mono<Map<String, Object>> validateGoogleIdToken(String idToken) {
        String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        return webClient.get()
                .uri(userInfoUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID Token", e));
    }

    private String extractPhoneNumber(Map<String, Object> peopleResponse) {
        if (peopleResponse == null || !peopleResponse.containsKey("phoneNumbers")) return null;
        List<Map<String, Object>> phoneNumbers = (List<Map<String, Object>>) peopleResponse.get("phoneNumbers");
        if (phoneNumbers == null || phoneNumbers.isEmpty()) return null;
        return (String) phoneNumbers.get(0).get("value");
    }
}