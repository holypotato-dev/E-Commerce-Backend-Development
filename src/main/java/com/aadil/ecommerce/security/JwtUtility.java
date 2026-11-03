package com.aadil.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtility {

    /**
     * Extended default expiration duration: 7 days in milliseconds (604,800,000 ms).
     */
    public static final long DEFAULT_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800000}")
    private Long expiration;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    protected void init() {
        byte[] byteCode = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(byteCode);

        // Pre-build immutable, thread-safe JwtParser with 60-second clock skew tolerance
        // to prevent premature expiration due to WSL2/host clock drift
        this.jwtParser = Jwts.parser()
                .verifyWith(this.secretKey)
                .clockSkewSeconds(60)
                .build();
    }

    /**
     * Resolves the effective token expiration duration in milliseconds.
     * Prevents common unit discrepancies where values configured in seconds
     * (e.g., 3600 for 1h, 86400 for 24h, 604800 for 7d, or 3000000)
     * were previously interpreted directly as milliseconds (causing near-instant expiration).
     */
    public long getEffectiveExpirationMs() {
        if (expiration == null || expiration <= 0) {
            return DEFAULT_EXPIRATION_MS;
        }

        // Values below 10,000,000 are interpreted as seconds and converted to milliseconds.
        // E.g., 3600s -> 3,600,000ms (1 hour); 86400s -> 86,400,000ms (24 hours);
        // 3,000,000s -> 3,000,000,000ms (~34.7 days).
        // Millisecond configurations (>= 10,000,000ms, like 604,800,000ms for 7 days) are used as-is.
        if (expiration < 10_000_000L) {
            return expiration * 1000L;
        }
        return expiration;
    }

    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        long now = System.currentTimeMillis();
        long expMs = getEffectiveExpirationMs();

        return Jwts.builder()
                .signWith(secretKey)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expMs))
                .claim("role", role)
                .compact();
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        long now = System.currentTimeMillis();
        long expMs = getEffectiveExpirationMs();

        return Jwts.builder()
                .claims(extraClaims)
                .signWith(secretKey)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expMs))
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date exp = extractExpiration(token);
            return exp != null && exp.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Validates that the token's subject matches the user's username
     * and that the cryptographic signature and expiration are valid.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            Date exp = claims.getExpiration();
            boolean notExpired = exp != null && !exp.before(new Date());
            return username != null && username.equalsIgnoreCase(userDetails.getUsername()) && notExpired;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
