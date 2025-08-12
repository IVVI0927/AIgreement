package com.example.legalai.gateway.controller;

import com.example.legalai.gateway.dto.AuthResponse;
import com.example.legalai.gateway.dto.LoginRequest;
import com.example.legalai.gateway.dto.RegisterRequest;
import com.example.legalai.gateway.security.JwtUtil;
import com.example.legalai.gateway.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return userService.validateUser(request.getUsername(), request.getPassword())
                .flatMap(isValid -> {
                    if (isValid) {
                        return userService.findByUsername(request.getUsername())
                                .map(user -> {
                                    String token = jwtUtil.generateToken(user.getUsername());
                                    AuthResponse response = new AuthResponse(
                                            token,
                                            user.getUsername(),
                                            user.getEmail(),
                                            "Login successful"
                                    );
                                    return ResponseEntity.ok(response);
                                });
                    } else {
                        AuthResponse response = new AuthResponse(
                                null,
                                null,
                                null,
                                "Invalid username or password"
                        );
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
                    }
                });
    }
    
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return userService.createUser(
                        request.getUsername(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getFirstName(),
                        request.getLastName()
                )
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getUsername());
                    AuthResponse response = new AuthResponse(
                            token,
                            user.getUsername(),
                            user.getEmail(),
                            "Registration successful"
                    );
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(e -> {
                    AuthResponse response = new AuthResponse(
                            null,
                            null,
                            null,
                            e.getMessage()
                    );
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
                });
    }
    
    @GetMapping("/validate")
    public Mono<ResponseEntity<Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean isValid = jwtUtil.validateToken(token);
            return Mono.just(ResponseEntity.ok(isValid));
        }
        return Mono.just(ResponseEntity.ok(false));
    }
}