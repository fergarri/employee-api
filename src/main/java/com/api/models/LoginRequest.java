package com.api.models;

public class LoginRequest {
    private String username;
    private String password;
    private Integer expirationMinutes;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password, Integer expirationMinutes) {
        this.username = username;
        this.password = password;
        this.expirationMinutes = expirationMinutes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(Integer expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}