package com.aadil.ecommerce.enums;

import lombok.Getter;

@Getter
public enum AdminRequestStatus {
    ACCEPTED("accepted"),
    PENDING("pending"),
    REJECTED("rejected"),
    EXPIRED("expired");

    private final String requestStatus;

    AdminRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}
