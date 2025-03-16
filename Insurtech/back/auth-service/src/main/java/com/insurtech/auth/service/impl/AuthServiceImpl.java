package com.insurtech.auth.service.impl;

import com.insurtech.auth.config.JwtConfig;
import com.insurtech.auth.exception.CustomAuthenticationException;
import com.insurtech.auth.model.dto.LoginRequest;
import com.insurtech.auth.model.dto.TokenResponse;
import com.insurtech.auth.model.entity.User;
import com.insurtech.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtConfig jwtConfig;

    @Override
    public TokenResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            return generateTokens(user);
        } catch (BadCredentialsException e) {
            throw new CustomAuthenticationException("Credenciales incorrectas");
        }
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        try {
            Claims claims = jwtConfig.getParser()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            if (!claims.get("type").equals("refresh")) {
                throw new CustomAuthenticationException("Tipo de token inválido");
            }

            String username = claims.getSubject();

            Map<String, Object> accessClaims = new HashMap<>();
            accessClaims.put("type", "access");

            String newAccessToken = Jwts.builder()
                    .setClaims(accessClaims)
                    .setSubject(username)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                    .setIssuer(jwtConfig.getIssuer())
                    .signWith(jwtConfig.getKey())
                    .compact();

            return new TokenResponse(newAccessToken, refreshToken, "Bearer", jwtConfig.getExpiration());

        } catch (Exception e) {
            throw new CustomAuthenticationException("Token de refresco inválido");
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            jwtConfig.getParser().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private TokenResponse generateTokens(User user) {
        Date now = new Date();

        // Create access token
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("type", "access");
        accessClaims.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));
        accessClaims.put("authorities", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        String accessToken = Jwts.builder()
                .setClaims(accessClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtConfig.getExpiration()))
                .setIssuer(jwtConfig.getIssuer())
                .signWith(jwtConfig.getKey())
                .compact();

        // Create refresh token with longer expiration
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("type", "refresh");

        String refreshToken = Jwts.builder()
                .setClaims(refreshClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtConfig.getExpiration() * 5))
                .setIssuer(jwtConfig.getIssuer())
                .signWith(jwtConfig.getKey())
                .compact();

        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtConfig.getExpiration());
    }
}