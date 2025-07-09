package com.loqal.authservice.service;

import com.loqal.authservice.entity.RefreshToken;
import com.loqal.authservice.entity.UserCredential;
import com.loqal.authservice.entity.dto.AuthResponse;
import com.loqal.authservice.entity.dto.LoginRequest;
import com.loqal.authservice.entity.dto.RegisterRequest;
import com.loqal.authservice.repository.RefreshTokenRepository;
import com.loqal.authservice.repository.UserCredentialRepository;
import com.loqal.authservice.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDetailServiceImpl userDetailServiceImpl;


    @Autowired
    AuthService(
            AuthenticationManager authManager,
            JwtUtils jwtUtils,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            UserDetailServiceImpl userDetailServiceImpl
    ) {
        this.authManager = authManager;
        this.jwtUtils = jwtUtils;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailServiceImpl = userDetailServiceImpl;
    }

    public ResponseEntity<?> login(LoginRequest request) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            String accessToken = jwtUtils.generateAccessToken(request.email());
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
        }
    }

    public ResponseEntity<?> register(RegisterRequest request) {
        try {
            if (userCredentialRepository.findByEmail(request.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email already registered"));
            }

            UserCredential user = new UserCredential();
            user.setEmail(request.email());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            userCredentialRepository.save(user);

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
            String newAccessToken = jwtUtils.generateAccessToken(username);
            String newRefreshToken = jwtUtils.generateRefreshToken(username);

            // Update refresh token in database
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
            // Check if the token is a refresh token
            Optional<RefreshToken> dbToken = refreshTokenRepository.findByToken(token);
            if (dbToken.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh token cannot be used for validation"));
            }

            String username = jwtUtils.extractUsername(token);
            UserDetails userDetails = userDetailServiceImpl.loadUserByUsername(username);

            // Optionally, verify token expiration matches access token duration
            long expirationMillis = jwtUtils.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (expirationMillis > jwtUtils.getRefreshTokenExpiration()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Token does not match access token characteristics"));
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
}