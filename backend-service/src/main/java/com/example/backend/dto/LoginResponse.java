package com.example.backend.dto;

// LoginResponse.java  — internal only, not sent directly to client
public class LoginResponse {

    private String accessToken;
    private String refreshToken;    // goes into HttpOnly cookie, not body

    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken  = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
