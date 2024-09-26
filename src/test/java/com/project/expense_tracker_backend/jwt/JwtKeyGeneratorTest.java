package com.project.expense_tracker_backend.jwt;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.security.AesWrapKeyAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.KeyAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtKeyGeneratorTest {

    @Test
    @Disabled("Needed only once to generate the secret key")
    void generateJwtSecretSigningKey() {

        SecretKey key = Jwts.SIG.HS256.key().build();

        String secretKey = Encoders.BASE64.encode(key.getEncoded());

        System.out.println(secretKey);
    }


}
