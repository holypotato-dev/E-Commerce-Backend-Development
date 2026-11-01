package com.aadil.ecommerce.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * One-off utility to generate a Base64-encoded HMAC-SHA256 key for {@code jwt.secret}.
 * Run this class's {@code main} method, then set the printed value as the JWT_SECRET env var.
 */
public final class SecretKeyGenerator {

    private SecretKeyGenerator() {
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        keyGenerator.init(256);

        SecretKey key = keyGenerator.generateKey();
        String base64 = Base64.getEncoder().encodeToString(key.getEncoded());

        System.out.println(base64);
    }
}
