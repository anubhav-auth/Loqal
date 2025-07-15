package com.loqal.adminservice.utils;
import java.util.Date;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final RSAKeyProvider keyProvider;

    public JwtService(RSAKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public String generateServiceToken() throws JOSEException {
        JWSSigner signer = new RSASSASigner(keyProvider.getPrivateKey());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("admin-internal")
                .issuer("admin-service")
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
}

