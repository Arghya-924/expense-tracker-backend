package com.project.expense_tracker_backend.config;

import com.project.expense_tracker_backend.constants.ApplicationConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtGenerator {

    @Value("${jwt.secret}")
    private String JWT_SECRET;

    @Value("${jwt.expiration}")
    private long JWT_EXPIRATION_SECONDS;

    public String generateToken(Authentication authentication) {

        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));

        String email = authentication.getPrincipal().toString();

        Map<String, String> claims = new HashMap<>();

        claims.put("email", email);

        return Jwts.builder()
                .subject(ApplicationConstants.JWT_SUBJECT)
                .claims(claims)
                .expiration(new Date(new Date().getTime() + JWT_EXPIRATION_SECONDS))
                .signWith(secretKey)
                .compact();
    }
}
