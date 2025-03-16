package com.insurtech.auth.service;

import com.insurtech.auth.model.dto.LoginRequest;
import com.insurtech.auth.model.dto.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest loginRequest);
    TokenResponse refreshToken(String refreshToken);
    boolean validateToken(String token);
}