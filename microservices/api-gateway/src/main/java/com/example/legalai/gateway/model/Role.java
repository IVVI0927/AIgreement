package com.example.legalai.gateway.model;

public enum Role {
    ADMIN("ROLE_ADMIN"),
    REVIEWER("ROLE_REVIEWER"),
    VIEWER("ROLE_VIEWER");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}