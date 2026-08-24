package com.loqal.identity.auth.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

/**
 * Loads the RSA signing keypair from configuration instead of generating one
 * per boot. Keys are base64-encoded: private key PKCS#8, public key X.509/SPKI.
 * Generation hint:
 *   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
 *     | openssl pkcs8 -topk8 -nocrypt | base64 -w0
 *   openssl rsa -pubout <<< private pem | base64 -w0
 */
@Getter
@Component
public class RSAKeyProvider {

    @Value("${jwt.rsa.private-key}")
    private String privateKeyBase64;

    @Value("${jwt.rsa.public-key}")
    private String publicKeyBase64;

    private RSAKey rsaJWK;

    @PostConstruct
    public void init() throws Exception {
        if (isBlank(privateKeyBase64) || isBlank(publicKeyBase64)) {
            throw new IllegalStateException(
                    "JWT signing keys are not configured. Set JWT_RSA_PRIVATE_KEY and JWT_RSA_PUBLIC_KEY "
                    + "(base64: PKCS#8 private, X.509 SPKI public). Tokens must survive restarts; "
                    + "ephemeral keys are no longer supported.");
        }
        byte[] privateDer = Base64.getDecoder().decode(stripPem(privateKeyBase64));
        byte[] publicDer = Base64.getDecoder().decode(stripPem(publicKeyBase64));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateDer));
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicDer));

        this.rsaJWK = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(stableKeyId(publicKey))
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripPem(String base64) {
        return base64.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    }

    private String stableKeyId(RSAPublicKey publicKey) {
        // Stable across restarts for the same key material; avoids leaking raw key bytes.
        return UUID.nameUUIDFromBytes(publicKey.getModulus().toByteArray()).toString();
    }

    public RSAPrivateKey getPrivateKey() throws JOSEException {
        return rsaJWK.toRSAPrivateKey();
    }

    public RSAPublicKey getPublicKey() throws JOSEException {
        return rsaJWK.toRSAPublicKey();
    }
}
