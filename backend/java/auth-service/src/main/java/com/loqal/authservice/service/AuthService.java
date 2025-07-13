package com.loqal.authservice.service;

import com.loqal.authservice.entity.Address;
import com.loqal.authservice.entity.RefreshToken;
import com.loqal.authservice.entity.UserCredential;
import com.loqal.authservice.entity.dto.*;
import com.loqal.authservice.repository.RefreshTokenRepository;
import com.loqal.authservice.repository.UserCredentialRepository;
import com.loqal.authservice.utils.JwtUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDetailServiceImpl userDetailServiceImpl;
    private final RestTemplate restTemplate;
    private final HttpSession httpSession;
    private final UserServiceClient userServiceClient;
    public static final UUID NON_TENANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${state-checker.enabled}")
    private Boolean stateCheckerEnabled;

    @Autowired
    AuthService(
            AuthenticationManager authManager,
            JwtUtils jwtUtils,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            UserDetailServiceImpl userDetailServiceImpl,
            RestTemplate restTemplate,
            HttpSession httpSession,
            UserServiceClient userServiceClient
    ) {
        this.authManager = authManager;
        this.jwtUtils = jwtUtils;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailServiceImpl = userDetailServiceImpl;
        this.restTemplate = restTemplate;
        this.httpSession = httpSession;
        this.userServiceClient = userServiceClient;
    }

    public ResponseEntity<?> login(LoginRequest request) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            String internalServiceToken = jwtUtils.generateInternalServiceToken();
            UserOauthRegisterDto dto = createDefaultOauthDto(request.email());

            UserInfoDto userInfo = userServiceClient.registerOrFetchUser(dto, internalServiceToken);
            if (userInfo == null || userInfo.userId() == null || userInfo.roles() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch user info"));
            }

            String accessToken = jwtUtils.generateAccessToken(
                    request.email(),
                    userInfo.roles(),
                    userInfo.tenantId(),
                    userInfo.userId()
            );

            String refreshToken = jwtUtils.generateRefreshToken(request.email());

            Optional<RefreshToken> existingToken = refreshTokenRepository.findByEmail(request.email());
            if (existingToken.isPresent()) {
                RefreshToken token = existingToken.get();
                token.setToken(refreshToken);
                token.setExpiration(Instant.now().plusMillis(jwtUtils.getRefreshTokenExpiration()));
                refreshTokenRepository.save(token);
            } else {
                RefreshToken dbToken = new RefreshToken();
                dbToken.setToken(refreshToken);
                dbToken.setEmail(request.email());
                dbToken.setExpiration(Instant.now().plusMillis(jwtUtils.getRefreshTokenExpiration()));
                refreshTokenRepository.save(dbToken);
            }

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Transactional
    public ResponseEntity<?> register(RegisterRequest request) {
        try {
            if (userCredentialRepository.findByEmail(request.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email already registered"));
            }

            UserCredential user = new UserCredential();
            user.setEmail(request.email());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            userCredentialRepository.save(user);

            UserOauthRegisterDto dto = UserOauthRegisterDto.builder()
                    .email(request.email())
                    .fullName(request.fullName())
                    .phoneNumber(request.phoneNumber())
                    .tenantId(NON_TENANT_UUID)
                    .profilePictureUrl("")
                    .address(Address.defaultAddress())
                    .build();

            userServiceClient.registerOrFetchUser(dto, jwtUtils.generateInternalServiceToken());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email already registered"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
        }
    }

    public ResponseEntity<?> refreshToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid token format"));
        }

        String refreshToken = authHeader.substring(7);
        try {
            Optional<RefreshToken> dbToken = refreshTokenRepository.findByToken(refreshToken);
            if (dbToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
            }
            String username = jwtUtils.extractUsername(refreshToken);
            if (jwtUtils.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh token is expired"));
            }
            String internalServiceToken = jwtUtils.generateInternalServiceToken();

            UserOauthRegisterDto dto = UserOauthRegisterDto.builder()
                    .email(username)
                    .fullName("Unknown User")
                    .address(Address.defaultAddress())
                    .build();

            UserInfoDto userInfo = userServiceClient.registerOrFetchUser(dto, internalServiceToken);

            if (userInfo == null || userInfo.userId() == null || userInfo.roles() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch user info"));
            }

            String newAccessToken = jwtUtils.generateAccessToken(
                    username,
                    userInfo.roles(),
                    userInfo.userId(),
                    userInfo.tenantId()
            );

            String newRefreshToken = jwtUtils.generateRefreshToken(username);

            RefreshToken token = dbToken.get();
            token.setToken(newRefreshToken);
            token.setExpiration(Instant.now().plusMillis(jwtUtils.getRefreshTokenExpiration()));
            refreshTokenRepository.save(token);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
        }
    }

    public ResponseEntity<?> validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid token format"));
        }

        String token = authHeader.substring(7);
        try {
            Optional<RefreshToken> dbToken = refreshTokenRepository.findByToken(token);
            if (dbToken.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh token cannot be used for validation"));
            }

            String username = jwtUtils.extractUsername(token);
            UserDetails userDetails = userDetailServiceImpl.loadUserByUsername(username);
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }

            if (jwtUtils.validateToken(token, userDetails)) {
                return ResponseEntity.ok(Map.of("message", "Token is valid"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }

    public ResponseEntity<?> initiateOAuthLogin(String provider) {
        if (!"google".equalsIgnoreCase(provider)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Unsupported OAuth provider: " + provider));
        }
        try {
            String state = generateRandomState();
            httpSession.setAttribute("oauth_state", state);
            String redirectUrl = generateOAuthRedirectUrl(provider, state);
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to initiate OAuth login: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> handleOAuthCallback(String provider, String code, String state) {
        if (!"google".equalsIgnoreCase(provider)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Unsupported OAuth provider: " + provider));
        }
        try {
            String storedState = (String) httpSession.getAttribute("oauth_state");
            boolean enforceStateCheck = stateCheckerEnabled;

            if (enforceStateCheck && (state == null || !state.equals(storedState))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid or missing state parameter"));
            }
            httpSession.removeAttribute("oauth_state");

            Map<String, Object> userInfo = exchangeCodeForUserInfo(provider, code);
            String email = (String) userInfo.get("email");
            String fullName = (String) userInfo.getOrDefault("name", "Unknown User");
            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email not provided by OAuth provider"));
            }


            String extractPhoneNumber = extractPhoneNumber(userInfo);
            String phoneNumber = (extractPhoneNumber== null)? " " : extractPhoneNumber;
            return getResponseEntity(email, fullName, phoneNumber);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "OAuth authentication failed: " + e.getMessage()));
        }
    }

    private String generateOAuthRedirectUrl(String provider, String state) {
        if (!"google".equalsIgnoreCase(provider)) {
            throw new UnsupportedOperationException("OAuth provider not implemented: " + provider);
        }
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + googleClientId +
                "&redirect_uri=" + googleRedirectUri +
                "&response_type=code" +
                "&scope=openid%20email%20profile%20https://www.googleapis.com/auth/user.phonenumbers.read" +
                "&state=" + state;
    }

    private Map<String, Object> exchangeCodeForUserInfo(String provider, String code) {
        if (!"google".equalsIgnoreCase(provider)) {
            throw new UnsupportedOperationException("OAuth provider not implemented: " + provider);
        }

        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to exchange code for token");
        }

        assert tokenResponse.getBody() != null;
        String idToken = (String) tokenResponse.getBody().get("id_token");
        String accessToken = (String) tokenResponse.getBody().get("access_token");
        if (idToken == null) {
            throw new RuntimeException("ID token not provided by Google");
        }

        // Fetch user info from tokeninfo endpoint
        String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(userInfoUrl, Map.class);
        if (userInfoResponse.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to fetch user info");
        }

        Map userInfo = userInfoResponse.getBody();
        assert userInfo != null;
        String email = (String) userInfo.get("email");
        if (email == null) {
            throw new RuntimeException("Email not provided by Google");
        }

        // Fetch phone number from People API
        String peopleApiUrl = "https://people.googleapis.com/v1/people/me?personFields=phoneNumbers";
        HttpHeaders peopleHeaders = new HttpHeaders();
        peopleHeaders.setBearerAuth(accessToken);
        HttpEntity<?> peopleEntity = new HttpEntity<>(peopleHeaders);
        ResponseEntity<Map> peopleResponse = restTemplate.exchange(peopleApiUrl, HttpMethod.GET, peopleEntity, Map.class);
        if (peopleResponse.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to fetch phone number");
        }

        userInfo.put("phoneNumbers", peopleResponse.getBody().get("phoneNumbers"));
        return userInfo;
    }

    private String generateRandomState() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public ResponseEntity<?> handleMobileOAuth(Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing ID token"));
        }

        try {
            // Validate ID token and get user info
            String userInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(userInfoUrl, Map.class);
            if (userInfoResponse.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Invalid ID token");
            }

            Map<String, Object> userInfo = userInfoResponse.getBody();
            String email = (String) userInfo.get("email");
            String fullName = (String) userInfo.getOrDefault("name", "Unknown User");
            if (email == null) {
                throw new RuntimeException("Email not found in token");
            }

            // Fetch access token by exchanging ID token
            String tokenEndpoint = "https://oauth2.googleapis.com/token";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("id_token", idToken);
            params.add("client_id", googleClientId);
            params.add("client_secret", googleClientSecret);
            params.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, tokenRequest, Map.class);
            if (tokenResponse.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to exchange ID token for access token");
            }

            String accessToken = (String) tokenResponse.getBody().get("access_token");
            if (accessToken == null) {
                throw new RuntimeException("Access token not provided by Google");
            }

            // Fetch phone number from People API
            String peopleApiUrl = "https://people.googleapis.com/v1/people/me?personFields=phoneNumbers";
            HttpHeaders peopleHeaders = new HttpHeaders();
            peopleHeaders.setBearerAuth(accessToken);
            HttpEntity<?> peopleEntity = new HttpEntity<>(peopleHeaders);
            ResponseEntity<Map> peopleResponse = restTemplate.exchange(peopleApiUrl, HttpMethod.GET, peopleEntity, Map.class);
            if (peopleResponse.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to fetch phone number");
            }

            String extractPhoneNumber = extractPhoneNumber(peopleResponse.getBody());
            String phoneNumber = (extractPhoneNumber == null)? " " : extractPhoneNumber;
            return getResponseEntity(email, fullName, phoneNumber);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "OAuth authentication failed: " + e.getMessage()));
        }
    }

    private String extractPhoneNumber(Map<String, Object> peopleResponse) {
        if (peopleResponse == null || !peopleResponse.containsKey("phoneNumbers")) {
            return null;
        }
        List<Map<String, Object>> phoneNumbers = (List<Map<String, Object>>) peopleResponse.get("phoneNumbers");
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return null;
        }
        // Return the first phone number's value
        Map<String, Object> phoneData = phoneNumbers.get(0);
        return (String) phoneData.get("value");
    }

    private ResponseEntity<?> getResponseEntity(String email, String fullName, String phoneNumber) {
        if (userCredentialRepository.findByEmail(email).isEmpty()) {
            UserCredential user = new UserCredential();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            userCredentialRepository.save(user);
        }

        UserOauthRegisterDto dto = UserOauthRegisterDto.builder()
                .email(email)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .address(Address.defaultAddress())
                .tenantId(NON_TENANT_UUID)
                .build();

        String serviceJwt = jwtUtils.generateInternalServiceToken();
        UserInfoDto userInfo = userServiceClient.registerOrFetchUser(dto, serviceJwt);
        if (userInfo == null || userInfo.userId() == null || userInfo.roles() == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch user info"));
        }

        String accessToken = jwtUtils.generateAccessToken(
                email,
                userInfo.roles(),
                userInfo.tenantId(),
                userInfo.userId()
        );
        String refreshToken = jwtUtils.generateRefreshToken(email);

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByEmail(email);
        RefreshToken token = existingToken.orElseGet(() -> new RefreshToken());
        token.setEmail(email);
        token.setToken(refreshToken);
        token.setExpiration(Instant.now().plusMillis(jwtUtils.getRefreshTokenExpiration()));
        refreshTokenRepository.save(token);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    private UserOauthRegisterDto createDefaultOauthDto(String email) {
        return UserOauthRegisterDto.builder()
                .email(email)
                .build();
    }
}