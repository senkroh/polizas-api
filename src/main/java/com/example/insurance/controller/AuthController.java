package com.example.insurance.controller;

import com.example.insurance.security.JwtService;
import com.example.insurance.security.TokenStore;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, logout, and token management")
public class AuthController {

    private final JwtService jwtService;
    private final TokenStore tokenStore;

    public AuthController(JwtService jwtService, TokenStore tokenStore) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
    }

    @Operation(summary = "Login", description = "Authenticates a user and returns an access token and a refresh token. Limited to 5 requests per minute.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "429", description = "Too many login attempts")
    })
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

    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued"),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired")
    })
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

    @Operation(summary = "Logout", description = "Blacklists the current access token and invalidates the refresh token server-side")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
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
