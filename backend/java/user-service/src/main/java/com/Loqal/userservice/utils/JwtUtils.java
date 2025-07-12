package com.Loqal.userservice.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractIssuer(String token) {
        return extractAllClaims(token).getIssuer();
    }

    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractAllClaims(token).get("user_id", String.class);
        return UUID.fromString(userIdStr);
    }

    public boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    public boolean validateIssuer(String token, String expectedIssuer) {
        return expectedIssuer.equals(extractIssuer(token)) && !isTokenExpired(token);
    }
}
