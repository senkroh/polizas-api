package com.example.insurance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "User", description = "Authenticated user information")
public class UserController {

    @Operation(summary = "Get current user", description = "Returns the username of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("name", jwt.getSubject());
    }
}
