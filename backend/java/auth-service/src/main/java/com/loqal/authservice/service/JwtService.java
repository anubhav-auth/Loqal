package com.loqal.authservice.service;

import com.loqal.authservice.utils.RSAKeyProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final RSAKeyProvider keyProvider;

    public JwtService(RSAKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public String generateAccessToken(String email, List<String> roles, UUID tenantId, UUID userId) throws JOSEException {
        return generateToken(email, roles, tenantId, userId, "access", 3600_000); // 1 hour
    }

    public String generateRefreshToken(String email, UUID userId) throws JOSEException {
        return generateToken(email, null, null, userId, "refresh", 7 * 24 * 3600_000); // 7 days
    }

    public String generateServiceToken() throws JOSEException {
        JWSSigner signer = new RSASSASigner(keyProvider.getPrivateKey());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("auth-internal")
                .issuer("auth-service")
                .claim("service", true)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 300_000)) // 5 min
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyProvider.getRsaJWK().getKeyID()).build(),
                claims
        );

        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private String generateToken(String email, List<String> roles, UUID tenantId, UUID userId, String type, long ttlMillis) throws JOSEException {
        JWSSigner signer = new RSASSASigner(keyProvider.getPrivateKey());

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(email)
                .claim("user_id", userId)
                .claim("token_type", type)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + ttlMillis));

        if (tenantId != null) {
            builder.claim("tenant_id", tenantId);
        }
        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyProvider.getRsaJWK().getKeyID()).build(),
                builder.build()
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}

