package com.example.insurance.config;

import com.example.insurance.security.JwtProperties;
import com.example.insurance.security.TokenStore;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final TokenStore tokenStore;

    public SecurityConfig(JwtProperties jwtProperties, TokenStore tokenStore) {
        this.jwtProperties = jwtProperties;
        this.tokenStore = tokenStore;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login", "/auth/refresh", "/auth/logout", "/error",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**","/actuator/**"
                        ).permitAll()
                        .anyRequest()
                        .authenticated()

                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        return token -> {
            Jwt jwt = decoder.decode(token);          // validates signature + expiry
            if (tokenStore.isBlacklisted(jwt.getId())) {  // checks blacklist
                throw new BadJwtException("Token has been revoked");
            }
            return jwt;
        };
    }
}
