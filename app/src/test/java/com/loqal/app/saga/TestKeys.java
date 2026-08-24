package com.loqal.app.saga;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/** Throwaway RSA pair for integration tests. */
final class TestKeys {

    record Keys(String privateKeyBase64, String publicKeyBase64) {}

    private TestKeys() {}

    static Keys generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new Keys(
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
