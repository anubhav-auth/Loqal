package com.loqal.identity.auth;

import com.loqal.identity.auth.utils.RSAKeyProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RSAKeyProviderTest {

    private static String b64(byte[] encoded) {
        return Base64.getEncoder().encodeToString(encoded);
    }

    static KeyPair generate() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    void loadsKeysFromConfigAndProducesStableKid() throws Exception {
        KeyPair keyPair = generate();
        RSAKeyProvider provider = new RSAKeyProvider();
        ReflectionTestUtils.setField(provider, "privateKeyBase64", b64(keyPair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(provider, "publicKeyBase64", b64(keyPair.getPublic().getEncoded()));

        provider.init();

        assertNotNull(provider.getRsaJWK());
        assertEquals(((RSAPublicKey) keyPair.getPublic()).getModulus(), provider.getPublicKey().getModulus());

        RSAKeyProvider secondInstance = new RSAKeyProvider();
        ReflectionTestUtils.setField(secondInstance, "privateKeyBase64", b64(keyPair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(secondInstance, "publicKeyBase64", b64(keyPair.getPublic().getEncoded()));
        secondInstance.init();

        assertEquals(provider.getRsaJWK().getKeyID(), secondInstance.getRsaJWK().getKeyID(),
                "kid must be stable across restarts for the same key material");
    }

    @Test
    void failsFastWhenKeysMissing() {
        RSAKeyProvider provider = new RSAKeyProvider();
        assertThrows(IllegalStateException.class, provider::init);
    }
}
