package com.aadil.ecommerce.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class InvitationCreations {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateInvitation() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String invitationHash(String rawInviteToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digestMessage = messageDigest.digest(rawInviteToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digestMessage);
        } catch (NoSuchAlgorithmException algorithmException) {
            throw new IllegalStateException("SHA-256 algorithm is not available in the JVM environment", algorithmException);
        }
        
    }
}
