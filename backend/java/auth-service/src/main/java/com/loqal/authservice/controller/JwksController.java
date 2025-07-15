package com.loqal.authservice.controller;

import com.loqal.authservice.utils.RSAKeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/.well-known")
public class JwksController {

    private final RSAKeyProvider keyProvider;

    public JwksController(RSAKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/jwks.json")
    public Map<String, Object> getJwks() {
        return new JWKSet(keyProvider.getRsaJWK().toPublicJWK()).toJSONObject();
    }
}

