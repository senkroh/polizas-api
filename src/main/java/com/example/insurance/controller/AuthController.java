package com.example.insurance.controller;

import com.example.insurance.security.JwtService;
import com.example.insurance.security.TokenStore;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenStore tokenStore;

    public AuthController(JwtService jwtService, TokenStore tokenStore) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    @RateLimiter(name = "login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String accessToken = jwtService.generateToken(username);
        String refreshToken = tokenStore.createRefreshToken(username);
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    //Implementar lógica para refrescar el token y para no hacer login cada hora
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body){
        String refreshToken = body.get("refreshToken");

        return tokenStore.getUsernameFromRefreshToken(refreshToken)
                .map(username -> {
                    String newAccessToken = jwtService.generateToken(username);
                    return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired refresh token")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader, @RequestBody Map<String, String> body) {

        //blacklisteamos el token de acceso actual
        String accessToken = authHeader.substring("Bearer ".length());
        String jti = jwtService.extractJti(accessToken);
        tokenStore.blacklistJti(jti);

        //matamos el token de refresco tambien
        String refreshToken = body.get("refreshToken");
        tokenStore.deleteRefreshToken(refreshToken);

        return ResponseEntity.noContent().build();


    }
}
