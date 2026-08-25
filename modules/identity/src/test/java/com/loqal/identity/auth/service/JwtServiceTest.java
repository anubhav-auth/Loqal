package com.loqal.identity.auth.service;

import com.loqal.identity.auth.utils.RSAKeyProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private UUID testUserId;
    private UUID testTenantId;
    private String testEmail;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        var keyPair = keyGen.generateKeyPair();
        String privateB64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicB64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        RSAKeyProvider keyProvider = new RSAKeyProvider();
        ReflectionTestUtils.setField(keyProvider, "privateKeyBase64", privateB64);
        ReflectionTestUtils.setField(keyProvider, "publicKeyBase64", publicB64);
        keyProvider.init();

        jwtService = new JwtService(keyProvider);

        testUserId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        testEmail = "user@example.com";
    }

    @Test
    void generateAccessToken_containsCorrectEmailAndUserId() throws Exception {
        String token = jwtService.generateAccessToken(
                testEmail, List.of("USER"), testTenantId, testUserId);

        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        assertEquals(testEmail, claims.getSubject());
        assertEquals(testUserId.toString(), claims.getStringClaim("user_id"));
        assertEquals("access", claims.getStringClaim("token_type"));
        assertEquals("auth-service", claims.getIssuer());
        assertNotNull(claims.getExpirationTime());
        assertTrue(claims.getExpirationTime().after(new Date()));
    }

    @Test
    void generateAccessToken_containsTenantAndRoles() throws Exception {
        String token = jwtService.generateAccessToken(
                testEmail, List.of("USER", "ADMIN"), testTenantId, testUserId);

        JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
        assertEquals(testTenantId.toString(), claims.getStringClaim("tenant_id"));
        List<String> roles = claims.getStringListClaim("roles");
        assertEquals(List.of("USER", "ADMIN"), roles);
    }

    @Test
    void generateRefreshToken_hasTokenTypeRefresh() throws Exception {
        String token = jwtService.generateRefreshToken(testEmail, testUserId);

        JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
        assertEquals(testEmail, claims.getSubject());
        assertEquals(testUserId.toString(), claims.getStringClaim("user_id"));
        assertEquals("refresh", claims.getStringClaim("token_type"));
        assertNull(claims.getStringListClaim("roles"));
    }

    @Test
    void extractUsername_returnsEmail() throws Exception {
        String token = jwtService.generateAccessToken(
                testEmail, List.of("USER"), testTenantId, testUserId);

        assertEquals(testEmail, jwtService.extractUsername(token));
    }

    @Test
    void isTokenExpired_returnsFalseForFreshToken() throws Exception {
        String token = jwtService.generateAccessToken(
                testEmail, List.of("USER"), testTenantId, testUserId);

        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_returnsTrueForExpiredToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(testEmail)
                .issuer("auth-service")
                .claim("user_id", testUserId.toString())
                .claim("token_type", "access")
                .issueTime(new Date(System.currentTimeMillis() - 7200_000))
                .expirationTime(new Date(System.currentTimeMillis() - 3600_000))
                .build();

        String token = signToken(claims);
        assertTrue(jwtService.isTokenExpired(token));
    }

    @Test
    void validateToken_returnsTrueForValidToken() throws Exception {
        String token = jwtService.generateAccessToken(
                testEmail, List.of("USER"), testTenantId, testUserId);

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForTamperedToken() throws Exception {
        String token = jwtService.generateAccessToken(
                testEmail, List.of("USER"), testTenantId, testUserId);

        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtService.validateToken(tampered));
    }

    @Test
    void generateServiceToken_hasServiceClaim() throws Exception {
        String token = jwtService.generateServiceToken();

        JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
        assertEquals(true, claims.getClaim("service"));
        assertEquals("auth-internal", claims.getSubject());
        assertEquals("auth-service", claims.getIssuer());
        assertNotNull(claims.getExpirationTime());
    }

    private String signToken(JWTClaimsSet claims) throws JOSEException {
        RSAKeyProvider keyProvider = (RSAKeyProvider) ReflectionTestUtils.getField(jwtService, "keyProvider");
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyProvider.getRsaJWK().getKeyID()).build(),
                claims);
        signedJWT.sign(new RSASSASigner(keyProvider.getPrivateKey()));
        return signedJWT.serialize();
    }
}
