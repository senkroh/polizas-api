package com.example.insurance.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal String username) {
        if (username == null) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap("name", username);
    }
}
