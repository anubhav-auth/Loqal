package com.loqal.authservice.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Getter
@Component
public class RSAKeyProvider {

    private RSAKey rsaJWK;

    @PostConstruct
    public void init() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        rsaJWK = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    public RSAPrivateKey getPrivateKey() throws JOSEException {
        return rsaJWK.toRSAPrivateKey();
    }

    public RSAPublicKey getPublicKey() throws JOSEException {
        return rsaJWK.toRSAPublicKey();
    }
}