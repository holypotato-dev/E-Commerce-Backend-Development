package com.aadil.ecommerce.enums;
import lombok.Getter;

@Getter
public enum UserRole {

    USER("user"),
    ADMIN("admin");

    private final String userRole;

    UserRole(String userRole) {
        this.userRole = userRole;
    }
}
