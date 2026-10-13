package com.careerdomain.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final byte[] secret;
    private final long lifetimeSeconds;
    public TokenService(@Value("${app.auth.secret}") String secret,
                        @Value("${app.auth.expiration-hours}") long hours) {
        if (secret.length() < 32) throw new IllegalStateException("APP_AUTH_SECRET must have at least 32 characters");
        this.secret = secret.getBytes(StandardCharsets.UTF_8); this.lifetimeSeconds = hours * 3600;
    }
    public String create(String email) { long expiry = Instant.now().getEpochSecond() + lifetimeSeconds; String body = b64(email + ":" + expiry); return body + "." + signature(body); }
    public String subject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2 || !constantEquals(signature(parts[0]), parts[1])) return null;
            String[] data = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split(":", 2);
            if (data.length != 2 || data[0].isBlank() || Long.parseLong(data[1]) < Instant.now().getEpochSecond()) return null;
            return data[0];
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    private String b64(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private String signature(String value) { try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret, "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private boolean constantEquals(String a, String b) { return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)); }
}
