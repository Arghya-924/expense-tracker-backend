package com.project.expense_tracker_backend.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

public class JwtKeyGeneratorTest {

    @Test
    @Disabled("Needed only once to generate the secret key")
    void generateJwtSecretSigningKey() {

        SecretKey key = Jwts.SIG.HS256.key().build();

        String secretKey = Encoders.BASE64.encode(key.getEncoded());

        System.out.println(secretKey);
    }


}
