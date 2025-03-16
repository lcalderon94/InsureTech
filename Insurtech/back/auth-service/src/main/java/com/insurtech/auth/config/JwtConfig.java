package com.insurtech.auth.config;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;

@Component
@Data
public class JwtConfig {

    @Value("${jwt.secret:rEfdWEFerf34r34FERfe3f34fERfefewfWEFWEFwefWEFWEFwef34F34f3}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @Value("${jwt.issuer:insurtech-auth}")
    private String issuer;

    private Key key;
    private JwtParser parser;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(secret.getBytes()));
        this.parser = Jwts.parserBuilder().setSigningKey(key).build();
    }
}