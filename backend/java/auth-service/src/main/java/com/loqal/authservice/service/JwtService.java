package com.loqal.authservice.service;

import com.loqal.authservice.utils.RSAKeyProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RSAKeyProvider keyProvider;

    public String generateAccessToken(String email, List<String> roles, UUID tenantId, UUID userId) throws JOSEException {
        return generateToken(email, roles, tenantId, userId, "access", 3600_000); // 1 hour
    }

    public String generateRefreshToken(String email, UUID userId) throws JOSEException {
        return generateToken(email, null, null, userId, "refresh", 7 * 24 * 3600_000); // 7 days
    }

    public String generateServiceToken() throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("auth-internal")
                .issuer("auth-service")
                .claim("service", true)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 300_000)) // 5 min
                .build();
        return signToken(claims);
    }

    private String generateToken(String email, List<String> roles, UUID tenantId, UUID userId, String type, long ttlMillis) throws JOSEException {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(email)
                .issuer("auth-service")
                .claim("user_id", userId)
                .claim("token_type", type)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + ttlMillis));

        if (tenantId != null) builder.claim("tenant_id", tenantId);
        if (roles != null && !roles.isEmpty()) builder.claim("roles", roles);

        return signToken(builder.build());
    }

    private String signToken(JWTClaimsSet claims) throws JOSEException {
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyProvider.getRsaJWK().getKeyID()).build(),
                claims
        );
        signedJWT.sign(new RSASSASigner(keyProvider.getPrivateKey()));
        return signedJWT.serialize();
    }

    public String extractUsername(String token) throws ParseException {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) throws ParseException {
        Date expiration = extractAllClaims(token).getExpirationTime();
        return expiration != null && expiration.before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSASSAVerifier verifier = new RSASSAVerifier(keyProvider.getPublicKey());
            return signedJWT.verify(verifier) && !isTokenExpired(token);
        } catch (ParseException | JOSEException e) {
            return false;
        }
    }

    private JWTClaimsSet extractAllClaims(String token) throws ParseException {
        return SignedJWT.parse(token).getJWTClaimsSet();
    }
}