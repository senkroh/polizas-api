package com.example.insurance.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private record RefreshTokenEntry(String username, Instant expiresAt) {}

    //JTI (token unico de id) de accesos de tokens invalidados
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    //Refrescar token (String UUID) -> entry con username y expiración
    private final Map<String, RefreshTokenEntry> refreshTokens = new ConcurrentHashMap<>();

    private final JwtProperties properties;

    public TokenStore(JwtProperties properties) {
        this.properties = properties;
    }

    public String createRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(properties.refreshExpiration());
        refreshTokens.put(token, new RefreshTokenEntry(username, expiresAt));
        return token;
    }

    public Optional<String> getUsernameFromRefreshToken(String token) {
        RefreshTokenEntry entry = refreshTokens.get(token);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            refreshTokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.username());
    }

    public void deleteRefreshToken(String token) {
        refreshTokens.remove(token);
    }

    public void blacklistJti(String jti) {
        blacklist.add(jti);
    }

    public boolean isBlacklisted(String jti) {
        return blacklist.contains(jti);
    }
}
